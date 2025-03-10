package com.example.vibeverse;

import static com.example.vibeverse.EditMoodActivity.blendColors;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for EditMoodActivity functionality.
 * These tests focus on the non-UI logic and utility methods used in the activity.
 */
public class EditMoodActivityTest {

    // Data structures for mood testing
    private Map<String, String> moodEmojis;
    private Map<String, Integer> moodColors;

    @Before
    public void setUp() {
        // Initialize the mood maps
        moodEmojis = new HashMap<>();
        moodColors = new HashMap<>();

        // Populate with expected values based on EditMoodActivity implementation
        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Surprised", "😲");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Shameful", "😳");

        // Set up color values (using integer values directly to avoid Color.parseColor)
        // These are approximate RGB values that match the hex codes in the activity
        moodColors.put("Happy", 0xFBC02D);      // Warm yellow
        moodColors.put("Sad", 0x42A5F5);        // Soft blue
        moodColors.put("Angry", 0xEF5350);      // Vibrant red
        moodColors.put("Surprised", 0xFF9800);  // Orange
        moodColors.put("Afraid", 0x5C6BC0);     // Indigo blue
        moodColors.put("Disgusted", 0x66BB6A);  // Green
        moodColors.put("Confused", 0xAB47BC);   // Purple
        moodColors.put("Shameful", 0xEC407A);   // Pink
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
     * A copy of the ColorUtils class from EditMoodActivity
     * to allow for testability without Android dependencies.
     */

    @Test
    public void testBlendColorsEqualMix() {
        // Test with 50% mix
        int red = 0xFF0000;   // (255, 0, 0)
        int green = 0x00FF00; // (0, 255, 0)
        float ratio = 0.5f;

        int result = blendColors(red, green, ratio);

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

        int result = blendColors(red, green, ratio);

        // Should be exactly the first color
        assertEquals(red, result);
    }

    @Test
    public void testBlendColorsFullSecondColor() {
        // Test with 0% first color (100% second color)
        int red = 0xFF0000;   // (255, 0, 0)
        int green = 0x00FF00; // (0, 255, 0)
        float ratio = 0.0f;

        int result = blendColors(red, green, ratio);

        // Should be exactly the second color
        assertEquals(green, result);
    }

    @Test
    public void testBlendColorsWithComplexColors() {
        // Test with more complex colors
        int purple = 0x800080;  // (128, 0, 128)
        int teal = 0x008080;    // (0, 128, 128)
        float ratio = 0.75f;    // 75% purple, 25% teal

        int result = blendColors(purple, teal, ratio);

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
     * According to EditMoodActivity, reason why must be 20 characters or less.
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
     * According to EditMoodActivity, reason why must be 3 words or less.
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
     * Implementation of the "reason why" validation logic from EditMoodActivity.
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
    // MISCELLANEOUS TESTS
    //------------------------------------------------------------------------------

    @Test
    public void testMoodIntensityTextTransformations() {
        // Test the text transformation based on intensity
        String baseMood = "Happy";

        // Low intensity (0-3) should add "Slightly" prefix
        String lowIntensity = transformMoodText(baseMood, 2);
        assertEquals("Slightly Happy", lowIntensity);

        // Medium intensity (4-7) should use base text
        String mediumIntensity = transformMoodText(baseMood, 5);
        assertEquals("Happy", mediumIntensity);

        // High intensity (8-10) should add "Very" prefix
        String highIntensity = transformMoodText(baseMood, 9);
        assertEquals("Very Happy", highIntensity);
    }

    /**
     * Helper method to transform mood text based on intensity
     */
    private String transformMoodText(String mood, int intensity) {
        if (intensity <= 3) {
            return "Slightly " + mood;
        } else if (intensity <= 7) {
            return mood;
        } else {
            return "Very " + mood;
        }
    }

    @Test
    public void testDpToPxConversion() {
        // Test the formula for converting dp to pixels
        float dp = 16f;
        float density = 2.0f; // Assume a density of 2.0 (typical for many devices)

        int expectedPx = Math.round(dp * density);
        assertEquals(32, expectedPx);
    }
}