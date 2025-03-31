package com.example.vibeverse;

import android.Manifest;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.rule.IntentsRule;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MapsActivityUITest {

    @Rule
    public ActivityScenarioRule<MapsActivity> activityRule =
            new ActivityScenarioRule<>(MapsActivity.class);

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );

    private FirebaseFirestore mockDb;

    @Before
    public void setup() {
        // You could mock FirebaseFirestore here if needed
        activityRule.getScenario().onActivity(activity -> {
            // Optional setup before each test
        });
    }

    @Test
    public void mapLoadsSuccessfully() {
        // Check that the map fragment is displayed
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    @Test
    public void bottomNavigationIsVisible() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }

    @Test
    public void locationPermissionIsGranted() {
        // Since GrantPermissionRule is used, the permission is granted by default
        // You could still mock location updates and verify behavior using IdlingResource or logs
    }

    @Test
    public void markersAreRenderedOnMap() {
        // This would require you to use the Firebase emulator or mock Firestore responses.
        // Alternatively, set up known test data in Firestore before running the test.

        // Since map rendering is not visible to Espresso directly,
        // you typically verify logs, or use mock GoogleMap to verify marker addition.
    }

    @Test
    public void testLocationFallbackWhenPermissionDenied() {
        // You'd need a separate test where you launch the activity without permissions
        // And verify the fallback location is used (like Edmonton)
    }
}