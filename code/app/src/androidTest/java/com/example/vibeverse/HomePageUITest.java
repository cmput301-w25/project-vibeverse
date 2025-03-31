package com.example.vibeverse;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class HomePageUITest {

    @Rule
    public ActivityScenarioRule<HomePage> activityRule =
            new ActivityScenarioRule<>(HomePage.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void homepageComponents_areVisible() {
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonNotification)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonFilter)).check(matches(isDisplayed()));
        onView(withId(R.id.editSearch)).check(matches(isDisplayed()));
    }

    @Test
    public void clickingNotificationButton_opensNotificationsActivity() {
        onView(withId(R.id.buttonNotification)).perform(click());
        intended(hasComponent(NotificationsActivity.class.getName()));
    }

    @Test
    public void clickingFilterButton_opensFilterDialog() {
        onView(withId(R.id.buttonFilter)).perform(click());
        onView(withId(R.id.radioGroupTime)).check(matches(isDisplayed()));
    }

    @Test
    public void typingSearchText_doesNotCrash() {
        onView(withId(R.id.editSearch)).perform(typeText("happy"));
        closeSoftKeyboard();
        // No crash = pass. Firebase not mocked so no data = OK
    }
}
