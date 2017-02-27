package dispim_animation;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileSaver;
import ij.plugin.FolderOpener;
import ij.plugin.HyperStackConverter;
import ij.plugin.ImageCalculator;
import ij.plugin.RGBStackMerge;
import ij.plugin.filter.RankFilters;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import mcib3d.geom.ObjectCreator3D;
import mcib3d.geom.Point3D;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;

public class DiSPIM_Animation {

    final private String directory;
    final private double channel1Min;
    final private double channel1Max;
    //final private int rotatingTimepoint;
    final private int zplane;
    private int stackSize;

    public DiSPIM_Animation(String dir, double min1, double max1, int zplane) {
        this.directory = dir;
        this.channel1Min = min1;
        this.channel1Max = max1;
        this.zplane = zplane;
    }

    public static void main(String[] args) {
        // TODO need to add the ability to select lineage channel!

        String directory = args[0];
        directory += "/MVR_STACKS/";

        double channel1Min = Double.parseDouble(args[1]);
        double channel1Max = Double.parseDouble(args[2]);
        int z = Integer.parseInt(args[3]);
        DiSPIM_Animation d = new DiSPIM_Animation(directory, channel1Min, channel1Max, z);

        d.project();
        d.createStack();
    }

    public void project() {

        File animationDir = new File(directory + File.separator + "animation" + File.separator);
        if (!animationDir.exists()) {
            animationDir.mkdir();
        }

        File dir = new File(directory);

        FilenameFilter tpFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.startsWith("TP") && (name.contains("Ch1") && (name.endsWith("tif")))) {
                    return true;
                } else {
                    return false;
                }
            }
        };

        File[] dirFiles = dir.listFiles(tpFilter);

        ArrayList<Timepoint> sortedFiles = new ArrayList<Timepoint>();

        for (File f : dirFiles) {
            sortedFiles.add(new Timepoint(f.getPath()));
        }
        Collections.sort(sortedFiles, new Comparator<Timepoint>() {
            @Override
            public int compare(Timepoint t1, Timepoint t2) {
                return t1.getTimepointID() - t2.getTimepointID();
            }
        });

        RankFilters rf = new RankFilters();

        int lastTimepoint = 0;
        String[] filenames = dir.list();
        for (String s : filenames) {
            if (s.endsWith(".h5")) {
                String st = s.substring(2, s.length() - 32);
                String ints = st.replaceAll("[^0-9.]", "");
                int time = Integer.parseInt(ints);
                if (time > lastTimepoint) {
                    lastTimepoint = time;
                }
            }
        }

        for (int i = 0; i < lastTimepoint; i++) {
            String filename1 = sortedFiles.get(i).getPath();
            ImagePlus ch1 = new ImagePlus(filename1);

            String filename2 = filename1.replace("Ch1", "Ch2");
            ImagePlus ch2 = new ImagePlus(filename2);
            
            stackSize = ch2.getNSlices();
            
            for (int j = 0; j < ch1.getNSlices(); j++) {
                ch1.setSlice(j);
                ImageProcessor i1 = ch1.getProcessor();
                rf.rank(i1, 0.7, RankFilters.MIN);
            }

            File file = new File(filename2.replace(".tif", "_Probabilities.h5"));
            HDF5Image image = new HDF5Image(file, "exported_data", 70);
            Img img = image.getImage();
            ImagePlus hdf = ImageJFunctions.wrap(img, "");
            IJ.run(hdf, "Properties...", "channels=1 slices=" + stackSize + " frames=1 unit=pixel pixel_width=1.0000 pixel_height=1.0000 voxel_depth=1.0000");

            ImageCalculator ic = new ImageCalculator();
            ImagePlus ch1Mult = ic.run("Multiply create stack", ch1, hdf);
            ImagePlus ch2Mult = ic.run("Multiply create stack", ch2, hdf);

            ch2.setSlice(zplane);
            ImageProcessor ip2 = ch2Mult.getProcessor();
            ImageProcessor ip1 = ch1Mult.getProcessor();
            double ch2Min = ip2.getMin();
            double ch2Max = ip2.getMax();

            ip2.setMinAndMax(ch2Min, ch2Max);
            ip1.setMinAndMax(channel1Min, channel1Max);

            drawBox(ch2Mult, 1024);

            ImagePlus[] images = new ImagePlus[]{ch2Mult, ch1Mult};
            ImagePlus comp = RGBStackMerge.mergeChannels(images, false);

            ch1.flush();
            ch2.flush();
            ch2Mult.flush();
            ch1Mult.flush();
            hdf.flush();

            //Rotate image
            int rotationFactor = 1;
            int angle = i * rotationFactor;
            int counter = i / (360 / rotationFactor);
            if ((i * rotationFactor) >= 360) {
                angle = (i * rotationFactor) - (360 * counter);
            }

            //After channel merging, branch on whether timepoint is the last timepoint
            if (i == lastTimepoint - 1) {
                for (int j = i; j < lastTimepoint * 2; j++) {
                    angle = j * rotationFactor;
                    if ((j * rotationFactor) >= 360) {
                        angle = (j * rotationFactor) - (360 * counter);
                    }

                    Projector proj2 = new Projector(comp, angle);
                    ImagePlus projection2 = proj2.doHyperstackProjections();
                    System.out.println("Saved anim_min" + j + ".tif");
                }

            } else {
                Projector proj = new Projector(comp, angle);
                ImagePlus projection = proj.doHyperstackProjections();
                
                FileSaver test = new FileSaver(projection);
                test.saveAsTiff(animationDir.getPath() + File.separator + "anim_min" + i + ".tif");

            }

            comp.flush();
        }
    }

    private void createStack() {
        FolderOpener fo = new FolderOpener();
        ImagePlus img = fo.openFolder(directory + File.separator + "animation" + File.separator);
        ImagePlus hyp = HyperStackConverter.toHyperStack(img, 2, 1, img.getStack().getSize() / 2);
        
//        LUT l1 = LutLoader.openLut("/nfs/waterston/pete/luts/GreenFireBlue.lut");
//        LUT l2 = LutLoader.openLut("/nfs/waterston/pete/luts/neon-red.lut");
//
//        for (int i = 1; i <= hyp.getNFrames(); i++) {
//            hyp.setT(i);
//            hyp.setC(2);
//            ImageProcessor ip = hyp.getProcessor();
//            ip.setLut(l1);
//        }
        

        FileSaver fin = new FileSaver(hyp);
        fin.saveAsTiffStack(directory + File.separator + "animation" + File.separator + "anim_hyperstack.tif");   

//        hyp.setT(hyp.getNFrames());
//        hyp.setC(2);
//        //hyp.setLut(LUT.createLutFromColor(Color.green));
//        hyp.setZ(hyp.getNSlices()/2);
//        ImageProcessor ip = hyp.getChannelProcessor();        
//        ip.setLut(LUT.createLutFromColor(Color.green));
//        ImageStatistics is = ip.getStatistics();
//        Double min = is.min;
//        Double max = is.max;
//        ip.setMinAndMax(min, max);
//        hyp.setDisplayRange(min, max);
    }

    private void drawBox(ImagePlus img, int maxIntensity) {

        int buffer = 25;
        int minX = 0 + buffer;
        int minY = 0 + buffer;
        int minZ = 0 + buffer;
        int maxX = img.getWidth() - buffer;
        int maxY = img.getHeight() - buffer;
        int maxZ = img.getNSlices() - buffer;

        Point3D A = new Point3D((double) minX, (double) minY, (double) minZ);
        Point3D B = new Point3D((double) minX, (double) maxY, (double) minZ);
        Point3D C = new Point3D((double) maxX, (double) maxY, (double) minZ);
        Point3D D = new Point3D((double) maxX, (double) minY, (double) minZ);
        Point3D E = new Point3D((double) minX, (double) minY, (double) maxZ);
        Point3D F = new Point3D((double) minX, (double) maxY, (double) maxZ);
        Point3D G = new Point3D((double) maxX, (double) maxY, (double) maxZ);
        Point3D H = new Point3D((double) maxX, (double) minY, (double) maxZ);

        //AB, BC, CD, DA, EF, FG, GH, HE, AE, DH, BF, CG
        ImageStack stack = img.getStack();
        ObjectCreator3D o = new ObjectCreator3D(stack);
        int rad = 1;
        o.createLine(A, B, maxIntensity, rad);
        o.createLine(B, C, maxIntensity, rad);
        o.createLine(C, D, maxIntensity, rad);
        o.createLine(D, A, maxIntensity, rad);
        o.createLine(E, F, maxIntensity, rad);
        o.createLine(F, G, maxIntensity, rad);
        o.createLine(G, H, maxIntensity, rad);
        o.createLine(H, E, maxIntensity, rad);
        o.createLine(A, E, maxIntensity, rad);
        o.createLine(D, H, maxIntensity, rad);
        o.createLine(B, F, maxIntensity, rad);
        o.createLine(C, G, maxIntensity, rad);
    }
}
