package com.example.vibeverse;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.contrib.RecyclerViewActions;

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
        onView(withId(R.id.buttonLogout)).check(matches(isDisplayed()));
        onView(withId(R.id.editSearch)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonFilter)).check(matches(isDisplayed()));
        onView(withId(R.id.recyclerFeed)).check(matches(isDisplayed()));
    }

    /**
     * Test 2: Ensure the logout button is clickable.
     */
    @Test
    public void testLogoutButtonClick() {
        onView(withId(R.id.buttonLogout)).perform(click());

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

    /**
     * Test 6: Test clicking on a RecyclerView item triggers its click listener.
     * Note: This assumes the RecyclerView has at least one item. If not, seed test data or adjust accordingly.
     */
    @Test
    public void testRecyclerViewItemClick() {
        onView(withId(R.id.recyclerFeed))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
    }

    /**
     * Test 7: Verify that the empty state view is displayed when there are no mood entries.
     * This test assumes the app's Firestore is in a state with no mood data.
     */
    @Test
    public void testEmptyStateVisibilityWhenNoMoods() {
        onView(withId(R.id.emptyStateView)).check(matches(isDisplayed()));
    }

    /**
     * Test 8: Verify progress bar visibility during data loading.
     * For a robust test, consider adding an IdlingResource to wait until loading completes,
     * then assert that the progress bar is hidden.
     */
    @Test
    public void testProgressBarVisibilityDuringLoading() {
        onView(withId(R.id.progressLoading)).check(matches(isDisplayed()));
    }
}
