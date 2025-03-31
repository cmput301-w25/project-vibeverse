package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;

import android.app.Activity;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.Root;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class RegisterUITest {

    @Rule
    public ActivityScenarioRule<Register> activityRule =
            new ActivityScenarioRule<>(Register.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void allInputFieldsAndButtons_areVisible() {
        onView(withId(R.id.email)).check(matches(isDisplayed()));
        onView(withId(R.id.password)).check(matches(isDisplayed()));
        onView(withId(R.id.register_button)).check(matches(isDisplayed()));
        onView(withId(R.id.loginNow)).check(matches(isDisplayed()));
        onView(withId(R.id.google_sign_up)).check(matches(isDisplayed()));
    }

    @Test
    public void clickingRegisterWithEmptyFields_showsEmailErrorToast() {
        onView(withId(R.id.register_button)).perform(click());
        onView(withText("Please enter your email"))
                .inRoot(new ToastMatcher())
                .check(matches(isDisplayed()));
    }
}
