package com.example.vibeverse;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class NavigationHelper {

    public static void setupBottomNavigation(final Activity activity, BottomNavigationView bottomNavigationView) {

        // Set the correct item as selected based on current activity
        int currentItemId = getCurrentItemId(activity);
        bottomNavigationView.setSelectedItemId(currentItemId);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            Intent intent = null;

            if (itemId == R.id.nav_home && !(activity instanceof HomePage)) {
                intent = new Intent(activity, HomePage.class);
            } else if (itemId == R.id.nav_profile && !(activity instanceof ProfilePage)) {
                intent = new Intent(activity, ProfilePage.class);
            } else if (itemId == R.id.nav_add && !(activity instanceof SelectMoodActivity)) {
                intent = new Intent(activity, SelectMoodActivity.class);
            }
            // Uncomment when implementing these pages:
            // else if (itemId == R.id.nav_search && !(activity instanceof SearchPage)) {
            //     intent = new Intent(activity, SearchPage.class);
            // } else if (itemId == R.id.nav_map && !(activity instanceof MapPage)) {
            //     intent = new Intent(activity, MapPage.class);
            // }

            if (intent != null) {
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // Prevents flickering when switching activities
                activity.finish(); // Ensures only one instance of each activity runs at a time
                return true;
            }
            return false;
        });
    }
    private static int getCurrentItemId(Activity activity) {
        if (activity instanceof HomePage) {
            return R.id.nav_home;
        } else if (activity instanceof ProfilePage) {
            return R.id.nav_profile;
        } else if (activity instanceof SelectMoodActivity) {
            return R.id.nav_add;
        }
        // Uncomment when implementing:
        // else if (activity instanceof SearchPage) {
        //     return R.id.nav_search;
        // } else if (activity instanceof MapPage) {
        //     return R.id.nav_map;
        // }
        return R.id.nav_home; // Default to Home if no match is found
    }
}
