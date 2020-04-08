package com.example.navigatorz;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
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
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import java.lang.ref.WeakReference;
import java.util.List;

import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconIgnorePlacement;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;

public class NavigateActivity extends AppCompatActivity implements OnMapReadyCallback, PermissionsListener {

    private static final String TAG = "NavigationAcivity";
    private static final String SEARCH_GEOJSON_SOURCE_ID = "SEARCH_GEOJSON_SOURCE_ID";
    private static final String RESULT_ICON_ID = "RESULT_ICON_ID";
    private static final String SEARCH_LAYER_ID = "SEARCH_LAYER_ID";
    private static final String EXTRA_LOCATION = TAG + ".location";

    // variables for adding location layer
    private PermissionsManager permissionsManager;
    private LocationComponent locationComponent;
    private LocationEngine locationEngine;

    private MapboxMap mapboxMap;
    private MapView mapView;

    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private NavigationMapRoute navigationMapRoute;

    private long DEFAULT_INTERVAL_IN_MILLISECONDS = 1000L;
    private long DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS*5;



    private Intent intent_search;
    private Button nav_button;
    private TextView txtCategory;
    private TextView txtPlaceName;
    private TextView txtPhone;
    private TextView txtAddress;


    // Variables needed to listen to location updates
    private MainActivityLocationCallback callback = new MainActivityLocationCallback(this);
    private boolean navInit = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigate);

        Intent intent = getIntent();
        intent_search = intent.getParcelableExtra(Intent.EXTRA_INTENT);





        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_navigate);

        mapView = findViewById(R.id.mapView2);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeActionContentDescription("Back to Navigation Map");
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap) {
        NavigateActivity.this.mapboxMap = mapboxMap;
        UiSettings uiSettings = mapboxMap.getUiSettings();
        uiSettings.setAttributionEnabled(false);
        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/wilsonsfc/ck3gbcohn0b641cqqa1yaaw24"), new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                enableLocationComponent(style);
                addSearchLayer(style);

                nav_button = findViewById(R.id.button_nav);

                txtCategory = findViewById(R.id.txtCategory);
                txtPlaceName = findViewById(R.id.txtPlaceName);
                txtPhone = findViewById(R.id.txtPhone);
                txtAddress = findViewById(R.id.txtAddress);


                nav_button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Log.d(TAG, "Starting Navigation");
                        startNavigation();
                    }
                });

            }
        });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {
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
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build();

        locationEngine.requestLocationUpdates(request, callback, getMainLooper());
        locationEngine.getLastLocation(callback);
    }

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
        if (granted) {
            enableLocationComponent(mapboxMap.getStyle());
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private class MainActivityLocationCallback
            implements LocationEngineCallback<LocationEngineResult> {

        private final WeakReference<NavigateActivity> activityWeakReference;

        MainActivityLocationCallback(NavigateActivity activity) {
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
            NavigateActivity activity = activityWeakReference.get();

            if (activity != null) {
                Location location = result.getLastLocation();

                if (location == null) {
                    return;
                }
                // Pass the new location to the Maps SDK's LocationComponent
                if (activity.mapboxMap != null && result.getLastLocation() != null) {
                    activity.mapboxMap.getLocationComponent().forceLocationUpdate(result.getLastLocation());
                    if(!navInit) {
                        navInit = true;
                        initNavigationMap(intent_search);
                    }
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
            NavigateActivity activity = activityWeakReference.get();
            if (activity != null) {
                Toast.makeText(activity, exception.getLocalizedMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }



    public void startNavigation() {
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .lightThemeResId(R.style.CustomNavigationViewLight)
                .build();
        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(NavigateActivity.this, options);
    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
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

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.updateRouteArrowVisibilityTo(false);
                            navigationMapRoute.updateRouteVisibilityTo(false);

                        } else {

                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.CustomNavigationMapRoute );
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });
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

    public static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void initNavigationMap(Intent data) {
        Log.d(TAG, ":Entering initNavMap");
        // Retrieve selected location's CarmenFeature
        CarmenFeature selectedCarmenFeature;
        selectedCarmenFeature = PlaceAutocomplete.getPlace(data);

        selectedCarmenFeature.address();
        Log.d(TAG, selectedCarmenFeature.placeName());

        txtPlaceName.setText("Name: " +selectedCarmenFeature.text());
        txtAddress.setText("Address: " +selectedCarmenFeature.placeName());
        try {
            String categories = selectedCarmenFeature.properties().get("category").getAsString();
            String[] categories_arr = categories.trim().split("\\s*,\\s*");
            String category = categories_arr[categories_arr.length-1];

            String category_formatted = category.substring(0, 1).toUpperCase() + category.substring(1);

            txtCategory.setText("Place Category: " +category_formatted);
            Log.d(TAG, category_formatted);

        } catch (NullPointerException e)  {
            String category = "No category available";
            Log.d(TAG, category);
            txtCategory.setText("Place Category: " +category);
        }

        try {
            String tel = selectedCarmenFeature.properties().get("tel").getAsString();
            txtPhone.setText("Phone Number: " +tel);
            Log.d(TAG, tel);

        } catch (NullPointerException e)  {
            String tel = "No telephone available";
            Log.d(TAG, tel);
            txtPhone.setText("Phone Number: " +tel);
        }


        // Create a new FeatureCollection and add a new Feature to it using selectedCarmenFeature above.
        // Then retrieve and update the source designated for showing a selected location's symbol layer icon

        Log.d(TAG, mapboxMap.toString());
        if (mapboxMap != null) {
            Log.d(TAG, ":Map not null");
            Style style = mapboxMap.getStyle();
            if (style != null) {
                GeoJsonSource source = style.getSourceAs(SEARCH_GEOJSON_SOURCE_ID);
                if (source != null) {
                    source.setGeoJson(FeatureCollection.fromFeatures(
                            new Feature[] {Feature.fromJson(selectedCarmenFeature.toJson())}));
                }

                LatLng destPoint = new LatLng(((Point) selectedCarmenFeature.geometry()).latitude(),
                        ((Point) selectedCarmenFeature.geometry()).longitude());

                Log.d(TAG, ":Move camera");


                // Move map camera to the selected location
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(
                        new CameraPosition.Builder()
                                .target(destPoint)
                                .zoom(17)
                                .build()), 1000);

                Location user_location = locationComponent.getLastKnownLocation();
                Log.d(TAG, user_location.toString());
                Point destinationPoint = Point.fromLngLat( ((Point)selectedCarmenFeature.geometry()).longitude(), ((Point)selectedCarmenFeature.geometry()).latitude());
                Point originPoint = Point.fromLngLat(user_location.getLongitude(), user_location.getLatitude());

                Log.d(TAG, destinationPoint.toString());
                Log.d(TAG, originPoint.toString());

                getRoute(originPoint, destinationPoint);
            }

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
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
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        super.onStop();
    }
}
