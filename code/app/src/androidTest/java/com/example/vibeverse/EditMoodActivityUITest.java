package com.example.vibeverse;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class EditMoodActivityUITest {

    private Intent createIntentWithExtras() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EditMoodActivity.class);
        intent.putExtra("selectedMood", "Happy");
        intent.putExtra("selectedEmoji", "😃");
        intent.putExtra("trigger", "Birthday");
        intent.putExtra("reasonWhy", "Feeling great");
        intent.putExtra("socialSituation", "With a crowd");
        intent.putExtra("moodPosition", 0);
        intent.putExtra("timestamp", "Mar 15, 2025 - 10:00 AM");
        intent.putExtra("photoUri", "N/A");
        return intent;
    }

    @Test
    public void testPrepopulatedFields() {
        Intent intent = createIntentWithExtras();
        try (ActivityScenario<EditMoodActivity> scenario = ActivityScenario.launch(intent)) {
            // Verify UI elements display expected values.
            onView(withId(R.id.selectedMoodEmoji))
                    .check(matches(withText("😃")));
            onView(withId(R.id.selectedMoodText))
                    .check(matches(withText("Happy")));
            onView(withId(R.id.triggerInput))
                    .check(matches(withText("Birthday")));
            onView(withId(R.id.reasonWhyInput))
                    .check(matches(withText("Feeling great")));
            onView(withId(R.id.socialSituationSpinner))
                    .check(matches(withSpinnerText(containsString("With a crowd"))));
        }
    }

    @Test
    public void testEmptyReasonWhyValidation() {
        Intent intent = createIntentWithExtras();
        try (ActivityScenario<EditMoodActivity> scenario = ActivityScenario.launch(intent)) {
            // Clear the "reason why" field.
            onView(withId(R.id.reasonWhyInput)).perform(clearText());
            // Click the update button
            onView(withId(R.id.updateButton)).perform(click());
            // Verify error message.
            onView(withId(R.id.reasonWhyInput))
                    .check(matches(hasErrorText("Reason why is required.")));
        }
    }

    @Test
    public void testExceedWordCountValidation() {
        Intent intent = createIntentWithExtras();
        try (ActivityScenario<EditMoodActivity> scenario = ActivityScenario.launch(intent)) {
            // Input more than 3 words into the reason why field.
            onView(withId(R.id.reasonWhyInput))
                    .perform(clearText(), typeText("This is definitely too many words"), closeSoftKeyboard());
            onView(withId(R.id.updateButton)).perform(click());
            // Verify error message.
            onView(withId(R.id.reasonWhyInput))
                    .check(matches(hasErrorText("Reason why must be 3 words or less.")));
        }
    }
}

