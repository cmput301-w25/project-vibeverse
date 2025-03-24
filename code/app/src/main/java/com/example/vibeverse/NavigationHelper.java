package com.example.vibeverse;

import android.app.Activity;
import android.content.Intent;

import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * NavigationHelper provides utility methods to configure and handle
 * bottom navigation in the application.
 * <p>
 * This class includes methods to set the appropriate navigation item as selected
 * based on the current Activity and to handle navigation item selections by launching
 * the corresponding Activity.
 * </p>
 */
public class NavigationHelper {

    /**
     * Sets up the bottom navigation for the given activity.
     * <p>
     * This method selects the correct navigation item based on the current Activity,
     * and sets a listener that launches the appropriate Activity when a navigation item is selected.
     * It also prevents activity flickering during transitions and ensures only one instance
     * of each Activity runs at a time.
     * </p>
     *
     * @param activity            The current Activity.
     * @param bottomNavigationView The BottomNavigationView to configure.
     */
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
            else if (itemId == R.id.nav_search && !(activity instanceof SearchUserPage)) {
                 intent = new Intent(activity, SearchUserPage.class);
            } else if (itemId == R.id.nav_map && !(activity instanceof MapsActivity)) {
                 intent = new Intent(activity, MapsActivity.class);
             }

            if (intent != null) {
                activity.startActivity(intent);
                activity.overridePendingTransition(0, 0); // Prevents flickering when switching activities
                activity.finish(); // Ensures only one instance of each activity runs at a time
                return true;
            }
            return false;
        });
    }

    /**
     * Determines the appropriate navigation item ID based on the current Activity.
     *
     * @param activity The current Activity.
     * @return The resource ID of the navigation item corresponding to the Activity.
     */
    private static int getCurrentItemId(Activity activity) {
        if (activity instanceof HomePage) {
            return R.id.nav_home;
        } else if (activity instanceof ProfilePage) {
            return R.id.nav_profile;
        } else if (activity instanceof SelectMoodActivity) {
            return R.id.nav_add;
        } else if (activity instanceof SearchUserPage) {
             return R.id.nav_search;
         }
        // Uncomment when implementing:
        else if (activity instanceof MapsActivity) {
             return R.id.nav_map;
        }
        return R.id.nav_home; // Default to Home if no match is found
    }
}
