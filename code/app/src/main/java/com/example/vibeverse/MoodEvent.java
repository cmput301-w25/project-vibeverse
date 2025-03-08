package com.example.vibeverse;

import static android.content.ContentValues.TAG;

import android.net.Uri;
import android.util.Log;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a mood event recorded by the user.
 * <p>
 * Each MoodEvent contains the emotional state (with an emoji),
 * an optional trigger, an optional social situation, a formatted timestamp,
 * an intensity level, and an optional photograph.
 * </p>
 */
public class MoodEvent implements Serializable {
    private String moodTitle;
    private String moodEmoji;

    private String trigger;

    private String documentId; // Firestore document ID
    private String socialSituation;
    private String timestamp; // Formatted timestamp
    private int intensity = 5; // Default intensity set to middle value
    private Photograph photograph;

    private Date date;

    private String subtitle;


    public String getMoodEmoji() {
        return moodEmoji;
    }

    public void setMoodEmoji(String moodEmoji) {
        this.moodEmoji = moodEmoji;
    }


    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public MoodEvent(String moodTitle, String moodEmoji, String trigger, String socialSituation) {
        this.moodEmoji = moodEmoji;
        this.moodTitle = moodTitle;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
    }


    public MoodEvent(String moodTitle, String moodEmoji, String trigger, String socialSituation, Photograph photograph) {
        this.moodTitle = moodTitle;
        this.moodEmoji = moodEmoji;
        this.trigger = trigger;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
        this.photograph = photograph;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> moodMap = new HashMap<>();
        moodMap.put("emotionalState", this.moodTitle);
        moodMap.put("trigger", this.trigger);
        moodMap.put("socialSituation", this.socialSituation);
        moodMap.put("timestamp", this.timestamp);
        moodMap.put("intensity", this.intensity);

        // Handle emoji and mood text extraction
        moodMap.put("emoji", getEmoji());
        moodMap.put("mood", getMoodTitle());

        // Handle photograph if present
        if (this.photograph != null) {
            moodMap.put("hasPhoto", true);
            moodMap.put("photoUri", getPhotoUri());


            // Add additional photo metadata
            if (photograph.getDateTaken() != null) {
                moodMap.put("photoDateTaken", photograph.getDateTaken().getTime());
            }

            moodMap.put("photoLocation", photograph.getLocation());
            moodMap.put("photoSizeKB", photograph.getFileSizeKB());
        } else {
            moodMap.put("hasPhoto", false);
        }

        return moodMap;
    }
    public static MoodEvent fromMap(Map<String, Object> data) {
        String moodEmoji = (String) data.get("emoji");

        String moodTitle = (String) data.get("mood");

        String trigger = (String) data.get("trigger");
        String socialSituation = (String) data.get("socialSituation");

        Log.d("fromMapDebug", "photoUri field: " + data.get("photoUri"));

        MoodEvent moodEvent = new MoodEvent(moodTitle, moodEmoji, trigger, socialSituation);

        // Set the timestamp if it exists
        if (data.containsKey("timestamp")) {
            moodEvent.setTimestamp((String) data.get("timestamp"));
        }

        // Set intensity if it exists
        if (data.containsKey("intensity")) {
            moodEvent.setIntensity(((Long) data.get("intensity")).intValue());
        }

        // Handle photograph if it exists
        if (data.containsKey("hasPhoto") && (Boolean) data.get("hasPhoto")) {
            String photoUri = (String) data.get("photoUri");
            if (photoUri != null && !photoUri.equals("N/A")) {
                // Create a Photograph object with the available data
                Uri uri = Uri.parse(photoUri);
                Log.d("fromMapDebug", "photoUri after parse: " + photoUri);

                // Get photo metadata if available
                Date photoDate = new Date();
                if (data.containsKey("photoDateTaken")) {
                    Object dateObj = data.get("photoDateTaken");
                    if (dateObj instanceof Long) {
                        photoDate = new Date((Long) dateObj);
                    } else if (dateObj instanceof Timestamp) {
                        photoDate = ((Timestamp) dateObj).toDate();
                    }
                }

                String location = "Unknown";
                if (data.containsKey("photoLocation")) {
                    location = (String) data.get("photoLocation");
                }

                long sizeKB = 0;
                if (data.containsKey("photoSizeKB")) {
                    Object sizeObj = data.get("photoSizeKB");
                    if (sizeObj instanceof Long) {
                        sizeKB = (Long) sizeObj;
                    } else if (sizeObj instanceof Integer) {
                        sizeKB = (Integer) sizeObj;
                    } else if (sizeObj instanceof Double) {
                        sizeKB = ((Double) sizeObj).longValue();
                    }
                }

                // Create photograph without bitmap (which can't be stored in Firestore)
                Photograph photograph = new Photograph(uri, sizeKB, photoDate, location);
                moodEvent.setPhotograph(photograph);
            }
        }

        Log.d("fromMapDebug", "Returning MoodEvent with photoUri: " + moodEvent.getPhotoUri());

        return moodEvent;
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
     * @param moodTitle The new emotional state.
     */
    public void setMoodTitle(String moodTitle) {
        this.moodTitle = moodTitle;
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
     * Gets the intensity level of this mood.
     *
     * @return The intensity level (0-10).
     */
    public int getIntensity() {
        return intensity;
    }

    /**
     * Sets the intensity level of this mood.
     *
     * @param intensity The intensity level (0-10).
     */
    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    /**
     * Returns the emotional state of this mood event.
     *
     * @return The emotional state.
     */
    public String getMoodTitle() { return moodTitle; }

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

    /**
     * Extracts the emoji from the emotional state string.
     * Assumes the emotional state starts with an emoji followed by a space.
     *
     * @return The emoji part of the emotional state.
     */
    public String getEmoji() {
        return this.moodEmoji;
    }

    /**
     * Gets the photo URI as a string if a photograph is attached.
     *
     * @return The photo URI as a string, or "N/A" if no photograph is attached.
     */
    public String getPhotoUri() {
        if (photograph != null && photograph.getImageUri() != null) {
            return photograph.getImageUri().toString();
        }
        return "N/A";
    }
}