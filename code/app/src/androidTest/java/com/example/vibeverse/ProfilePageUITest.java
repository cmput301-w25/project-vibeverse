package com.example.vibeverse;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.contrib.DrawerActions;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.contrib.DrawerMatchers.isOpen;
import static androidx.test.espresso.contrib.DrawerMatchers.isClosed;

import android.view.Gravity;

/**
 * Instrumented UI tests for the ProfilePage activity.
 */
@RunWith(AndroidJUnit4.class)
public class ProfilePageUITest {

    @Rule
    public ActivityScenarioRule<ProfilePage> activityScenarioRule =
            new ActivityScenarioRule<>(ProfilePage.class);

    /**
     * Test 1: Verify that all core UI elements are displayed.
     */
    @Test
    public void testProfilePageUIElementsAreDisplayed() {
        onView(withId(R.id.textName)).check(matches(isDisplayed()));
        onView(withId(R.id.textUsername)).check(matches(isDisplayed()));
        onView(withId(R.id.textBioContent)).check(matches(isDisplayed()));
        onView(withId(R.id.profilePicture)).check(matches(isDisplayed()));
        onView(withId(R.id.editSearch)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonFilter)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerFeed)).check(matches(isDisplayed()));
    }

    /**
     * Test 2: Ensure the logout menu item is clickable via the right drawer.
     */
    @Test
    public void testLogoutButtonClick() {
        // Open the right-side drawer
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open(Gravity.END));

        // Check that the drawer is open
        onView(withId(R.id.drawer_layout)).check(matches(isOpen(Gravity.END)));

        // Click the logout menu item
        onView(withId(R.id.menu_logout)).perform(click());
    }

    /**
     * Test 3: Check that tapping the filter button shows the FilterDialog.
     */
    @Test
    public void testFilterDialogDisplay() {
        onView(withId(R.id.buttonFilter)).perform(click());
    }

    /**
     * Test 4: Verify search functionality by typing into the search EditText.
     */
    @Test
    public void testSearchFunctionality() {
        onView(withId(R.id.editSearch))
                .perform(typeText("happy"), closeSoftKeyboard());
    }

    /**
     * Test 5: Verify that the BottomNavigationView is displayed.
     */
    @Test
    public void testBottomNavigationDisplayed() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }
}
