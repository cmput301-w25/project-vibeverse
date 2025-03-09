package com.example.vibeverse;

import android.graphics.Color;

/**
 * Enum representing different moods.
 * <p>
 * Each mood has a display name, an emoji representation, and a color resource
 * associated with it.
 * </p>
 */
public enum Mood {
    /**
     * Represents a happy mood.
     */
    HAPPY("Happy", "😃", R.color.happy_color),

    /**
     * Represents a sad mood.
     */
    SAD("Sad", "😢", R.color.sad_color),

    /**
     * Represents an angry mood.
     */
    ANGRY("Angry", "😡", R.color.angry_color),

    /**
     * Represents a surprised mood.
     */
    SURPRISED("Surprised", "😲", R.color.surprised_color),

    /**
     * Represents an afraid mood.
     */
    AFRAID("Afraid", "😨", R.color.afraid_color),

    /**
     * Represents a disgusted mood.
     */
    DISGUSTED("Disgusted", "🤢", R.color.disgusted_color),

    /**
     * Represents a confused mood.
     */
    CONFUSED("Confused", "🤔", R.color.confused_color),

    /**
     * Represents a shameful mood.
     */
    SHAMEFUL("Shameful", "😳", R.color.shameful_color);

    /** The display name of the mood. */
    private final String name;
    /** The emoji representing the mood. */
    private final String emoji;
    /** The color resource associated with the mood. */
    private final int color;

    /**
     * Constructs a new Mood.
     *
     * @param name  The display name of the mood.
     * @param emoji The emoji representing the mood.
     * @param color The color resource ID associated with the mood.
     */
    Mood(String name, String emoji, int color) {
        this.name = name;
        this.emoji = emoji;
        this.color = color;
    }

    /**
     * Returns the display name of the mood.
     *
     * @return The name of the mood.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the emoji representing the mood.
     *
     * @return The mood emoji.
     */
    public String getEmoji() {
        return emoji;
    }

    /**
     * Returns the color resource ID associated with the mood.
     *
     * @return The color resource ID.
     */
    public int getColor() {
        return color;
    }
}
