package com.example.vibeverse;

import android.graphics.Color;

public enum Mood {
    HAPPY("Happy", "😃", R.color.happy_color),
    SAD("Sad", "😢", R.color.sad_color),
    ANGRY("Angry", "😡", R.color.angry_color),
    SURPRISED("Surprised", "😲", R.color.surprised_color),
    AFRAID("Afraid", "😨", R.color.afraid_color),
    DISGUSTED("Disgusted", "🤢", R.color.disgusted_color),
    CONFUSED("Confused", "🤔", R.color.confused_color),
    SHAMEFUL("Shameful", "😳", R.color.shameful_color);

    private final String name;
    private final String emoji;
    private final int color;

    Mood(String name, String emoji, int color) {
        this.name = name;
        this.emoji = emoji;
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public String getEmoji() {
        return emoji;
    }

    public int getColor() {
        return color;
    }
}
