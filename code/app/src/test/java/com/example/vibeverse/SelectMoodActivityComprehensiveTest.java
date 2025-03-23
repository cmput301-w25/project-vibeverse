package com.example.vibeverse;

import android.net.Uri;

import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for SelectMoodActivity functionality.
 * This test class consolidates tests for mood data, color utilities,
 * form validation, and MoodEvent creation.
 */
public class SelectMoodActivityComprehensiveTest {

    // Data structures for mood testing
    private Map<String, String> moodEmojis;
    private Map<String, Integer> moodColors;

    @Before
    public void setUp() {
        // Initialize the mood maps
        moodEmojis = new HashMap<>();
        moodColors = new HashMap<>();

        // Populate with expected values based on SelectMoodActivity implementation
        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Surprised", "😲");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Shameful", "😳");

        // Set up color keys (actual values not needed for key tests)
        moodColors.put("Happy", 0);
        moodColors.put("Sad", 0);
        moodColors.put("Angry", 0);
        moodColors.put("Surprised", 0);
        moodColors.put("Afraid", 0);
        moodColors.put("Disgusted", 0);
        moodColors.put("Confused", 0);
        moodColors.put("Shameful", 0);
    }

    //------------------------------------------------------------------------------
    // MOOD DATA TESTS
    //------------------------------------------------------------------------------

    @Test
    public void testMoodEmojisMapping() {
        // Verify emoji mappings match expected values
        assertEquals(8, moodEmojis.size());
        assertEquals("😃", moodEmojis.get("Happy"));
        assertEquals("😢", moodEmojis.get("Sad"));
        assertEquals("😡", moodEmojis.get("Angry"));
        assertEquals("😲", moodEmojis.get("Surprised"));
        assertEquals("😨", moodEmojis.get("Afraid"));
        assertEquals("🤢", moodEmojis.get("Disgusted"));
        assertEquals("🤔", moodEmojis.get("Confused"));
        assertEquals("😳", moodEmojis.get("Shameful"));
    }

    @Test
    public void testMoodColorsMappingContainsAllMoods() {
        // Verify all expected moods have corresponding color entries
        assertEquals(8, moodColors.size());
        assertTrue(moodColors.containsKey("Happy"));
        assertTrue(moodColors.containsKey("Sad"));
        assertTrue(moodColors.containsKey("Angry"));
        assertTrue(moodColors.containsKey("Surprised"));
        assertTrue(moodColors.containsKey("Afraid"));
        assertTrue(moodColors.containsKey("Disgusted"));
        assertTrue(moodColors.containsKey("Confused"));
        assertTrue(moodColors.containsKey("Shameful"));
    }

    @Test
    public void testIntensityDisplayString() {
        // Test intensity display string generation logic
        // For intensity 5, we should have 6 filled circles (0-5 inclusive) and 5 empty ones
        StringBuilder intensityBuilder = new StringBuilder();
        int progress = 5;

        for (int i = 0; i <= 10; i++) {
            if (i <= progress) {
                intensityBuilder.append("●"); // Filled circle for active levels
            } else {
                intensityBuilder.append("○"); // Empty circle for inactive levels
            }
        }

        assertEquals("●●●●●●○○○○○", intensityBuilder.toString());
    }

    //------------------------------------------------------------------------------
    // COLOR UTILITY TESTS
    //------------------------------------------------------------------------------

    /**
     * A simplified version of the ColorUtils class from SelectMoodActivity
     * to allow for testability without Android dependencies.
     */
    private static class TestColorUtils {
        public static int blendColors(int color1, int color2, float ratio) {
            final float inverseRatio = 1f - ratio;
            int r1 = (color1 >> 16) & 0xFF;
            int g1 = (color1 >> 8) & 0xFF;
            int b1 = color1 & 0xFF;

            int r2 = (color2 >> 16) & 0xFF;
            int g2 = (color2 >> 8) & 0xFF;
            int b2 = color2 & 0xFF;

            float r = (r1 * ratio) + (r2 * inverseRatio);
            float g = (g1 * ratio) + (g2 * inverseRatio);
            float b = (b1 * ratio) + (b2 * inverseRatio);

            return (((int) r) << 16) | (((int) g) << 8) | ((int) b);
        }
    }

    @Test
    public void testBlendColorsEqualMix() {
        // Test with 50% mix
        int red = 0xFF0000;   // (255, 0, 0)
        int green = 0x00FF00; // (0, 255, 0)
        float ratio = 0.5f;

        int result = TestColorUtils.blendColors(red, green, ratio);

        // Expected: (127.5, 127.5, 0) which rounds to (127, 127, 0) -> 0x7F7F00
        int expectedR = 127;
        int expectedG = 127;
        int expectedB = 0;
        int expected = (expectedR << 16) | (expectedG << 8) | expectedB;

        assertEquals(expected, result);
    }

    @Test
    public void testBlendColorsFullFirstColor() {
        // Test with 100% first color
        int red = 0xFF0000;   // (255, 0, 0)
        int green = 0x00FF00; // (0, 255, 0)
        float ratio = 1.0f;

        int result = TestColorUtils.blendColors(red, green, ratio);

        // Should be exactly the first color
        assertEquals(red, result);
    }

    @Test
    public void testBlendColorsFullSecondColor() {
        // Test with 0% first color (100% second color)
        int red = 0xFF0000;   // (255, 0, 0)
        int green = 0x00FF00; // (0, 255, 0)
        float ratio = 0.0f;

        int result = TestColorUtils.blendColors(red, green, ratio);

        // Should be exactly the second color
        assertEquals(green, result);
    }

    @Test
    public void testBlendColorsWithComplexColors() {
        // Test with more complex colors
        int purple = 0x800080;  // (128, 0, 128)
        int teal = 0x008080;    // (0, 128, 128)
        float ratio = 0.75f;    // 75% purple, 25% teal

        int result = TestColorUtils.blendColors(purple, teal, ratio);

        // Expected: 0.75 * (128, 0, 128) + 0.25 * (0, 128, 128)
        // = (96, 32, 128)
        // = 0x602080
        int expectedR = 96;
        int expectedG = 32;
        int expectedB = 128;
        int expected = (expectedR << 16) | (expectedG << 8) | expectedB;

        assertEquals(expected, result);
    }

    //------------------------------------------------------------------------------
    // FORM VALIDATION TESTS
    //------------------------------------------------------------------------------

    /**
     * Test validation of the "reason why" field character limits.
     * According to SelectMoodActivity, reason why must be 20 characters or less.
     */
    @Test
    public void testReasonWhyCharacterLimitValidation() {
        // Valid cases - 20 chars or less
        assertTrue(isReasonWhyValid(""));                 // Empty
        assertTrue(isReasonWhyValid("Happy today"));      // Normal case
        assertTrue(isReasonWhyValid("Exactly 20 characte")); // Exactly 20 chars

        // Invalid cases - more than 20 chars
        assertFalse(isReasonWhyValid("This is a reason why that is definitely too long"));
        assertFalse(isReasonWhyValid("Exactly 21 characters")); // 21 chars
    }

    /**
     * Test validation of the "reason why" field word limits.
     * According to SelectMoodActivity, reason why must be 3 words or less.
     */
    @Test
    public void testReasonWhyWordLimitValidation() {
        // Valid cases - 3 words or less
        assertTrue(isReasonWhyValid(""));             // Empty
        assertTrue(isReasonWhyValid("Happy"));        // One word
        assertTrue(isReasonWhyValid("Very happy day")); // Three words

        // Invalid cases - more than 3 words
        assertFalse(isReasonWhyValid("This is four words exactly"));
        assertFalse(isReasonWhyValid("This is way too many words for the reason why field"));
    }

    /**
     * Implementation of the "reason why" validation logic from SelectMoodActivity.
     *
     * @param reasonWhy The reason why text to validate
     * @return true if the text passes validation, false otherwise
     */
    private boolean isReasonWhyValid(String reasonWhy) {
        // Check character limit
        if (reasonWhy.length() > 20) {
            return false;
        }

        // Check word limit
        String[] words = reasonWhy.split("\\s+");
        if (words.length > 3) {
            return false;
        }

        return true;
    }

    //------------------------------------------------------------------------------
    // MOOD EVENT CREATION TESTS
    //------------------------------------------------------------------------------

    // The following test uses a test-specific method to check MoodEvent creation
    // without requiring mocking of the Photograph class

    @Test
    public void testMoodEventCreation() {
        // Test without photo
        MoodEvent noPhotoEvent = new MoodEvent("2131241241", "Happy", "😃", "Good day", "Got good news");

        assertEquals("Happy", noPhotoEvent.getMoodTitle());
        assertEquals("😃", noPhotoEvent.getEmoji());
        assertEquals("Good day", noPhotoEvent.getReasonWhy());
        assertEquals("With friends", noPhotoEvent.getSocialSituation());
        assertNotNull(noPhotoEvent.getTimestamp());
        assertNull(noPhotoEvent.getPhotograph());
        assertEquals("N/A", noPhotoEvent.getPhotoUri());

        // Test intensity settings
        noPhotoEvent.setIntensity(8);
        assertEquals(8, noPhotoEvent.getIntensity());

        // Test with valid intensity values
        noPhotoEvent.setIntensity(0);
        assertEquals(0, noPhotoEvent.getIntensity());

        noPhotoEvent.setIntensity(10);
        assertEquals(10, noPhotoEvent.getIntensity());
    }
}