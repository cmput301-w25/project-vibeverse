package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class UserDetailsUITest {

    /**
     * Test that when required fields are empty, appropriate errors are shown.
     */
    @Test
    public void testEmptyFieldsShowError() {
        try (ActivityScenario<UserDetails> scenario = ActivityScenario.launch(UserDetails.class)) {
            // Clear required fields.
            onView(withId(R.id.fullName)).perform(typeText(""), closeSoftKeyboard());
            onView(withId(R.id.username)).perform(typeText(""), closeSoftKeyboard());
            onView(withId(R.id.bio)).perform(typeText(""), closeSoftKeyboard());
            onView(withId(R.id.dob)).perform(typeText(""), closeSoftKeyboard());
            // Tap Continue button.
            onView(withId(R.id.continueButton)).perform(click());
            // Verify that error messages are shown (assumes error text "Required!" is used).
            onView(withId(R.id.fullName)).check(matches(hasErrorText("Required!")));
            onView(withId(R.id.username)).check(matches(hasErrorText("Required!")));
            onView(withId(R.id.bio)).check(matches(hasErrorText("Required!")));
            onView(withId(R.id.dob)).check(matches(hasErrorText("Required!")));
        }
    }

    /**
     * Test that entering valid user details navigates forward.
     * (Assumes that after filling in the fields and tapping Continue, the app navigates to MainActivity.)
     */
    @Test
    public void testValidUserDetailsNavigatesForward() throws InterruptedException {
        try (ActivityScenario<UserDetails> scenario = ActivityScenario.launch(UserDetails.class)) {
            // Fill in valid user details.
            onView(withId(R.id.fullName))
                    .perform(typeText("Test User"), closeSoftKeyboard());
            onView(withId(R.id.username))
                    .perform(typeText("TestUser1234"), closeSoftKeyboard());
            onView(withId(R.id.bio))
                    .perform(typeText("This is a test bio"), closeSoftKeyboard());
            onView(withId(R.id.dob))
                    .perform(click());
            onView(withText("OK"))
                    .perform(click());

            // Simulate selecting a gender from the spinner.
            onView(withId(R.id.genderSpinner)).perform(click());
            // Select the gender "Male" (assuming "Male" is one of the options).
            onData(is(equalTo("Male"))).perform(click());
            // Tap the Continue button.
            onView(withId(R.id.continueButton)).perform(click());
            // Wait for the transition.
            Thread.sleep(2000);
            // Verify that a view unique to the next screen is displayed.
            // For example, if MainActivity has a RecyclerView with id recyclerFeed:
            onView(withId(R.id.recyclerFeed)).check(matches(isDisplayed()));
        }
    }
}
