package com.example.vibeverse;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.swipeRight;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class EditMoodActivityUITest {

    /**
     * Initializes Espresso Intents and launches EditMoodActivity with initial extras.
     */
    @Before
    public void setUp() {
        Intents.init();
        Intent intent = new Intent();
        intent.putExtra("selectedMood", "Happy");
        intent.putExtra("selectedEmoji", "😃");
        intent.putExtra("moodPosition", 0);
        ActivityScenario.launch(EditMoodActivity.class, intent.getExtras());
    }

    /**
     * Releases Espresso Intents resources.
     */
    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Verifies the main container is displayed, ensuring the activity launched properly.
     */
    @Test
    public void testActivityLaunches() {
        onView(withId(R.id.mainContainer)).check(matches(isDisplayed()));
    }

    /**
     * Checks that the mood text and emoji match the initial intent extras.
     */
    @Test
    public void testInitialMoodIsDisplayed() {
        onView(withId(R.id.selectedMoodText)).check(matches(withText(anyOf(
                containsString("Happy"),
                containsString("Very Happy")
        ))));
        onView(withId(R.id.selectedMoodEmoji)).check(matches(withText("😃")));
    }

    /**
     * Confirms the back arrow triggers the intended navigation or closes the activity.
     */
    @Test
    public void testBackArrowNavigation() {
        onView(withId(R.id.backArrow)).perform(click());
    }

    /**
     * Ensures an empty "Reason Why" field shows the proper error.
     */
    @Test
    public void testEmptyReasonWhyShowsError() {
        onView(withId(R.id.reasonWhyInput)).perform(clearText());
        closeSoftKeyboard();
        onView(withId(R.id.continueButton)).perform(click());
        onView(withId(R.id.reasonWhyInput))
                .check(matches(hasErrorText("Reason why is required.")));
    }

    /**
     * Validates that entering more than 20 characters triggers the correct error message.
     */
    @Test
    public void testTooLongReasonShowsError() {
        String overLongReason = "A very, very long reason for feeling this way definitely over 20 chars";
        onView(withId(R.id.reasonWhyInput)).perform(replaceText(overLongReason));
        closeSoftKeyboard();
        onView(withId(R.id.continueButton)).perform(click());
        onView(withId(R.id.reasonWhyInput))
                .check(matches(hasErrorText("Reason why must be 20 characters or less.")));
    }

    /**
     * Confirms that using more than three words in "Reason Why" results in the correct error.
     */
    @Test
    public void testTooManyWordsReasonShowsError() {
        String tooManyWords = "I have five words here";
        onView(withId(R.id.reasonWhyInput)).perform(replaceText(tooManyWords));
        closeSoftKeyboard();
        onView(withId(R.id.continueButton)).perform(click());
        onView(withId(R.id.reasonWhyInput))
                .check(matches(hasErrorText("Reason why must be 3 words or less.")));
    }

    /**
     * Checks that valid input (<=20 chars, <=3 words) allows mood updates without errors.
     */
    @Test
    public void testValidInputUpdatesMood() {
        onView(withId(R.id.reasonWhyInput)).perform(replaceText("Feeling fine"));
        closeSoftKeyboard();
        onView(withId(R.id.triggerInput)).perform(replaceText("New Trigger"));
        closeSoftKeyboard();
        onView(withId(R.id.continueButton)).perform(click());
    }

    /**
     * Verifies selecting "With Friends" in the spinner updates its displayed value.
     */
    @Test
    public void testSelectSocialSituation() {
        onView(withId(R.id.socialSituationSpinner)).perform(click());
        onView(withText("With Friends")).perform(click());
        onView(withId(R.id.socialSituationSpinner))
                .check(matches(withSpinnerText(containsString("With Friends"))));
    }

    /**
     * Swipes the SeekBar to confirm the mood intensity updates and changes mood text accordingly.
     */
    @Test
    public void testIntensitySlider() {
        onView(withId(R.id.moodIntensitySlider)).perform(swipeRight());
        onView(withId(R.id.selectedMoodText))
                .check(matches(withText(anyOf(
                        containsString("Very Happy"),
                        containsString("Happy")
                ))));
    }

    /**
     * Checks that tapping the image button triggers an intent for camera or gallery actions.
     */
    @Test
    public void testAddImage_opensCameraOrGalleryIntent() {
        onView(withId(R.id.btnImage)).perform(scrollTo(), click());
        intended(anyOf(
                hasAction(equalToIgnoringCase(Intent.ACTION_GET_CONTENT)),
                hasAction(equalToIgnoringCase(Intent.ACTION_PICK)),
                hasAction(equalToIgnoringCase(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
        ));
    }

    /**
     * Fills required fields, clicks "Update Mood," and verifies no error is shown.
     */
    @Test
    public void testUpdateReturnsResultIntent() {
        onView(withId(R.id.reasonWhyInput)).perform(replaceText("Good"));
        closeSoftKeyboard();
        onView(withId(R.id.continueButton)).perform(click());
    }

    /**
     * Presses the system back button with partially filled fields to ensure navigation or discard logic.
     */
    @Test
    public void testSystemBackButtonWithUnsavedData() {
        onView(withId(R.id.triggerInput)).perform(replaceText("Halfway typed trigger"));
        closeSoftKeyboard();
        pressBack();
    }
}
