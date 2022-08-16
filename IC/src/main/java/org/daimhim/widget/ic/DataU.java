package org.daimhim.widget.ic;

import java.util.Date;

public class DataU {
    public static String millisToData(long millis){
        return String.format("%tF%n",new Date(millis));
    }
}
