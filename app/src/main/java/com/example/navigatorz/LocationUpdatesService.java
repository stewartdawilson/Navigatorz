package com.example.navigatorz;


import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.JsonObject;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.tilequery.MapboxTilequery;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
public class LocationUpdatesService extends Service {

    private static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_01";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for announcements when user is walking. Inexact. Updates may be more or less frequent.
     */
    private static final long WALKING_UPDATE_INTERVAL_IN_MILLISECONDS = 15000;

    /**
     * The desired interval for announcements when user is still. Inexact. Updates may be more or less frequent.
     */
    private static final long STILL_UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;

    /**
     * The current location.
     */
    private Location mLocation;

    private TextToSpeech announcer;
    private boolean still = false;
    private String roadName = "";
    private ArrayList<Location> tilequerylocs = new ArrayList<>();
    private HashMap<String, ArrayList<String>> poi = new HashMap<>();
    private ArrayList<Integer> bearings_arr = new ArrayList<Integer>();
    private ArrayList<DirectionsRoute> routes = new ArrayList<>();
    private Ringtone r;





    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult.getLastLocation() == mLocation) {
                    return;
                }
                onNewLocation(locationResult.getLastLocation());
            }
        };




        createLocationRequest(still);
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        r = RingtoneManager.getRingtone(getApplicationContext(), notification);


        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");

            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_android, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_android, getString(R.string.remove_location_updates),
                        servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Utils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);

        mLocation = location;

        if (location == null) {
            return;
        }
        LatLng truncLatLng = truncateLatLng(location, 1e4);
        Point point = Point.fromLngLat(truncLatLng.getLongitude(), truncLatLng.getLatitude());

        makeTilequeryApiCall(point);
        makeTilequeryApiRoadCall(point);
    }

    public ArrayList<String> buildOutput(ArrayList<String> directions) {
        ArrayList<String> poi_text = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, ArrayList<String>> pair : poi.entrySet()) {
            Log.d("COUNT", ""+count);
            Log.d("PAIR", pair.getKey());
            Log.d("DIRECTIONSIZE", ""+directions.size());
            Log.d("DIRECTIONS", directions.get(count));
            if(!routes.isEmpty()) {
                int distance  = (int) Math.round(routes.get(count).distance());
                int time  = (int) Math.round(routes.get(count).duration());
                String units = " seconds";
                if (time>=60) {
                    time = time/60;
                    units = " minutes";
                }
                poi_text.add(pair.getKey() + " is " + distance + "m " + "or " + time + units + " on your " + directions.get(count)+"\n");
                count++;
            }

        }
        return poi_text;
    }

    private LatLng truncateLatLng(Location location, double trunc) {
        double lat = (Math.floor(location.getLatitude()*trunc))/trunc;
        double lng = (Math.floor(location.getLongitude()*trunc))/trunc;
        Log.d("LATLNG", "" +location.getLatitude() + " " + location.getLongitude());
        Log.d("LATLNG", "" +location.getLatitude()*trunc + " " + (location.getLongitude())*trunc);
        Log.d("LATLNG", "" +Math.floor(location.getLongitude()*trunc) + " " + Math.floor(location.getLatitude()*trunc));

        Log.d("LATLNG", "" +lat + " " + lng);

        return new LatLng(lat, lng);

    }


    /**
     * Use the Java SDK's MapboxTilequery class to build a API request and use the API response
     *
     * @param point the center point that the the tilequery will originate from.
     */
    private void makeTilequeryApiCall(@NonNull Point point) {
        MapboxTilequery tilequery = MapboxTilequery.builder()
                .accessToken(getString(R.string.access_token))
                .mapIds("mapbox.mapbox-streets-v8")
                .query(point)
                .radius(35)
                .limit(3)
                .geometry("point")
                .dedupe(true)
                .layers("poi_label,transit_stop_label")
                .build();

        tilequery.enqueueCall(new Callback<FeatureCollection>() {
            @Override
            public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
                if (response.body() != null) {
                    FeatureCollection responseFeatureCollection = response.body();
                    if (responseFeatureCollection.features() != null) {
                        List<Feature> featureList = responseFeatureCollection.features();
                        if (featureList.isEmpty()) {
                            Log.d(TAG, "No features");
                        } else {
                            tilequerylocs = new ArrayList<>();
                            poi = new HashMap<>();
                            Log.d("FEATURELIST-POI", featureList.toString());
                            extractDataPOI(featureList, point);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
                Log.d("Request failed: %s", throwable.getMessage());
            }
        });
    }

    /**
     * Use the Java SDK's MapboxTilequery class to build a API request and use the API response
     *
     * @param point the center point that the the tilequery will originate from.
     */
    private void makeTilequeryApiRoadCall(@NonNull Point point) {
        MapboxTilequery tilequery = MapboxTilequery.builder()
                .accessToken(getString(R.string.access_token))
                .mapIds("mapbox.mapbox-streets-v8")
                .query(point)
                .radius(60)
                .limit(1)
                .geometry("linestring")
                .dedupe(true)
                .layers("road")
                .build();

        tilequery.enqueueCall(new Callback<FeatureCollection>() {
            @Override
            public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
                if (response.body() != null) {
                    FeatureCollection responseFeatureCollection = response.body();
                    if (responseFeatureCollection.features() != null) {
                        List<Feature> featureList = responseFeatureCollection.features();
                        if (featureList.isEmpty()) {
                            Log.d(TAG, "No roads");
                        } else {
                            roadName = "";
                            Log.d("FEATURELIST-ROAD", featureList.toString());
                            extractDataRoad(featureList);

                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
                Log.d("Request failed: %s", throwable.getMessage());
            }
        });
    }

    public void extractDataRoad(List<Feature> featureList) {
        for (int i =0; i<featureList.size(); i++) {
            Feature feature = featureList.get(i);

            JsonObject tile = feature.getProperty("tilequery").getAsJsonObject();

            JsonObject props = feature.properties();
            if (tile.get("layer").getAsString().equals("road") && tile.get("geometry").getAsString().equals("linestring") && props.get("name")!=null) {
                roadName = props.get("name").getAsString();
                break;
            }
        }
    }

    public void extractDataPOI(List<Feature> featureList, Point point) {
        for (int i =0; i<featureList.size(); i++) {
            Feature feature = featureList.get(i);
            Log.d("ExtractDataPOI:", feature.toString());

            JsonObject tile = feature.getProperty("tilequery").getAsJsonObject();

            JsonObject props = feature.properties();
            try {
                props.get("name").getAsString();
            } catch (Exception e) {
                continue;
            }

            Point feature_point = (Point) feature.geometry();
            JsonObject tilequery = (JsonObject) props.get("tilequery");

            ArrayList<ArrayList<String>> filters = checkLocationTypeFilters();

            ArrayList<String> classes = filters.get(0);
            ArrayList<String> category = filters.get(1);
            ArrayList<String> transit = filters.get(2);


            if (props != null && tile != null && feature_point != null && tilequery != null) {
                ArrayList<String> details = new ArrayList<>();
                if(props.get("category_en")!=null) {
                    String location_type = props.get("category_en").getAsString();
                    details.add(location_type);

                } else {
                    String location_type = "";
                    details.add(location_type);
                }
                if(!classes.isEmpty() || !category.isEmpty() || !transit.isEmpty()) {
                    if(classes.contains(props.get("class").getAsString()) || category.contains(props.get("category_en").getAsString()) || transit.contains(tilequery.get("layer").getAsString())) {
                        Log.d(TAG, "Is accepted");
                        String location_name = props.get("name").getAsString();

                        double distance =  (double) Math.round(tile.get("distance").getAsDouble());

                        double longitude = feature_point.longitude();
                        double latitude = feature_point.latitude();

                        Location loc = new Location("");
                        loc.setLongitude(longitude);
                        loc.setLatitude(latitude);
                        tilequerylocs.add(loc);


                        details.add(Double.toString(distance));
                        poi.put(location_name, details);

                        getRoute(point, feature_point);
                    } else {
                        Log.d(TAG, "Not accepted");
                    }
                } else {
                    Log.d(TAG, "No filters");
                    String location_name = props.get("name").getAsString();


                    double distance =  (double) Math.round(tile.get("distance").getAsDouble());

                    double longitude = feature_point.longitude();
                    double latitude = feature_point.latitude();

                    Location loc = new Location("");
                    loc.setLongitude(longitude);
                    loc.setLatitude(latitude);
                    tilequerylocs.add(loc);


                    details.add(Double.toString(distance));
                    poi.put(location_name, details);

                    getRoute(point, feature_point);
                }
            }
        }
        processPointsOfInterest();
    }

    private void processPointsOfInterest() {
        Log.d(TAG, "Location Bearing:" +mLocation.getBearing());
        Integer mybearing =  Math.round(mLocation.getBearing());
        updateBearings(mybearing);

        Log.d(TAG+":tilequerylocs", tilequerylocs.toString());

        CalculateDirection cd = new CalculateDirection(mLocation, bearings_arr, tilequerylocs);
        ArrayList<String> directions = cd.bearingsToDirection();
        ArrayList<String> messages = buildOutput(directions);
        Log.d("ROAD", "" +roadName);

        // Notify anyone listening for broadcasts about the new location.
        Intent intent = new Intent(ACTION_BROADCAST);
        intent.putExtra(EXTRA_LOCATION, messages);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    private void updateBearings(Integer mybearing) {
        if(bearings_arr.size()<5) {
            bearings_arr.add(mybearing);
        } else {
            bearings_arr.remove(0);
            bearings_arr.add(mybearing);
        }
    }

    private ArrayList<ArrayList<String>> checkLocationTypeFilters() {
        ArrayList<String> classes = new ArrayList<>();
        ArrayList<String> category = new ArrayList<>();
        ArrayList<String> transit = new ArrayList<>();
        if(Utils.requestingHealthUpdates(this)) {
            classes.add("medical");
        }
        if(Utils.requestingHotelUpdates(this)) {
            classes.add("lodging");
        }
        if(Utils.requestingBankUpdates(this)){
            category.add("Bank");
        }
        if(Utils.requestingBarUpdates(this)) {
            category.add("Pub");
            category.add("Nightclub");
            category.add("Bar");
        }
        if(Utils.requestingTransportUpdates(this)) {
            transit.add("transit_stop_label");
        }
        if(Utils.requestingEntertainmentUpdates(this)) {
            classes.add("arts_and_entertainment");
        }
        if(Utils.requestingStoreUpdates(this)) {
            classes.add("store_like");
        }
        if(Utils.requestingFoodUpdates(this)) {
            classes.add("food_and_drink");
            classes.add("food_and_drink_stores");
        }
        ArrayList<ArrayList<String>> filters = new ArrayList<>();
        filters.add(classes);
        filters.add(category);
        filters.add(transit);
        return filters;
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(getString(R.string.access_token))
                .origin(origin)
                .destination(destination)
                .profile("walking")
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }
                        DirectionsRoute route = response.body().routes().get(0);
                        routes.add(route);
                    }
                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
    }


    /**
     * Sets the location request parameters.
     * @param still whether the user is still or not still (walking)
     */
    public void createLocationRequest(boolean still) {
        mLocationRequest = new LocationRequest();
        if(still) {
            Log.d(TAG, "User is still");
            mLocationRequest.setInterval(STILL_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else {
            Log.e(TAG, "User not still");
            mLocationRequest.setInterval(WALKING_UPDATE_INTERVAL_IN_MILLISECONDS);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        }
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}