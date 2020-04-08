package com.example.navigatorz;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;


import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.gson.JsonObject;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.tilequery.MapboxTilequery;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.model.PlaceOptions;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;


import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import pub.devrel.easypermissions.EasyPermissions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;


/**
 * Use the Mapbox Tilequery API to retrieve information about Features on a Vector Tileset. More info about
 * the Tilequery API can be found at https://www.mapbox.com/api-documentation/#tilequery
 */
public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    private static final String RESULT_GEOJSON_SOURCE_ID = "RESULT_GEOJSON_SOURCE_ID";
    private static final String SEARCH_GEOJSON_SOURCE_ID = "SEARCH_GEOJSON_SOURCE_ID";
    private static final String LOCATION_CENTER_GEOJSON_SOURCE_ID = "LOCATION_CENTER_GEOJSON_SOURCE_ID";
    private static final String LAYER_ID = "LAYER_ID";
    private static final String RESULT_ICON_ID = "RESULT_ICON_ID";
    private static final String LOCATION_ICON_ID = "LOCATION_ICON_ID";
    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;
    private static final String LOCATION_LAYER_ID = "LOCATION_LAYER_ID";
    private static final String SEARCH_LAYER_ID = "SEARCH_LAYER_ID";
    protected static final String TAG = MapsActivity.class.getSimpleName();



    private LocationComponent locationComponent;


    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private boolean bearingSwitch = false;
    private List<String[]> data = new ArrayList<String[]>();


    BroadcastReceiver broadcastReceiver;
    private ArrayList<DirectionsRoute> routes = new ArrayList<>();



    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;


    private int prev_type = -1;




    // Monitors the state of the connection to the service.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LocationUpdatesService.LocalBinder binder = (LocationUpdatesService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };





    private LocationEngine locationEngine;
    private ArrayList<Location> tilequerylocs = new ArrayList<>();
    private HashMap<Location, ArrayList<String>> poi_navigation = new HashMap<>();
    private ArrayList<Integer> bearings_arr = new ArrayList<Integer>();
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 8000;
    private long DEFAULT_MAX_WAIT_TIME = 8000;

    // Variables needed to listen to location updates
    private MapsActivityLocationCallback callback = new MapsActivityLocationCallback(this);


    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_maps);



        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                    int type = intent.getIntExtra("type", -1);
                    int confidence = intent.getIntExtra("confidence", 0);
                    handleUserActivity(type, confidence);
                }
            }
        };
        getPermissionWriteFile();

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Navigation Mode");
            actionBar.setHomeActionContentDescription("Back to Main Menu");
        }
    }

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.unknown_activity);
        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);


        if (confidence > Constants.CONFIDENCE && type != prev_type && Utils.requestingLocationUpdates(this)) {
            switch (type) {
                case DetectedActivity.STILL: {
                    prev_type = DetectedActivity.STILL;
                    Log.e(TAG, "User is still");
                    //mService.removeLocationUpdates();
                    //mService.createLocationRequest(true);
                    //mService.requestLocationUpdates();
                    break;
                }
                case DetectedActivity.WALKING | DetectedActivity.UNKNOWN: {
                    prev_type = DetectedActivity.UNKNOWN;
                    //mService.removeLocationUpdates();
                    //mService.createLocationRequest(false);
                    //mService.requestLocationUpdates();
                    break;
                }
            }
        }
    }






    private void getPermissionWriteFile() {
        String perms = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...


        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, "We need this",
                    1, perms);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MapsActivity.this.mapboxMap = mapboxMap;
        UiSettings uiSettings = mapboxMap.getUiSettings();
        uiSettings.setAttributionEnabled(false);
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/wilsonsfc/ck3gbcohn0b641cqqa1yaaw24"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                displayDeviceLocation(style);
                addResultLayer(style);
                addClickLayer(style);
                addSearchLayer(style);
            }
        });
    }

    private void initSearchFab(Location point) {

        findViewById(R.id.fab_location_search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new PlaceAutocomplete.IntentBuilder()
                        .accessToken(Mapbox.getAccessToken() != null ? Mapbox.getAccessToken() : getString(R.string.access_token))
                        .placeOptions(PlaceOptions.builder()
                                .backgroundColor(Color.parseColor("#ffffff"))
                                .limit(10)
                                .proximity(Point.fromLngLat(point.getLongitude(),point.getLatitude()))
                                .build(PlaceOptions.MODE_CARDS))
                        .build(MapsActivity.this);
                startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            Intent intent1 = new Intent(MapsActivity.this, NavigateActivity.class);
            intent1.putExtra(Intent.EXTRA_INTENT, data);
            startActivity(intent1);

        }


    }

    /**
     * Add a map layer which will show marker icons for all of the Tilequery API results
     */
    private void addResultLayer(@NonNull Style loadedMapStyle) {
        // Add the marker image to map
        loadedMapStyle.addImage(RESULT_ICON_ID, getBitmapFromVectorDrawable(this, R.drawable.blue_marker));

        // Retrieve GeoJSON information from the Mapbox Tilequery API
        loadedMapStyle.addSource(new GeoJsonSource(RESULT_GEOJSON_SOURCE_ID));

        loadedMapStyle.addLayer(new SymbolLayer(LAYER_ID, RESULT_GEOJSON_SOURCE_ID).withProperties(
                iconImage(RESULT_ICON_ID),
                iconOffset(new Float[] {0f, -12f}),
                iconIgnorePlacement(true),
                iconAllowOverlap(true)
        ));
    }

    private void addSearchLayer(@NonNull Style loadedMapStyle) {
        // Add the marker image to map
        loadedMapStyle.addImage(RESULT_ICON_ID, getBitmapFromVectorDrawable(this, R.drawable.blue_marker));

        // Retrieve GeoJSON information from the Mapbox Tilequery API
        loadedMapStyle.addSource(new GeoJsonSource(SEARCH_GEOJSON_SOURCE_ID));

        loadedMapStyle.addLayer(new SymbolLayer(SEARCH_LAYER_ID, SEARCH_GEOJSON_SOURCE_ID).withProperties(
                iconImage(RESULT_ICON_ID),
                iconOffset(new Float[] {0f, -12f}),
                iconIgnorePlacement(true),
                iconAllowOverlap(true)
        ));
    }

    /**
     * Add a map layer which will show a truncated location icon where the map was clicked
     */
    private void addClickLayer(@NonNull Style loadedMapStyle) {
        loadedMapStyle.addImage(LOCATION_ICON_ID, getBitmapFromVectorDrawable(this, R.drawable.red_marker));

        loadedMapStyle.addSource(new GeoJsonSource(LOCATION_CENTER_GEOJSON_SOURCE_ID));

        loadedMapStyle.addLayer(new SymbolLayer(LOCATION_LAYER_ID, LOCATION_CENTER_GEOJSON_SOURCE_ID).withProperties(
                iconImage(LOCATION_ICON_ID),
                iconOffset(new Float[] {0f, -12f}),
                iconIgnorePlacement(true),
                iconAllowOverlap(true)
        ));
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }


    /**
     * Use the Java SDK's MapboxTilequery class to build a API request and use the API response
     *
     * @param point the center point that the the tilequery will originate from.
     */
    private void makeTilequeryApiCall(@NonNull LatLng point) {
        Integer radius = Utils.getCurrentRadius(this);
        MapboxTilequery tilequery = MapboxTilequery.builder()
                .accessToken(getString(R.string.access_token))
                .mapIds("mapbox.mapbox-streets-v8")
                .query(Point.fromLngLat(point.getLongitude(), point.getLatitude()))
                .radius(radius)
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
                            Toast.makeText(MapsActivity.this,
                                    getString(R.string.no_tilequery_response_features_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            tilequerylocs = new ArrayList<>();
                            poi_navigation = new HashMap<>();
                                Log.d(TAG, featureList.toString());

                            extractDataPOI(featureList, point);

                        }
                    }
                    mapboxMap.getStyle(new Style.OnStyleLoaded() {
                        @Override
                        public void onStyleLoaded(@NonNull Style style) {
                            GeoJsonSource resultSource = style.getSourceAs(RESULT_GEOJSON_SOURCE_ID);
                            //GeoJsonSource locationSource = style.getSourceAs(LOCATION_CENTER_GEOJSON_SOURCE_ID);

                            if (resultSource != null && responseFeatureCollection.features() != null) {
                                List<Feature> featureList = responseFeatureCollection.features();
                                //locationSource.setGeoJson(Point.fromLngLat(point.getLongitude(), point.getLatitude()));
                                if (featureList.isEmpty()) {
                                    Toast.makeText(MapsActivity.this,
                                            getString(R.string.no_tilequery_response_features_toast), Toast.LENGTH_SHORT).show();
                                } else {
                                    resultSource.setGeoJson(FeatureCollection.fromFeatures(featureList));

                                }
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
                Log.d(TAG, "Request failed: "+ throwable.getMessage());
                Toast.makeText(MapsActivity.this, R.string.api_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void extractDataPOI(List<Feature> featureList, LatLng point) {
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

            Point p = (Point) feature.geometry();

            if (props != null && tile != null && p != null) {

                String location_name = props.get("name").getAsString();
                String location_type = "";
                if(props.get("category_en")!=null) {
                    location_type = props.get("category_en").getAsString();
                }

                double distance =  (double) Math.round(tile.get("distance").getAsDouble());

                double longitude = p.longitude();
                double latitude = p.latitude();

                Location loc = new Location("");
                loc.setLongitude(longitude);
                loc.setLatitude(latitude);
                tilequerylocs.add(loc);

                ArrayList<String> details = new ArrayList<>();
                details.add(location_type);
                details.add(Double.toString(distance));
                details.add(p.toJson());
                details.add(location_name);
                poi_navigation.put(loc, details);
                getRoute(Point.fromLngLat(point.getLongitude(),point.getLatitude()),p);
            }
        }

    }


    /**
     * Use the Maps SDK's LocationComponent to display the device location on the map
     */
    @SuppressWarnings( {"MissingPermission"})
    private void displayDeviceLocation(@NonNull Style loadedMapStyle) {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            locationComponent = mapboxMap.getLocationComponent();

            // Set the LocationComponent activation options
            LocationComponentActivationOptions locationComponentActivationOptions =
                    LocationComponentActivationOptions.builder(this, loadedMapStyle)
                            .useDefaultLocationEngine(false)
                            .build();

            // Activate with the LocationComponentActivationOptions object
            locationComponent.activateLocationComponent(locationComponentActivationOptions);

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);
            locationComponent.setRenderMode(RenderMode.COMPASS);

            initLocationEngine();
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }


    /**
     * Set up the LocationEngine and the parameters for querying the device's location
     */
    @SuppressLint("MissingPermission")
    private void initLocationEngine() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);

        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

    // The following three methods are related to showing the device's location via the LocationComponent
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        Style style = mapboxMap.getStyle();
        if (granted && style != null) {
            displayDeviceLocation(style);
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }


    private class MapsActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MapsActivity> activityWeakReference;

        MapsActivityLocationCallback(MapsActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @SuppressLint("SetTextI18n")
        @Override
        public void onSuccess(LocationEngineResult result) {
            MapsActivity activity = activityWeakReference.get();




            if (activity != null) {
                Location location = result.getLastLocation();
                initSearchFab(location);
                Log.d(TAG, "Location: " +location);

                if (location == null) {
                    return;
                }
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {

                    LatLng point = truncateLatLng(location, 1e4);
                    makeTilequeryApiCall(point);

                    Integer mybearing =  Math.round(location.getBearing());
                    if(bearings_arr.size()<4) {
                        bearings_arr.add(mybearing);
                    } else {
                        bearings_arr.remove(0);
                        bearings_arr.add(mybearing);
                    }

                    CalculateDirection cd = new CalculateDirection(location, bearings_arr, tilequerylocs,poi_navigation);
                    cd.bearingsToDirection();
                    StringBuilder output = buildOutput(location, Point.fromLngLat(point.getLatitude(), point.getLongitude()));
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                }
            }
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location can not be captured
         *
         * @param exception the exception message
         */
        @Override
        public void onFailure(@NonNull Exception exception) {
            Log.d(TAG, "LocationChangeActivity"+exception.getLocalizedMessage());
            MapsActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
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

    private LatLng truncateLatLng(Location location, double trunc) {
        double lat = (Math.floor(location.getLatitude()*trunc))/trunc;
        double lng = (Math.floor(location.getLongitude()*trunc))/trunc;
        Log.d(TAG, "" +location.getLatitude() + " " + location.getLongitude());
        Log.d(TAG, "" +location.getLatitude()*trunc + " " + (location.getLongitude())*trunc);
        Log.d(TAG, "" +Math.floor(location.getLongitude()*trunc) + " " + Math.floor(location.getLatitude()*trunc));

        Log.d(TAG, "" +lat + " " + lng);

        return new LatLng(lat, lng);

    }


    public StringBuilder buildOutput(Location location, Point point)  {
        StringBuilder poi_text = new StringBuilder();

        AtomicInteger count = new AtomicInteger();
        String msg;
        for (Entry<Location, ArrayList<String>> pair : poi_navigation.entrySet()) {
            Log.d(TAG, ""+count);
            Log.d(TAG, pair.getKey()+"");
            Log.d(TAG, pair.getValue().get(4));
            if(!routes.isEmpty()) {
                if(pair.getValue().get(4).equals("Behind")) {
                    continue;
                }
                int time = (int) Math.round(routes.get(count.get()).duration());
                AtomicReference<String> units = new AtomicReference<>(" seconds");
                if (time >= 60) {
                    time = time / 60;
                    units.set(" minutes");
                }
                long distance = Math.round(Double.parseDouble(pair.getValue().get(1)));
                if(distance<=5) {
                    poi_text.append(pair.getValue().get(3)).append(" is right next to you").append("\n");
                    msg = pair.getValue().get(3)+ " is right next to you\n";
                } else {
                    poi_text.append(pair.getValue().get(3)).append(" is ").append(pair.getValue().get(1)).append("m ").append(" on your ").append(pair.getValue().get(4)).append("\n");
                    msg = pair.getValue().get(3) + " is " + distance + "m or " + +time + units.get() + " on your " + pair.getValue().get(4) + "\n";
                }
                count.getAndIncrement();
                Point poi_point = Point.fromJson(pair.getValue().get(2));
                if (bearingSwitch) {
                    data.add(new String[] {location.getBearing()+"",calculateAverage(bearings_arr)+"",point.latitude()+"",point.longitude()+"", poi_point.latitude()+"",poi_point.longitude()+"",msg });
                }
            }
        }
        return poi_text;
    }

    private void getRoute(Point origin, Point destination) {
        Log.d(TAG, "getRoute:destination:"+destination.coordinates().toString());
        Log.d(TAG, "getRoute:origin:"+origin.coordinates().toString());
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

    @SuppressLint("SimpleDateFormat")
    public void csvWriter(List<String[]> data) {
        Log.d(TAG,"csvWriter"+data.toString());

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        String file_ts = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
        File file = new File(path, "Bearing_Test" + "_" + file_ts + ".csv");

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(file), ',');
            String[] entries = {"bearing","avg_bearing","user_lat","user_lng","poi_lat","poi_lng","message"};
            writer.writeNext(entries);
            writer.writeAll(data);
            writer.close();
            Log.d(TAG, "Write successful");
        } catch (IOException e) {
            Log.e(TAG, "Caught IOException: " + e.getMessage());
        }



    }


    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        mapView.onPause();
    }

    private void startTracking() {
        Intent intent1 = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        startService(intent1);
    }

    private void stopTracking() {
        Intent intent = new Intent(MapsActivity.this, BackgroundDetectedActivitiesService.class);
        stopService(intent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Prevent leaks
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates(callback);
        }
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}