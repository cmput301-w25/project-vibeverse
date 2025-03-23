package com.example.vibeverse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@RunWith(AndroidJUnit4.class)
public class LoginUITest {

    /**
     * Test that when the email field is empty and the login button is clicked,
     * a Toast with "Please enter your email" is displayed.
     */
    @Test
    public void testEmptyEmailShowsError() {
        try (ActivityScenario<Login> scenario = ActivityScenario.launch(Login.class)) {
            // Clear the email field and enter a valid password.
            onView(withId(R.id.email)).perform(clearText());
            onView(withId(R.id.password))
                    .perform(typeText("password123"), closeSoftKeyboard());
            // Click the login button.
            onView(withId(R.id.login_button)).perform(click());

            // Verify the Toast message. We need to get the activity’s root view.
            scenario.onActivity(activity -> {
                onView(withText("Please enter your email"))
                        .inRoot(new ToastMatcher());
            });
        }
    }

    /**
     * Test that when the password field is empty and the login button is clicked,
     * a Toast with "Please enter your password" is displayed.
     */
    @Test
    public void testEmptyPasswordShowsError() {
        try (ActivityScenario<Login> scenario = ActivityScenario.launch(Login.class)) {
            // Enter a valid email and clear the password field.
            onView(withId(R.id.email))
                    .perform(typeText("user@example.com"), closeSoftKeyboard());
            onView(withId(R.id.password)).perform(clearText());
            // Click the login button.
            onView(withId(R.id.login_button)).perform(click());

            // Verify the Toast message.
            scenario.onActivity(activity -> {
                onView(withText("Please enter your password"))
                        .inRoot(new ToastMatcher());
            });
        }
    }

    /**
     * Test that a valid login navigates to HomePage.
     * (This test uses Espresso-Intents to check that the proper intent is fired.)
     */
    @Test
    public void testValidLoginNavigatesToHomePage() {
        Intents.init();
        try (ActivityScenario<Login> scenario = ActivityScenario.launch(Login.class)) {
            onView(withId(R.id.email))
                    .perform(typeText("demo@demo.com"), closeSoftKeyboard());
            onView(withId(R.id.password))
                    .perform(typeText("demo1670"), closeSoftKeyboard());
            onView(withId(R.id.login_button)).perform(click());

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            // Verify that an intent to launch HomePage is fired.
            intended(hasComponent(HomePage.class.getName()));
        } finally {
            Intents.release();
        }
    }
}
