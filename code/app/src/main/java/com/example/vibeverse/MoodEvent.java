package com.example.vibeverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MoodEvent implements Serializable {
    private String emotionalState;
    private String trigger;
    private String socialSituation;
    private String timestamp; // Formatted timestamp

    private Photograph photograph;

    public MoodEvent(String emotionalState, String trigger, String socialSituation) {
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
    }
    public MoodEvent(String emotionalState, String trigger, String socialSituation, Photograph photograph) {
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
        this.photograph = photograph;
    }



    // Get formatted date & time
    private String getCurrentFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    public Photograph getPhotograph() {
        return photograph;
    }

    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public void setPhotograph(Photograph photograph) {
        this.photograph = photograph;
    }

    public String getEmotionalState() { return emotionalState; }
    public String getTrigger() { return trigger; }
    public String getSocialSituation() { return socialSituation; }
    public String getTimestamp() { return timestamp; }
}
