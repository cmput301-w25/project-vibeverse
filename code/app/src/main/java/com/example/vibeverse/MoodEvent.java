package com.example.vibeverse;

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
 * Each MoodEvent contains details such as the emotional state (with an emoji),
 * an optional social situation, a formatted timestamp,
 * an intensity level (default is 5), and an optional photograph.
 * A MoodEvent can be converted to/from a Map for storage in Firestore.
 * </p>
 */
public class MoodEvent implements Serializable {
    private String moodTitle;
    private String moodEmoji;
    private String reasonWhy;
  
    private String documentId; // Firestore document ID
    private String socialSituation;
    private String timestamp; // Formatted timestamp
    private int intensity = 5; // Default intensity set to middle value
    private Photograph photograph;
    private Date date;
    private String subtitle;

    /**
     * Returns the emoji representing the mood.
     *
     * @return The mood emoji.
     */
    public String getMoodEmoji() {
        return moodEmoji;
    }

    /**
     * Sets the emoji representing the mood.
     *
     * @param moodEmoji The emoji to set.
     */
    public void setMoodEmoji(String moodEmoji) {
        this.moodEmoji = moodEmoji;
    }

    /**
     * Returns the Firestore document ID for this mood event.
     *
     * @return The document ID.
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Sets the Firestore document ID for this mood event.
     *
     * @param documentId The document ID to set.
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Returns the subtitle for this mood event.
     *
     * @return The subtitle string.
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * Sets the subtitle for this mood event.
     *
     * @param subtitle The subtitle to set.
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    /**
     * Returns the date object representing when this mood event was created.
     *
     * @return The date of the mood event.
     */
    public Date getDate() {
        return date;
    }

    /**
     * Sets the date for this mood event.
     *
     * @param date The date to set.
     */
    public void setDate(Date date) {
        this.date = date;
    }


    /**
     * Constructs a new MoodEvent with the given mood title, emoji, and social situation.
     * The timestamp is automatically set to the current date and time.
     *
     * @param moodTitle       The emotional state (e.g., "Happy").
     * @param moodEmoji       The emoji representing the mood.
     * @param reasonWhy       The reason for the mood.
     * @param socialSituation The social situation when the mood was recorded.
     */
    public MoodEvent(String moodTitle, String moodEmoji, String reasonWhy, String socialSituation) {
        this.reasonWhy = reasonWhy;
        this.moodEmoji = moodEmoji;
        this.moodTitle = moodTitle;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
    }


    /**
     * Constructs a new MoodEvent with an associated Photograph.
     *
     * @param moodTitle       The emotional state.
     * @param moodEmoji       The emoji representing the mood.
     * @param reasonWhy       The reason for the mood.
     * @param socialSituation The social situation.
     * @param photograph      The Photograph associated with this mood event.
     */

    public MoodEvent(String moodTitle, String moodEmoji, String reasonWhy, String socialSituation, Photograph photograph) {
        this.reasonWhy = reasonWhy;
        this.moodTitle = moodTitle;
        this.moodEmoji = moodEmoji;
        this.socialSituation = socialSituation;
        this.timestamp = getCurrentFormattedTime();
        this.photograph = photograph;
    }

    /**
     * Converts this MoodEvent into a Map for storage in Firestore.
     *
     * @return A Map representation of this MoodEvent.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> moodMap = new HashMap<>();
        moodMap.put("emotionalState", this.moodTitle);
        moodMap.put("socialSituation", this.socialSituation);
        moodMap.put("timestamp", this.timestamp);
        moodMap.put("intensity", this.intensity);
        moodMap.put("reasonWhy", this.reasonWhy);

        // Store emoji and mood title
        moodMap.put("emoji", getEmoji());
        moodMap.put("mood", getMoodTitle());

        // Handle photograph data if present
        if (this.photograph != null) {
            moodMap.put("hasPhoto", true);
            moodMap.put("photoUri", getPhotoUri());

            if (photograph.getDateTaken() != null) {
                moodMap.put("photoDateTaken", photograph.getDateTaken().getTime());
            }
            moodMap.put("photoLocation", photograph.getLocation());
            moodMap.put("photoSizeKB", photograph.getFileSize());
        } else {
            moodMap.put("hasPhoto", false);
        }

        return moodMap;
    }

    /**
     * Creates a MoodEvent from a Map retrieved from Firestore.
     *
     * @param data The Map containing MoodEvent data.
     * @return A new MoodEvent instance.
     */
    public static MoodEvent fromMap(Map<String, Object> data) {
        String moodEmoji = (String) data.get("emoji");
        String moodTitle = (String) data.get("mood");
        String socialSituation = (String) data.get("socialSituation");
        String reasonWhy = (String) data.get("reasonWhy");

        Log.d("fromMapDebug", "photoUri field: " + data.get("photoUri"));

        MoodEvent moodEvent = new MoodEvent(moodTitle, moodEmoji, reasonWhy, socialSituation);

        // Set the timestamp if available
        if (data.containsKey("timestamp")) {
            moodEvent.setTimestamp((String) data.get("timestamp"));
        }

        // Set intensity if available
        if (data.containsKey("intensity")) {
            moodEvent.setIntensity(((Long) data.get("intensity")).intValue());
        }

        // Process photograph data if present
        if (data.containsKey("hasPhoto") && (Boolean) data.get("hasPhoto")) {
            String photoUri = (String) data.get("photoUri");
            if (photoUri != null && !photoUri.equals("N/A")) {
                Uri uri = Uri.parse(photoUri);
                Log.d("fromMapDebug", "photoUri after parse: " + photoUri);

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

                // Create a Photograph object without bitmap data
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
     * @return The Photograph object, or null if no photograph is attached.
     */
    public Photograph getPhotograph() {
        return photograph;
    }

    /**
     * Sets the emotional state (mood title) for this mood event.
     *
     * @param moodTitle The mood title to set.
     */
    public void setMoodTitle(String moodTitle) {
        this.moodTitle = moodTitle;
    }


    /**
     * Sets the social situation for this mood event.
     *
     * @param socialSituation The social situation to set.
     */
    public void setSocialSituation(String socialSituation) {
        this.socialSituation = socialSituation;
    }

    /**
     * Sets the formatted timestamp for this mood event.
     *
     * @param timestamp The timestamp string to set.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Sets the Photograph associated with this mood event.
     *
     * @param photograph The Photograph to attach.
     */
    public void setPhotograph(Photograph photograph) {
        this.photograph = photograph;
    }

    /**
     * Returns the intensity level of this mood.
     *
     * @return The intensity level (0-10).
     */
    public int getIntensity() {
        return intensity;
    }

    /**
     * Sets the intensity level for this mood event.
     *
     * @param intensity The intensity level (0-10) to set.
     */
    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public String getReasonWhy() {
        return reasonWhy;
    }

    public void setReasonWhy(String reasonWhy) {
        this.reasonWhy = reasonWhy;
    }

    /**
     * Returns the mood title (emotional state) of this mood event.
     *
     * @return The mood title.
     */
    public String getMoodTitle() { return moodTitle; }

    /**
     * Returns the social situation of this mood event.
     *
     * @return The social situation string.
     */
    public String getSocialSituation() { return socialSituation; }

    /**
     * Returns the formatted timestamp of when this mood event was created.
     *
     * @return The timestamp string.
     */
    public String getTimestamp() { return timestamp; }

    /**
     * Returns the emoji representing the mood.
     * <p>
     * This method extracts the emoji from the moodEmoji field.
     * </p>
     *
     * @return The mood emoji.
     */
    public String getEmoji() {
        return this.moodEmoji;
    }

    /**
     * Returns the photo URI as a string if a photograph is attached.
     *
     * @return The photo URI string, or "N/A" if no photograph is attached.
     */
    public String getPhotoUri() {
        if (photograph != null && photograph.getImageUri() != null) {
            return photograph.getImageUri().toString();
        }
        return "N/A";
    }

    public long getPhotoSize() {
        if (photograph != null) {
            return photograph.getFileSize();
        }
        return 0;
    }

    public Date getPhotoDate() {
        if (photograph != null) {
            return photograph.getDateTaken();
        }
        return null;
    }

    public String getPhotoLocation() {
        if (photograph != null) {
            return photograph.getLocation();
        }
        return "Unknown";
    }
}