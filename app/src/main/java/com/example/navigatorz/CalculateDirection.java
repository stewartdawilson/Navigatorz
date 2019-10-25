package com.example.navigatorz;

import android.location.Location;
import android.nfc.Tag;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CalculateDirection {

    private Location mylocation;
    private Integer mybearing;
    private ArrayList<Location> tilequerylocs;
    private String TAG = "CalculateDirection";


    public CalculateDirection(Location mylocation, Integer mybearing, ArrayList<Location> tilequerylocs) {
        this.mylocation = mylocation;
        this.mybearing =  mybearing;
        this.tilequerylocs = tilequerylocs;
    }

    public ArrayList<Integer> bearingsToLocations() {
        ArrayList<Integer> bearings_to_poi = new ArrayList<>();
        for(int i =0; i<tilequerylocs.size();i++) {
            Integer bearing = (int) mylocation.bearingTo(tilequerylocs.get(i));
           bearings_to_poi.add(bearing);
       }
       return bearings_to_poi;
    }

    public ArrayList<String> bearingsToDirection() {
        ArrayList<String> directions = new ArrayList<>();

        ArrayList<Integer> bearings_to_poi = bearingsToLocations();
        String mydirection = headingToString2(mybearing);
        int my_bearing_back = (int)Math.round((mybearing+180)%360);
        for (int i =0; i<bearings_to_poi.size();i++) {
            Integer bearing = 0;
            Log.d(TAG, bearings_to_poi.get(i).toString());

            if(bearings_to_poi.get(i)<0) {
                bearing = 360+bearings_to_poi.get(i);
            } else {
                bearing = bearings_to_poi.get(i);
            }
            if(bearing>mybearing || bearing<my_bearing_back) {

                Log.d(TAG, bearing.toString());
                String direction = "Right";
                directions.add(direction);

            } else if(bearing<mybearing || bearing>my_bearing_back) {
                Log.d(TAG,bearing.toString());
                String direction = "Left";
                directions.add(direction);
            }

        }
        return directions;


    }
    /*
    https://stackoverflow.com/questions/2131195/cardinal-direction-algorithm-in-java
     */
    public static String headingToString2(double x)
    {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[ (int)Math.round((  ((double)x % 360) / 45)) ];
    }

}
