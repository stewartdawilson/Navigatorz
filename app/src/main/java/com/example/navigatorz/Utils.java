package com.example.navigatorz;
import android.content.Context;
import android.location.Location;
import android.preference.PreferenceManager;

import java.text.DateFormat;
import java.util.Date;

class Utils {

    static final String KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates";
    static final String KEY_REQUESTING_HEALTH_UPDATES = "requesting_health_updates";
    static final String KEY_REQUESTING_BANK_UPDATES = "requesting_bank_updates";
    static final String KEY_REQUESTING_ENTERTAINMENT_UPDATES = "requesting_entertainment_updates";
    static final String KEY_REQUESTING_FOOD_UPDATES = "requesting_food_updates";
    static final String KEY_REQUESTING_BAR_UPDATES = "requesting_bar_updates";
    static final String KEY_REQUESTING_HOTEL_UPDATES = "requesting_hotel_updates";
    static final String KEY_REQUESTING_STORE_UPDATES = "requesting_store_updates";
    static final String KEY_REQUESTING_TRANSPORT_UPDATES = "requesting_transport_updates";




    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingLocationUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingHealthUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_HEALTH_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingBankUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_BANK_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingEntertainmentUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_ENTERTAINMENT_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingStoreUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_STORE_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingFoodUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_FOOD_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingTransportUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_TRANSPORT_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingHotelUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_HOTEL_UPDATES, false);
    }

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The {@link Context}.
     */
    static boolean requestingBarUpdates(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_REQUESTING_BAR_UPDATES, false);
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    static void setRequestingLocationUpdates(Context context, boolean requestingLocationUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
                .apply();
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingHealthUpdates The location updates state.
     */
    static void setHealthUpdates(Context context, boolean requestingHealthUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_HEALTH_UPDATES, requestingHealthUpdates)
                .apply();
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingBankUpdates The location updates state.
     */
    static void setBankUpdates(Context context, boolean requestingBankUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_BANK_UPDATES, requestingBankUpdates)
                .apply();
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingBarUpdates The location updates state.
     */
    static void setBarUpdates(Context context, boolean requestingBarUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_BAR_UPDATES, requestingBarUpdates)
                .apply();
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingHotelUpdates The location updates state.
     */
    static void setHotelUpdates(Context context, boolean requestingHotelUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_HOTEL_UPDATES, requestingHotelUpdates)
                .apply();
    }


    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingTransportUpdates The location updates state.
     */
    static void setTransportUpdates(Context context, boolean requestingTransportUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_TRANSPORT_UPDATES, requestingTransportUpdates)
                .apply();
    }


    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingStoreUpdates The location updates state.
     */
    static void setStoreUpdates(Context context, boolean requestingStoreUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_STORE_UPDATES, requestingStoreUpdates)
                .apply();
    }

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingFoodUpdates The location updates state.
     */
    static void setFoodUpdates(Context context, boolean requestingFoodUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_FOOD_UPDATES, requestingFoodUpdates)
                .apply();
    }

    static void setEntertainmentUpdates(Context context, boolean requestingEntertainmentUpdates) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(KEY_REQUESTING_ENTERTAINMENT_UPDATES, requestingEntertainmentUpdates)
                .apply();
    }
    /**
     * Returns the {@code location} object as a human readable string.
     * @param location  The {@link Location}.
     */
    static String getLocationText(Location location) {
        return location == null ? "Unknown location" :
                "(" + location.getLatitude() + ", " + location.getLongitude() + ")";
    }

    static String getLocationTitle(Context context) {
        return context.getString(R.string.walking,
                DateFormat.getDateTimeInstance().format(new Date()));
    }
}