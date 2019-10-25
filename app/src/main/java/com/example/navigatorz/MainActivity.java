package com.example.navigatorz;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

/**
 * Use the Mapbox Tilequery API to retrieve information about Features on a Vector Tileset. More info about
 * the Tilequery API can be found at https://www.mapbox.com/api-documentation/#tilequery
 */
public class MainActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {

    private static final String RESULT_GEOJSON_SOURCE_ID = "RESULT_GEOJSON_SOURCE_ID";
    private static final String CLICK_CENTER_GEOJSON_SOURCE_ID = "CLICK_CENTER_GEOJSON_SOURCE_ID";
    private static final String LAYER_ID = "LAYER_ID";
    private static final String RESULT_ICON_ID = "RESULT_ICON_ID";
    private static final String CLICK_ICON_ID = "CLICK_ICON_ID";
    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private TextView tilequeryResponseTextView;
    private TextView bearingTextView;
    private TextView bearingAccuracyTextView;

    private LocationEngine locationEngine;
    private ArrayList<Location> tilequerylocs = new ArrayList<>();
    private FeatureCollection fc;
    private HashMap<String, ArrayList<String>> poi = new HashMap<>();
    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5;

    // Variables needed to listen to location updates
    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);

        tilequeryResponseTextView = findViewById(R.id.tilequery_response_info_textview);
        bearingAccuracyTextView = findViewById(R.id.bearing_accuracy_info_textview);
        bearingTextView = findViewById(R.id.bearing_info_textview);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(Style.MAPBOX_STREETS, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                displayDeviceLocation(style);
                Toast.makeText(MainActivity.this, R.string.click_on_map_instruction, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

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
        MapboxTilequery tilequery = MapboxTilequery.builder()
                .accessToken(getString(R.string.access_token))
                .mapIds("mapbox.mapbox-streets-v8")
                .query(Point.fromLngLat(point.getLongitude(), point.getLatitude()))
                .radius(143)
                .limit(5)
                .geometry("point")
                .dedupe(true)
                .layers("poi_label")
                .build();

        tilequery.enqueueCall(new Callback<FeatureCollection>() {
            @Override
            public void onResponse(Call<FeatureCollection> call, Response<FeatureCollection> response) {
                if (response.body() != null) {
                    FeatureCollection responseFeatureCollection = response.body();
                    if (responseFeatureCollection.features() != null) {
                        List<Feature> featureList = responseFeatureCollection.features();
                        if (featureList.isEmpty()) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.no_tilequery_response_features_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            extractData(featureList);

                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<FeatureCollection> call, Throwable throwable) {
                Log.d("Request failed: %s", throwable.getMessage());
                Toast.makeText(MainActivity.this, R.string.api_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void extractData(List<Feature> featureList) {
        for (int i =0; i<featureList.size(); i++) {
            Feature feature = featureList.get(i);

            JsonObject tile = feature.getProperty("tilequery").getAsJsonObject();

            JsonObject props = feature.properties();
            Point p = (Point) feature.geometry();
            if (props != null && tile != null && p != null) {
                String location_name = props.get("name").getAsString();
                String location_type = "";
                if(props.get("category_en")!=null) {
                    location_type = props.get("category_en").getAsString();
                }
                Double distance =  (double) Math.round(tile.get("distance").getAsDouble());

                double longitude = p.longitude();
                double latitude = p.latitude();

                Location loc = new Location("");
                loc.setLongitude(longitude);
                loc.setLatitude(latitude);
                tilequerylocs.add(loc);

                ArrayList<String> details = new ArrayList<>();
                details.add(location_type);
                details.add(distance.toString());
                poi.put(location_name, details);
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
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

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


    private class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<MainActivity> activityWeakReference;

        MainActivityLocationCallback(MainActivity activity) {
            this.activityWeakReference = new WeakReference<>(activity);
        }

        /**
         * The LocationEngineCallback interface's method which fires when the device's location has changed.
         *
         * @param result the LocationEngineResult object which has the last known location within it.
         */
        @Override
        public void onSuccess(LocationEngineResult result) {
            MainActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();
                Log.d("LOCATION", "" +location);

                if (location == null) {
                    return;
                }
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    LatLng point = new LatLng(location.getLatitude(), location.getLongitude());
                    makeTilequeryApiCall(point);

                    Log.d("LOCATION", "" +location.getBearing());
                    Integer mybearing =  Math.round(location.getBearing());
                    bearingTextView.setText(String.format("%s", location.getBearing()));

                    CalculateDirection cd = new CalculateDirection(location, mybearing, tilequerylocs);
                    ArrayList<String> directions = cd.bearingsToDirection();
                    StringBuilder output = buildOutput(directions);
                    tilequeryResponseTextView.setText(output);



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
            Log.d("LocationChangeActivity", exception.getLocalizedMessage());
            MainActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public StringBuilder buildOutput(ArrayList<String> directions) {
        StringBuilder poi_text = new StringBuilder();
        int count = 0;
        for (Entry<String, ArrayList<String>> pair : poi.entrySet()) {
            poi_text.append(pair.getKey()).append(" is ").append(pair.getValue().get(1)).append("m ").append(" on your ").append(directions.get(count)).append("\n");
            count++;
        }
        return poi_text;
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
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