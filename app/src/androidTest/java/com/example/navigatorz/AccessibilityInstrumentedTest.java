package com.example.navigatorz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.mapboxsdk.plugins.places.autocomplete.PlaceAutocomplete;

import androidx.test.espresso.accessibility.AccessibilityChecks;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.ServiceTestRule;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class AccessibilityInstrumentedTest {

    @Rule
    public ActivityTestRule<SettingsActivity> settingsActivityActivityTestRule =
            new ActivityTestRule<>(SettingsActivity.class, true, false);

    @Rule
    public ActivityTestRule<MapsActivity> mapsActivityActivityTestRule =
            new ActivityTestRule<>(MapsActivity.class, true, false);

    @Rule
    public ActivityTestRule<NavigateActivity> navigateActivityActivityTestRule =
            new ActivityTestRule<>(NavigateActivity.class, true, false);

    @Rule
    public ActivityTestRule<IntroActivity> introActivityActivityTestRule =
            new ActivityTestRule<>(IntroActivity.class, true, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivityActivityTestRule =
            new ActivityTestRule<>(MainActivity.class, true, false);

    @BeforeClass
    public static void enableAccessibilityChecks() {
        AccessibilityChecks.enable().setRunChecksFromRootView(true);
    }

    @Test
    public void testMainActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, MainActivity.class);
        mainActivityActivityTestRule.launchActivity(intent);

        onView(withId(R.id.txtFood)).perform(click());
    }

    @Test
    public void testMapsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, MapsActivity.class);
        mapsActivityActivityTestRule.launchActivity(intent);

        onView(withId(R.id.bearing_info_textview)).perform(click());

    }

    @Test
    public void testSettingsActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, SettingsActivity.class);
        settingsActivityActivityTestRule.launchActivity(intent);

        onView(withId(R.id.settings)).perform(click());

    }

    @Test
    public void testNavigateActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, NavigateActivity.class);
        navigateActivityActivityTestRule.launchActivity(intent);
    }

    @Test
    public void testIntroActivityAccessibility() throws Exception {
        Context targetContext = InstrumentationRegistry.getInstrumentation()
                .getTargetContext();
        Intent intent = new Intent(targetContext, IntroActivity.class);
        introActivityActivityTestRule.launchActivity(intent);
        onView(withId(R.id.txtWelcome)).perform(click());
    }

}