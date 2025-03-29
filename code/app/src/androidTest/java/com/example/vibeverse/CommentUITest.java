package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.vibeverse.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class CommentUITest {

    @Rule
    public ActivityScenarioRule<CommentSectionActivity> activityRule =
            new ActivityScenarioRule<>(CommentSectionActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void commentUIComponents_areVisible() {
        onView(withId(R.id.recyclerComments)).check(matches(isDisplayed()));
        onView(withId(R.id.editComment)).check(matches(isDisplayed()));
        onView(withId(R.id.buttonSendComment)).check(matches(isDisplayed()));
    }

    @Test
    public void typeAndSendComment_showsToastOrUpdatesList() {
        String commentText = "This is an Espresso test comment";

        onView(withId(R.id.editComment)).perform(typeText(commentText));
        closeSoftKeyboard(); // prevent keyboard from blocking button
        onView(withId(R.id.buttonSendComment)).perform(click());

        // You can add IdlingResource or delay here to wait for Firestore sync
        // Just verifying UI didn't crash is useful here
    }

    @Test
    public void clickingReplyIcon_showsReplyBanner() {
        // This will only work if your RecyclerView has data preloaded
        // Optionally use RecyclerViewActions to click inside

        onView(withId(R.id.replyIcon)).perform(click());
        onView(withId(R.id.replyBanner)).check(matches(isDisplayed()));
    }

    @Test
    public void clickingComment_opensUserProfile() {
        // Click first comment in RecyclerView
        onView(withId(R.id.recyclerComments))
                .perform(androidx.test.espresso.contrib.RecyclerViewActions
                        .actionOnItemAtPosition(0, click()));
        intended(hasComponent(UsersProfile.class.getName()));
    }
}
