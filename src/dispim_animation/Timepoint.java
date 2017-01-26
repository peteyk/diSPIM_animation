/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
    
//    public int compare(Timepoint t1, Timepoint t2) {
//        return t1.getTimepointID() - t2.getTimepointID();
//    }
    
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
