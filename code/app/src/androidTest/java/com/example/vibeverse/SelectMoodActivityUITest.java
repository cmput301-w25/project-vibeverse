package com.example.vibeverse;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

import android.view.View;
import android.widget.SeekBar;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SelectMoodActivityUITest {

    /**
     * Verify that the mood grid contains at least eight mood buttons.
     */
    @Test
    public void testMoodGridHasAtLeastEightButtons() {
        try (ActivityScenario<SelectMoodActivity> scenario = ActivityScenario.launch(SelectMoodActivity.class)) {
            // Check that the GridLayout with id moodGrid has at least 8 children.
            onView(withId(R.id.moodGrid))
                    .check(matches(hasMinimumChildCount(8)));
        }
    }

    /**
     * Verify that the default mood is "Happy" with its emoji "😃".
     */
    @Test
    public void testDefaultMoodIsHappy() {
        try (ActivityScenario<SelectMoodActivity> scenario = ActivityScenario.launch(SelectMoodActivity.class)) {
            // Verify that the selected mood text contains "Happy"
            onView(withId(R.id.selectedMoodText))
                    .check(matches(withText(containsString("Happy"))));
            // And that the selected emoji is the default "😃".
            onView(withId(R.id.selectedMoodEmoji))
                    .check(matches(withText("😃")));
        }
    }

    /**
     * Verify that you can change the mood by tapping on a mood button.
     * For example, when tapping on the "Angry" mood button (which displays the angry emoji "😡"),
     * the selected mood should update accordingly.
     */
    @Test
    public void testChangingMoodToAngryUpdatesUI() {
        try (ActivityScenario<SelectMoodActivity> scenario = ActivityScenario.launch(SelectMoodActivity.class)) {
            // Tap the mood button that displays the angry emoji "😡".
            onView(withText("😡"))
                    .perform(scrollTo(), click());
            // Verify that the selected mood emoji is now "😡".
            onView(withId(R.id.selectedMoodEmoji))
                    .check(matches(withText("😡")));
            // Verify that the mood text now contains "Angry".
            onView(withId(R.id.selectedMoodText))
                    .check(matches(withText(containsString("Angry"))));
        }
    }

    /**
     * Verify that adjusting the intensity slider updates the intensity display.
     */
    @Test
    public void testIntensitySliderUpdatesIndicator() {
        try (ActivityScenario<SelectMoodActivity> scenario = ActivityScenario.launch(SelectMoodActivity.class)) {
            // Set the slider to a high value (e.g., 8 on a 0-10 scale) using a custom action.
            onView(withId(R.id.moodIntensitySlider))
                    .perform(new ViewAction() {
                        @Override
                        public Matcher<View> getConstraints() {
                            // This action is only valid for SeekBars.
                            return isDisplayed();
                        }
                        @Override
                        public String getDescription() {
                            return "Set progress of SeekBar to 8";
                        }
                        @Override
                        public void perform(androidx.test.espresso.UiController uiController, View view) {
                            if (view instanceof SeekBar) {
                                ((SeekBar)view).setProgress(8);
                                uiController.loopMainThreadUntilIdle();
                            }
                        }
                    });
            // Optionally wait a moment for the UI to update.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // Handle interruption if needed.
            }
            // Check that the intensity display shows at least 5 filled circles (●).
            onView(withId(R.id.intensityDisplay))
                    .check(matches(withText(containsString("●●●●●"))));
        }
    }

    /**
     * Verify that tapping the Continue button navigates to the next screen.
     * (Assumes that after selecting a mood, setting intensity, and filling any required text fields,
     * tapping Continue will transition to the next activity that has a view with id recyclerFeed.)
     */
    @Test
    public void testContinueButtonNavigatesForward() throws InterruptedException {
        try (ActivityScenario<SelectMoodActivity> scenario = ActivityScenario.launch(SelectMoodActivity.class)) {
            // Change mood to "Angry" by tapping the button with angry emoji "😡".
            onView(withText("😡"))
                    .perform(scrollTo(), click());

            // Set the intensity slider to 10.
            onView(withId(R.id.moodIntensitySlider))
                    .perform(new ViewAction() {
                        @Override
                        public Matcher<View> getConstraints() {
                            return isDisplayed();
                        }
                        @Override
                        public String getDescription() {
                            return "Set progress of SeekBar to 10";
                        }
                        @Override
                        public void perform(androidx.test.espresso.UiController uiController, View view) {
                            if (view instanceof SeekBar) {
                                ((SeekBar)view).setProgress(10);
                                uiController.loopMainThreadUntilIdle();
                            }
                        }
                    });

            // Fill in the required text fields.
            onView(withId(R.id.triggerInput))
                    .perform(typeText("Traffic"), closeSoftKeyboard());
            onView(withId(R.id.reasonWhyInput))
                    .perform(typeText("Angry about traffic"), closeSoftKeyboard());

            // Tap the Continue button.
            onView(withId(R.id.continueButton))
                    .perform(click());

            // Wait for the transition (ideally, use an IdlingResource rather than Thread.sleep).
            Thread.sleep(3000);

            // Verify that a view unique to the next screen (e.g., a RecyclerView with id recyclerFeed) is displayed.
            onView(withId(R.id.recyclerFeed))
                    .check(matches(isDisplayed()));
        }
    }
}
