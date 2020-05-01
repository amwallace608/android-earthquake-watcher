package com.amwallace.earthquakewatcher.Util;

public class Constants {
    public static final String URL = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/all_week.geojson";
    public static final int LIMIT = 100;

    public static int colorScale(double magnitude) {
        int colorCode;

        if (magnitude < 3.0) {  //very weak
            colorCode = 0;      //azure
        } else if (magnitude >= 3.0 && magnitude < 4.0) {//minor
            colorCode = 1;      //cyan
        } else if (magnitude >= 4.0 && magnitude < 5.0) {//light
            colorCode = 2;      //green
        } else if (magnitude >= 5.0 && magnitude < 6.0){//moderate
            colorCode = 3;      //yellow
        } else if(magnitude >= 6.0 && magnitude < 7.0){//strong
            colorCode = 4;      //orange
        } else if(magnitude >= 7.0 && magnitude < 8.0){//major
            colorCode = 5;      //rose
        } else if(magnitude >= 8.0){//great
            colorCode = 6;      //green
        } else {
            colorCode = 7;      //violet - indicates issue
        }

      return colorCode;
    }
}
