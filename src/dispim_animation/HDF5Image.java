/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dispim_animation;

import ch.systemsx.cisd.base.mdarray.MDByteArray;
import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataClass;
import ch.systemsx.cisd.hdf5.HDF5DataTypeInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.HDF5LinkInformation;
import ch.systemsx.cisd.hdf5.HDF5ObjectType;
import ch.systemsx.cisd.hdf5.IHDF5ObjectReadOnlyInfoProviderHandler;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import java.io.File;
import java.util.List;
import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.integer.UnsignedShortType;

/**
 *
 * @author gevirl
 */
// form an imglib2 image from an hdf5 file dataset
public class HDF5Image {

    public HDF5Image(File hdf5, String dataSet) {
        this(hdf5, dataSet, null);
    }

    public HDF5Image(File hdf5, String dataSet, Integer thres) {
        minMax[0] = Float.MAX_VALUE;
        minMax[1] = Float.MIN_VALUE;

//        ByteHDF5DataSource byteSource = new ByteHDF5DataSource(hdf5,dataSet);
        IHDF5Reader reader = HDF5Factory.openForReading(hdf5);
        List<HDF5LinkInformation> infos = reader.object().getGroupMemberInformation("/", true);
        for (HDF5LinkInformation info : infos) {
            String name = info.getName();
            String path = info.getPath();
            HDF5ObjectType type = info.getType();
            if (type == HDF5ObjectType.DATASET && name.equals(dataSet)) {
                IHDF5ObjectReadOnlyInfoProviderHandler hand = reader.object();
                HDF5DataTypeInformation dtInfo = hand.getDataSetInformation(path).getTypeInformation();
                if (dtInfo.getDataClass() == HDF5DataClass.FLOAT) {
                    MDFloatArray mdArray = reader.float32().readMDArray(path);
                    int[] dims = mdArray.dimensions();
                    ImgFactory<UnsignedShortType> imgFactory = new ArrayImgFactory<>();
                    img = imgFactory.create(new int[]{dims[2], dims[1], dims[0]}, new UnsignedShortType());
                    Cursor cursor = img.localizingCursor();

                    while (cursor.hasNext()) {
                        cursor.fwd();
                        UnsignedShortType obj = (UnsignedShortType) cursor.get();
                        int v = 100 - (int) (100 * mdArray.get(cursor.getIntPosition(2), cursor.getIntPosition(1), cursor.getIntPosition(0), 0));
                        if (v < minMax[0]) {
                            minMax[0] = v;
                        }
                        if (v > minMax[1]) {
                            minMax[1] = v;
                        }
                        if (thres != null) {
                            if (v > thres) {
                                obj.setInteger(1);
                            } else {
                                obj.setInteger(0);
                            }
                        } else {
                            obj.setInteger(v);
                        }
                    }
                } else if (dtInfo.getElementSize() == 1 && !dtInfo.isSigned()) {
                    MDByteArray mdArray = reader.uint8().readMDArray(path);
                    int[] dims = mdArray.dimensions();
                    ImgFactory<UnsignedShortType> imgFactory = new ArrayImgFactory<>();
                    img = imgFactory.create(new int[]{dims[2], dims[1], dims[0]}, new UnsignedShortType());
                    Cursor cursor = img.localizingCursor();

                    while (cursor.hasNext()) {
                        cursor.fwd();
                        UnsignedShortType obj = (UnsignedShortType) cursor.get();
                        int v = mdArray.get(cursor.getIntPosition(2), cursor.getIntPosition(1), cursor.getIntPosition(0));
                        if (v < minMax[0]) {
                            minMax[0] = v;
                        }
                        if (v > minMax[1]) {
                            minMax[1] = v;
                        }
                        if (thres != null) {
                            if (v > thres) {
                                obj.setInteger(1);
                            } else {
                                obj.setInteger(0);
                            }
                        } else {
                            obj.setInteger(v);
                        }
                    }
                }
            }
        }
    }

    public float[] getMinMax() {
        return minMax;
    }

    public Img<UnsignedShortType> getImage() {
        return img;
    }
    Img<UnsignedShortType> img;
    float[] minMax = new float[2];

    static public void main(String[] args) {
        File file = new File("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/MVR_STACKS/", "TP177_Ch2_Ill0_Ang0,90_Probabilities.h5");
        HDF5Image image = new HDF5Image(file, "exported_data");
        new ImageJ();
        ImageJFunctions.showFloat(image.getImage());
        Img img = image.getImage();
        ImagePlus ip = ImageJFunctions.wrap(img, "");

        //IJ.saveAsTiff(ip, "/net/waterston/vol9/diSPIM/20161207_tbx-9_OP636/MVR_STACKS/TP160_Ch2_Ill0_Ang0,90_Simple Segmentation.tiff");
    }
}
