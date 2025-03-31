package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;



import android.content.Intent;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class NotificationUITest {
    @Rule
    public ActivityScenarioRule<NotificationsActivity> activityRule =
            new ActivityScenarioRule<>(NotificationsActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void recyclerView_isVisible() {
        onView(withId(R.id.recyclerNotifications)).check(matches(isDisplayed()));
    }

    @Test
    public void bottomNav_isVisible() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
    }

    @Test
    public void backButton_navigatesToHomePage() {
        onView(withId(R.id.buttonBack)).perform(click());
        intended(hasComponent(HomePage.class.getName()));
    }


}