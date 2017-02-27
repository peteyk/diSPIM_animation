
package dispim_animation;

import java.util.Comparator;

/**
 *
 * @author peteyk
 */
public class Timepoint implements Comparable<Timepoint> {
    
    private String path;
    
    
    public Timepoint(String path) {
        this.path = path;
    }
    
    
    public int getTimepointID() {
        String pattern = this.path.substring(this.path.indexOf("TP"), this.path.length() - 21);
        String numberOnly= pattern.replaceAll("[^0-9]", "");
        int id = Integer.parseInt(numberOnly);
        return id;
    }

    @Override
    public int compareTo(Timepoint t) {
        return this.getTimepointID() - t.getTimepointID();
    }
    
    public String getPath() {
        return this.path;
    }
}
