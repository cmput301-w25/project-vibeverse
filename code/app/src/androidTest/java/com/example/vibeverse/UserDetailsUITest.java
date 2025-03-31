package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.swipeUp;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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

    @Test
    public void testContinueButtonBlockedWithEmptyFields() {
        try (ActivityScenario<UserDetails> scenario = ActivityScenario.launch(UserDetails.class)) {
            onView(withId(R.id.continueButton)).perform(click());

            // Still on UserDetails screen (assumes recyclerFeed is NOT present)
            onView(withId(R.id.recyclerFeed)).check(doesNotExist());
        }
    }




    @Test
    public void testDatePickerSetsDOBField() {
        try (ActivityScenario<UserDetails> scenario = ActivityScenario.launch(UserDetails.class)) {
            // Click the DOB field to open the DatePicker
            onView(withId(R.id.dob)).perform(click());

            // Wait a little for the dialog to appear (sometimes needed on emulator)
            Thread.sleep(500);

            // Click "OK" on the date picker (assumes default date is okay)
            onView(withText("OK")).perform(click());

            // Check that DOB field now has a non-empty value
            onView(withId(R.id.dob)).check(matches(not(withText(""))));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
