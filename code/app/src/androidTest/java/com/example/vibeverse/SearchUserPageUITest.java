package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.*;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SearchUserPageUITest {

    @Rule
    public ActivityTestRule<SearchUserPage> activityRule = new ActivityTestRule<>(SearchUserPage.class, true, false);

    @Test
    public void testSearchBarIsVisible() {
        activityRule.launchActivity(new Intent());
        onView(withId(R.id.editSearch)).check(matches(isDisplayed()));
    }

    @Test
    public void testTypingInSearchFieldTriggersSearch() throws InterruptedException {
        activityRule.launchActivity(new Intent());
        onView(withId(R.id.editSearch)).perform(typeText("john"));
        Thread.sleep(2000); 
        onView(withId(R.id.recyclerSearchResults)).check(matches(isDisplayed()));
    }

    @Test
    public void testRecyclerViewIsClearedOnEmptyInput() throws InterruptedException {
        activityRule.launchActivity(new Intent());
        onView(withId(R.id.editSearch)).perform(typeText("something"), clearText());
        Thread.sleep(2000);
        onView(withId(R.id.recyclerSearchResults)).check(matches(hasChildCount(0)));
    }
}
