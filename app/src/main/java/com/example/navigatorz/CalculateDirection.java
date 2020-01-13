package com.example.navigatorz;

import android.icu.text.LocaleDisplayNames;
import android.location.Location;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class CalculateDirection {

    private Location mylocation;
    private ArrayList<Integer> mybearings;
    private ArrayList<Location> tilequerylocs;
    private String TAG = "CalculateDirection";


    public CalculateDirection(Location mylocation, ArrayList<Integer> mybearings, ArrayList<Location> tilequerylocs) {
        this.mylocation = mylocation;
        this.mybearings =  mybearings;
        this.tilequerylocs = tilequerylocs;
    }

    public ArrayList<Integer> bearingsToLocations() {
        ArrayList<Integer> bearings_to_poi = new ArrayList<>();
        for(int i =0; i<tilequerylocs.size();i++) {
            Integer bearing = (int) mylocation.bearingTo(tilequerylocs.get(i));
            bearings_to_poi.add(bearing);
       }
        Log.d(TAG+":tilequerylocs", tilequerylocs.toString());
        Log.d(TAG+":bearingsToLocations", bearings_to_poi.toString());

        return bearings_to_poi;
    }

    public int calculateAverage(List<Integer> bearings) {
        Integer sum = 0;
        if(!bearings.isEmpty()) {
            for (Integer bearing : bearings) {
                sum += bearing;
            }
            return (int) Math.round(sum.doubleValue() / bearings.size());
        }
        return Math.round(sum);
    }

    public ArrayList<String> bearingsToDirection() {
        ArrayList<String> directions = new ArrayList<>();

        ArrayList<Integer> bearings_to_poi = bearingsToLocations();

        for (int i =0; i<bearings_to_poi.size();i++) {
            Integer bearing = 0;
            Integer mybearing = calculateAverage(mybearings);
            int my_bearing_back = (int)Math.round((mybearing+180)%360);



            if(bearings_to_poi.get(i)<0) {
                bearing = 360+bearings_to_poi.get(i);
            } else {
                bearing = bearings_to_poi.get(i);
            }

            Log.d(TAG+":BEARING BEFORE TRANSFORMATION", bearing.toString());


            bearing = (bearing-mybearing)%360;
            if (bearing<0) bearing += 360;
            int mybearingtemp = 0;
            int my_bearing_backtemp = 180;

            Log.d(TAG+":BEARING AFTER TRANSFORMATION", bearing.toString());
            Log.d(TAG+":MY CURRENT BEARING", mybearing.toString());
            Log.d(TAG+":MY CURRENT BEARING BACK", ""+my_bearing_back);

            int mybearingThres1 = (mybearingtemp+10)%360;
            int mybearingThres2 = (mybearingtemp-10)%360;
            int mybearingbackThres1 = (my_bearing_backtemp+10)%360;
            int mybearingbackThres2 = (my_bearing_backtemp-10)%360;
            if (mybearingThres2<0) mybearingThres2 += 360;
            if (mybearingbackThres2<0) mybearingbackThres2 += 360;





            Log.d(TAG+":BEARING-1", ""+mybearingThres1);
            Log.d(TAG+":BEARING-2", ""+mybearingThres2);
            Log.d(TAG+":BEARING-3", ""+mybearingbackThres1);
            Log.d(TAG+":BEARING-4", ""+mybearingbackThres2);



            if(bearing>mybearingThres1 && bearing<mybearingbackThres2) {
                Log.d(TAG+":Adding RIGHT", bearing.toString());
                String direction = "Right";
                directions.add(direction);
            } else if(bearing<mybearingThres2 && bearing>mybearingbackThres1) {
                Log.d(TAG+":Adding LEFT",bearing.toString());
                String direction = "Left";
                directions.add(direction);
            } else if((bearing<=mybearingThres1 || bearing>=mybearingThres2) || bearing==mybearing) {
                Log.d(TAG+":Adding FRONT",bearing.toString());
                String direction = "Front";
                directions.add(direction);
            } else if((bearing>=mybearingbackThres2 && bearing<=mybearingbackThres1) || bearing==my_bearing_back) {
                Log.d(TAG+":Adding BEHIND",bearing.toString());
                String direction = "Behind";
                directions.add(direction);
            }
        }
        return directions;


    }
    /*
    https://stackoverflow.com/questions/2131195/cardinal-direction-algorithm-in-java
     */
    private static String headingToString2(double x)
    {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW", "N"};
        return directions[ (int)Math.round((  ((double)x % 360) / 45)) ];
    }

}
