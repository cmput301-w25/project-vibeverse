package com.example.vibeverse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoodEvent {
    private String emotionalState;
    private String trigger;
    private String socialSituation;
    private String timestamp; // Formatted timestamp

    public MoodEvent(String emotionalState, String trigger, String socialSituation) {
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
    }

    // Get formatted date & time
    private String getCurrentFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    public String getEmotionalState() { return emotionalState; }
    public String getTrigger() { return trigger; }
    public String getSocialSituation() { return socialSituation; }
    public String getTimestamp() { return timestamp; }
}
