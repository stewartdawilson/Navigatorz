package com.example.navigatorz;

import android.location.Location;
import android.util.Log;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



public class CalculateDirection {

    private Location mylocation;
    private ArrayList<Integer> mybearings;
    private ArrayList<Location> tilequerylocs;
    private  HashMap<Location, ArrayList<String>> poi;
    private String TAG = "CalculateDirection";


    public CalculateDirection(Location mylocation, ArrayList<Integer> mybearings, ArrayList<Location> tilequerylocs,  HashMap<Location, ArrayList<String>> poi) {
        this.mylocation = mylocation;
        this.mybearings =  mybearings;
        this.tilequerylocs = tilequerylocs;
        this.poi = poi;
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

    public void bearingsToDirection() {

        ArrayList<Integer> bearings_to_poi = bearingsToLocations();

        for (int i =0; i<bearings_to_poi.size();i++) {
            Integer bearing;
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

            int mybearingThres1 = (mybearingtemp+15)%360;
            int mybearingThres2 = (mybearingtemp-15)%360;
            int mybearingbackThres1 = (my_bearing_backtemp+10)%360;
            int mybearingbackThres2 = (my_bearing_backtemp-10)%360;
            if (mybearingThres2<0) mybearingThres2 += 360;
            if (mybearingbackThres2<0) mybearingbackThres2 += 360;







            Log.d(TAG+":BEARING-1", ""+mybearingThres1);
            Log.d(TAG+":BEARING-2", ""+mybearingThres2);
            Log.d(TAG+":BEARING-3", ""+mybearingbackThres1);
            Log.d(TAG+":BEARING-4", ""+mybearingbackThres2);




            if(bearing>mybearingThres1 && bearing<130) {
                Log.d(TAG+":Adding RIGHT", bearing.toString());
                String direction = "Right";
                poi.get(tilequerylocs.get(i)).add(direction);
            } else if(bearing<mybearingThres2 && bearing>230) {
                Log.d(TAG+":Adding LEFT",bearing.toString());
                String direction = "Left";
                poi.get(tilequerylocs.get(i)).add(direction);

            } else if((bearing<=mybearingThres1 || bearing>=mybearingThres2) || bearing.equals(mybearing)) {
                Log.d(TAG+":Adding FRONT",bearing.toString());
                String direction = "Front";
                poi.get(tilequerylocs.get(i)).add(direction);

            } else if((bearing>=130 && bearing<=230) || bearing==my_bearing_back) {
                Log.d(TAG+":Adding BEHIND",bearing.toString());
                String direction = "Behind";
                poi.get(tilequerylocs.get(i)).add(direction);

            }
        }
    }

}
