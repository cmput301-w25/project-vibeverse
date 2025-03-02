package com.example.vibeverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a mood event recorded by the user.
 * <p>
 * Each MoodEvent contains the emotional state (with an emoji),
 * an optional trigger, an optional social situation, a formatted timestamp,
 * and an optional photograph.
 * </p>
 */
public class MoodEvent implements Serializable {
    private String emotionalState;
    private String trigger;
    private String socialSituation;
    private String timestamp; // Formatted timestamp

    private Photograph photograph;

    /**
     * Constructs a new MoodEvent without an associated photograph.
     *
     * @param emotionalState  The emotional state of the mood event.
     * @param trigger         The trigger for the mood event (optional).
     * @param socialSituation The social situation during the mood event (optional).
     */
    public MoodEvent(String emotionalState, String trigger, String socialSituation) {
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
    }

    /**
     * Constructs a new MoodEvent with an associated photograph.
     *
     * @param emotionalState  The emotional state of the mood event.
     * @param trigger         The trigger for the mood event (optional).
     * @param socialSituation The social situation during the mood event (optional).
     * @param photograph      The photograph associated with the mood event.
     */
    public MoodEvent(String emotionalState, String trigger, String socialSituation, Photograph photograph) {
        this.emotionalState = emotionalState;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
        this.photograph = photograph;
    }



    /**
     * Returns the current date and time formatted as "MMM dd, yyyy - hh:mm a".
     *
     * @return A formatted timestamp string.
     */
    private String getCurrentFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * Returns the Photograph associated with this mood event.
     *
     * @return The Photograph object, or null if none is set.
     */
    public Photograph getPhotograph() {
        return photograph;
    }

    /**
     * Sets the emotional state for this mood event.
     *
     * @param emotionalState The new emotional state.
     */
    public void setEmotionalState(String emotionalState) {
        this.emotionalState = emotionalState;
    }

    /**
     * Sets the trigger for this mood event.
     *
     * @param trigger The new trigger.
     */
    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    /**
     * Sets the social situation for this mood event.
     *
     * @param socialSituation The new social situation.
     */
    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    /**
     * Sets the timestamp for this mood event.
     *
     * @param timestamp The new formatted timestamp.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the Photograph associated with this mood event.
     *
     * @param photograph The Photograph to set.
     */
    public void setPhotograph(Photograph photograph) {
        this.photograph = photograph;
    }

    /**
     * Returns the emotional state of this mood event.
     *
     * @return The emotional state.
     */
    public String getEmotionalState() { return emotionalState; }

    /**
     * Returns the trigger of this mood event.
     *
     * @return The trigger.
     */
    public String getTrigger() { return trigger; }

    /**
     * Returns the social situation of this mood event.
     *
     * @return The social situation.
     */
    public String getSocialSituation() { return socialSituation; }

    /**
     * Returns the formatted timestamp of when this mood event was created.
     *
     * @return The timestamp.
     */
    public String getTimestamp() { return timestamp; }
}
