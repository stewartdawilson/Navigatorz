package com.example.navigatorz;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import android.Manifest;

import android.content.pm.PackageManager;

import android.net.Uri;

import android.provider.Settings;
import androidx.annotation.NonNull;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import okhttp3.internal.Util;

import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * The only activity in this sample.
 *
 * Note: Users have three options in "Q" regarding location:
 * <ul>
 *     <li>Allow all the time</li>
 *     <li>Allow while app is in use, i.e., while app is in foreground</li>
 *     <li>Not allow location at all</li>
 * </ul>
 * Because this app creates a foreground service (tied to a Notification) when the user navigates
 * away from the app, it only needs location "while in use." That is, there is no need to ask for
 * location all the time (which requires additional permissions in the manifest).
 *
 * "Q" also now requires developers to specify foreground service type in the manifest (in this
 * case, "location").
 *
 * Note: For Foreground Services, "P" requires additional permission in manifest. Please check
 * project manifest for more information.
 *
 * Note: for apps running in the background on "O" devices (regardless of the targetSdkVersion),
 * location may be computed less frequently than requested when the app is not in the foreground.
 * Apps that use a foreground service -  which involves displaying a non-dismissable
 * notification -  can bypass the background location limits and request location updates as before.
 *
 * This sample uses a long-running bound and started service for location updates. The service is
 * aware of foreground status of this activity, which is the only bound client in
 * this sample. After requesting location updates, when the activity ceases to be in the foreground,
 * the service promotes itself to a foreground service and continues receiving location updates.
 * When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that foreground service is removed.
 *
 * While the foreground service notification is displayed, the user has the option to launch the
 * activity from the notification. The user can also remove location updates directly from the
 * notification. This dismisses the notification and stops the service.
 */
public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    // Used in checking for runtime permissions.
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    private int prev_type = -1;

    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;

    // A reference to the service used to get location updates.
    private LocationUpdatesService mService = null;

    // Tracks the bound state of the service.
    private boolean mBound = false;
    
    // UI elements.
    private ImageButton mRequestLocationUpdatesButton;
    private TextView mExploretxt;
    private TextView mAnnoucementstxt;
    private ImageButton mNavigationButton;
    private FloatingActionButton mFabHealth;
    private FloatingActionButton mFabFood;
    private FloatingActionButton mFabTransport;
    private FloatingActionButton mFabEntertainment;
    private FloatingActionButton mFabStore;
    private FloatingActionButton mFabBar;
    private FloatingActionButton mFabHotel;
    private FloatingActionButton mFabBank;
    private ImageButton mSettingsButton;

    private boolean initialized;
    private String queuedText;
    private TextToSpeech tts;
    private String msg;
    String mostRecentUtteranceID;
    private HashMap<String, String> points = new HashMap<>();







    /**
     * The current location.
     */
    private Location mLocation;

    BroadcastReceiver broadcastReceiver;





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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myReceiver = new MyReceiver();
        setContentView(R.layout.activity_main);

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(tts.getEngines().size() == 0){
                    Toast.makeText(MainActivity.this,"No Engines Installed",Toast.LENGTH_LONG).show();
                }else{
                    if (status == TextToSpeech.SUCCESS){
                        tts.setLanguage(Locale.UK);
                        ttsInitialized();
                    }
                }
            }
        });

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

        startTracking();

        // Check that the user hasn't revoked permissions by going to Settings.
        if (Utils.requestingLocationUpdates(this)) {
            if (!checkPermissions()) {
                requestPermissions();
            }
        }
    }

    private void ttsInitialized() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, utteranceId+" "+msg);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // *** toast will not work if called from a background thread ***
                        mAnnoucementstxt.setText(points.get(utteranceId));
                    }
                });

            }

            @Override
            // this method will always called from a background thread.
            public void onDone(String utteranceId) {
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);

        mRequestLocationUpdatesButton = (ImageButton) findViewById(R.id.request_location_updates_button);
        mNavigationButton = (ImageButton) findViewById(R.id.nav_map_button);

        mFabHealth = (FloatingActionButton) findViewById(R.id.fab_main_health);
        mFabBank = (FloatingActionButton) findViewById(R.id.fab_main_banks);
        mFabBar = (FloatingActionButton) findViewById(R.id.fab_main_bars);
        mFabEntertainment = (FloatingActionButton) findViewById(R.id.fab_main_entertainment);
        mFabHotel = (FloatingActionButton) findViewById(R.id.fab_main_hotels);
        mFabFood = (FloatingActionButton) findViewById(R.id.fab_main_drinkfood);
        mFabTransport = (FloatingActionButton) findViewById(R.id.fab_main_transport);
        mFabStore = (FloatingActionButton) findViewById(R.id.fab_main_stores);
        mSettingsButton = (ImageButton) findViewById(R.id.button_main_settings);


        mExploretxt = (TextView) findViewById(R.id.txtExplore);
        mAnnoucementstxt = (TextView) findViewById(R.id.txt_main_annoucements);

        mRequestLocationUpdatesButton.setOnClickListener(this);
        mFabBank.setOnClickListener(this);
        mFabStore.setOnClickListener(this);
        mFabHealth.setOnClickListener(this);
        mFabFood.setOnClickListener(this);
        mFabTransport.setOnClickListener(this);
        mFabBar.setOnClickListener(this);
        mFabHotel.setOnClickListener(this);
        mFabEntertainment.setOnClickListener(this);

        Log.d(TAG, "YES YES YES");

        mNavigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
            }
        });
        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        // Restore the state of the buttons when the activity (re)launches.
        setButtonsState(Utils.requestingLocationUpdates(this), Utils.KEY_REQUESTING_LOCATION_UPDATES);
        setButtonsState(Utils.requestingStoreUpdates(this), Utils.KEY_REQUESTING_STORE_UPDATES);
        setButtonsState(Utils.requestingBankUpdates(this), Utils.KEY_REQUESTING_BANK_UPDATES);
        setButtonsState(Utils.requestingHealthUpdates(this), Utils.KEY_REQUESTING_HEALTH_UPDATES);
        setButtonsState(Utils.requestingHotelUpdates(this), Utils.KEY_REQUESTING_HOTEL_UPDATES);
        setButtonsState(Utils.requestingEntertainmentUpdates(this), Utils.KEY_REQUESTING_ENTERTAINMENT_UPDATES);
        setButtonsState(Utils.requestingBarUpdates(this), Utils.KEY_REQUESTING_BAR_UPDATES);
        setButtonsState(Utils.requestingFoodUpdates(this), Utils.KEY_REQUESTING_FOOD_UPDATES);
        setButtonsState(Utils.requestingTransportUpdates(this), Utils.KEY_REQUESTING_TRANSPORT_UPDATES);



        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LocationUpdatesService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void startTracking() {
        Intent intent1 = new Intent(this, BackgroundDetectedActivitiesService.class);
        startService(intent1);
    }

    private void handleUserActivity(int type, int confidence) {
        String label = getString(R.string.unknown_activity);
        Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);


        if (confidence > Constants.CONFIDENCE && type != prev_type && Utils.requestingLocationUpdates(this)) {
            switch (type) {
                case DetectedActivity.STILL: {
                    prev_type = DetectedActivity.STILL;
                    Log.e(TAG, "User is still");
                    mService.removeLocationUpdates();
                    mService.createLocationRequest(true);
                    mService.requestLocationUpdates();
                    break;
                }
                case DetectedActivity.WALKING | DetectedActivity.UNKNOWN: {
                    prev_type = DetectedActivity.UNKNOWN;
                    mService.removeLocationUpdates();
                    mService.createLocationRequest(false);
                    mService.requestLocationUpdates();
                    break;
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LocationUpdatesService.ACTION_BROADCAST));
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(Constants.BROADCAST_DETECTED_ACTIVITY));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
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
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    /**
     * Returns the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        return  PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.activity_main),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService.requestLocationUpdates();
            } else {
                // Permission denied.
                setButtonsState(false, Utils.KEY_REQUESTING_LOCATION_UPDATES);
                Snackbar.make(
                        findViewById(R.id.activity_main),
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.settings, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.request_location_updates_button:
            {
                Log.d(TAG, "Exploring button pressed");
                boolean state = Utils.requestingLocationUpdates(this);
                if (!checkPermissions()) {
                    requestPermissions();
                } else {
                    Log.d(TAG, "Location state:"+state);
                    if(!state) {
                        Log.i(TAG, "Requesting Location Updates");
                        mService.requestLocationUpdates();
                    } else {
                        Log.i(TAG, "Stopping Location Updates");
                        mService.removeLocationUpdates();
                        tts.stop();
                    }
                }
                break;
            }
            case R.id.fab_main_health:
            {
                boolean state = Utils.requestingHealthUpdates(this);
                Log.d(TAG, "Health State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Health Updates");
                    Utils.setHealthUpdates(this, true);

                } else {
                    Log.i(TAG, "Stopping Health Updates");
                    Utils.setHealthUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_transport:
            {
                boolean state = Utils.requestingTransportUpdates(this);
                Log.d(TAG, "Transport State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Transport Updates");
                    Utils.setTransportUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Transport Updates");
                    Utils.setTransportUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_stores:
            {
                boolean state = Utils.requestingStoreUpdates(this);
                Log.d(TAG, "Store State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Store Updates");
                    Utils.setStoreUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Store Updates");
                    Utils.setStoreUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_hotels:
            {
                boolean state = Utils.requestingHotelUpdates(this);
                Log.d(TAG, "Hotel State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Hotel Updates");
                    Utils.setHotelUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Hotel Updates");
                    Utils.setHotelUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_entertainment:
            {
                boolean state = Utils.requestingEntertainmentUpdates(this);
                Log.d(TAG, "Entertainment State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Entertainment Updates");
                    Utils.setEntertainmentUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Entertainment Updates");
                    Utils.setEntertainmentUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_drinkfood:
            {
                boolean state = Utils.requestingFoodUpdates(this);
                Log.d(TAG, "Food State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Food Updates");
                    Utils.setFoodUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Food Updates");
                    Utils.setFoodUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_bars:
            {
                boolean state = Utils.requestingBarUpdates(this);
                Log.d(TAG, "Bar State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Bar Updates");
                    Utils.setBarUpdates(this, true);
                } else {
                    Log.i(TAG, "Stopping Bar Updates");
                    Utils.setBarUpdates(this, false);
                }
                break;
            }
            case R.id.fab_main_banks:
            {
                boolean state = Utils.requestingBankUpdates(this);
                Log.d(TAG, "Bank State:"+state);
                if(!state) {
                    Log.i(TAG, "Requesting Bank Updates");
                    Utils.setBankUpdates(this, true);

                } else {
                    Log.i(TAG, "Stopping Bank Updates");
                    Utils.setBankUpdates(this, false);
                }
                break;
            }

        }

    }
    /**
     * Receiver for broadcasts sent by {@link LocationUpdatesService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());

            if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY)) {
                Log.d(TAG, "Receiving activities");
                int type = intent.getIntExtra("type", -1);
                int confidence = intent.getIntExtra("confidence", 0);
                handleUserActivity(type, confidence);
            }
            ArrayList<String> messages = intent.getStringArrayListExtra(LocationUpdatesService.EXTRA_LOCATION);
            points = new HashMap<>();
            for (String m : messages) {
                speak(m);
            }
        }
    }

    private void speak(String m) {
        // set unique utterance ID for each utterance
        mostRecentUtteranceID = (new Random().nextInt() % 9999999) + ""; // "" is String force

        // set params
        // *** this method will work for more devices: API 19+ ***
        HashMap<String, String> params = new HashMap<>();
        points.put(mostRecentUtteranceID, m);
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, mostRecentUtteranceID);
        tts.speak(m,TextToSpeech.QUEUE_ADD,params);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        // Update the buttons state depending on whether location updates are being requested.
        Log.d(TAG+"onSharedPreferenceChanged:", s);

        switch (s) {
            case Utils.KEY_REQUESTING_LOCATION_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES, false),  Utils.KEY_REQUESTING_LOCATION_UPDATES);break;
            case Utils.KEY_REQUESTING_HEALTH_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_HEALTH_UPDATES, false), Utils.KEY_REQUESTING_HEALTH_UPDATES);break;
            case Utils.KEY_REQUESTING_BANK_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_BANK_UPDATES, false), Utils.KEY_REQUESTING_BANK_UPDATES);break;
            case Utils.KEY_REQUESTING_ENTERTAINMENT_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_ENTERTAINMENT_UPDATES, false), Utils.KEY_REQUESTING_ENTERTAINMENT_UPDATES);break;
            case Utils.KEY_REQUESTING_TRANSPORT_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_TRANSPORT_UPDATES, false),  Utils.KEY_REQUESTING_TRANSPORT_UPDATES);break;
            case Utils.KEY_REQUESTING_FOOD_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_FOOD_UPDATES, false), Utils.KEY_REQUESTING_FOOD_UPDATES);break;
            case Utils.KEY_REQUESTING_HOTEL_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_HOTEL_UPDATES, false), Utils.KEY_REQUESTING_HOTEL_UPDATES);break;
            case Utils.KEY_REQUESTING_STORE_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_STORE_UPDATES, false), Utils.KEY_REQUESTING_STORE_UPDATES);break;
            case Utils.KEY_REQUESTING_BAR_UPDATES: setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_BAR_UPDATES, false), Utils.KEY_REQUESTING_BAR_UPDATES);break;
        }

    }


    private void setButtonsState(boolean requestingUpdates, String button_pressed) {
        Log.d(TAG, requestingUpdates+"");
        Log.d(TAG, "setButtonsState:"+button_pressed);

        switch (button_pressed) {
            case Utils.KEY_REQUESTING_LOCATION_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mRequestLocationUpdatesButton.setColorFilter(Color.BLACK);
                    mExploretxt.setTextColor(Color.BLACK);
                    mRequestLocationUpdatesButton.setBackground(getDrawable(R.drawable.roundcorneryellow));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mRequestLocationUpdatesButton.setColorFilter(Color.WHITE);
                    mExploretxt.setTextColor(Color.WHITE);
                    mRequestLocationUpdatesButton.setBackground(getDrawable(R.drawable.roundcornerblack));
                }
                break;
            }
            case Utils.KEY_REQUESTING_HEALTH_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabHealth.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabHealth.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabHealth.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabHealth.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_BANK_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabBank.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabBank.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabBank.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabBank.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_FOOD_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabFood.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabFood.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabFood.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabFood.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_TRANSPORT_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabTransport.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabTransport.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabTransport.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabTransport.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_STORE_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabStore.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabStore.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabStore.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabStore.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_HOTEL_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabHotel.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabHotel.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabHotel.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabHotel.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_BAR_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabBar.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabBar.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabBar.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabBar.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }
            case Utils.KEY_REQUESTING_ENTERTAINMENT_UPDATES: {
                if (requestingUpdates) {
                    Log.d(TAG, "Change buttons to yellow");
                    mFabEntertainment.setImageTintList(ColorStateList.valueOf(Color.BLACK));
                    mFabEntertainment.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.yellow, null)));
                } else {
                    Log.d(TAG, "Change buttons to black");
                    mFabEntertainment.setImageTintList(ColorStateList.valueOf(Color.WHITE));
                    mFabEntertainment.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
                }
                break;
            }

        }

    }
}