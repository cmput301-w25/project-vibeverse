package com.example.vibeverse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for the ProfilePage activity's logic.
 *
 * These tests focus purely on the business logic without any Android framework or Firebase dependencies.
 */
@RunWith(JUnit4.class)
public class ProfilePageTest {

    // Fixed reference date for testing: January 10, 2023
    private static final Date REFERENCE_DATE;
    static {
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 10, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        REFERENCE_DATE = cal.getTime();
    }

    /**
     * Mock version of Uri for unit tests
     * This replaces android.net.Uri to avoid Android dependencies in unit tests
     */
    static class Uri implements Serializable {
        private final String uriString;

        private Uri(String uriString) {
            this.uriString = uriString;
        }

        public static Uri parse(String uriString) {
            return new Uri(uriString);
        }

        @Override
        public String toString() {
            return uriString;
        }
    }

    /**
     * Mock version of the Photograph class that doesn't depend on Android classes
     * This is a direct implementation that matches the real Photograph class's interface
     * but avoids dependencies on Android-specific classes (like real Bitmap)
     */
    static class Photograph implements Serializable {
        private Uri imageUri;
        private String imageUriString;
        private long fileSizeKB;
        private Object bitmap; // Replaced Bitmap with Object for mock
        private Date dateTaken;
        private String location;

        /**
         * Constructs a Photograph with a bitmap (mock version)
         */
        public Photograph(Uri imageUri, long fileSizeKB, Object bitmap, Date dateTaken, String location) {
            this.imageUri = imageUri;
            this.imageUriString = imageUri != null ? imageUri.toString() : null;
            this.fileSizeKB = fileSizeKB;
            this.bitmap = bitmap;
            this.dateTaken = dateTaken;
            this.location = location;
        }

        /**
         * Constructs a Photograph without a bitmap (mock version)
         */
        public Photograph(Uri imageUri, long fileSizeKB, Date dateTaken, String location) {
            this.imageUri = imageUri;
            this.imageUriString = imageUri != null ? imageUri.toString() : null;
            this.fileSizeKB = fileSizeKB;
            this.dateTaken = dateTaken;
            this.location = location;
        }

        // For testing only - constructor that accepts a String uri
        public Photograph(String imageUriString, long fileSizeKB, Date dateTaken, String location) {
            this.imageUri = imageUriString != null ? Uri.parse(imageUriString) : null;
            this.imageUriString = imageUriString;
            this.fileSizeKB = fileSizeKB;
            this.dateTaken = dateTaken;
            this.location = location;
        }

        public String getImageUriString() {
            return imageUriString;
        }

        public void setImageUriString(String imageUriString) {
            this.imageUriString = imageUriString;
            if (imageUriString != null) {
                this.imageUri = Uri.parse(imageUriString);
            }
        }

        public void setFileSizeKB(long fileSizeKB) {
            this.fileSizeKB = fileSizeKB;
        }

        public void setBitmap(Object bitmap) {
            this.bitmap = bitmap;
        }

        public void setDateTaken(Date dateTaken) {
            this.dateTaken = dateTaken;
        }

        public void setLocation(String location) {
            this.location = location;
        }

        public void setImageUri(Uri imageUri) {
            this.imageUri = imageUri;
            this.imageUriString = imageUri != null ? imageUri.toString() : null;
        }

        public Uri getImageUri() {
            return imageUri;
        }

        public long getFileSizeKB() {
            return fileSizeKB;
        }

        public Object getBitmap() {
            return bitmap;
        }

        public Date getDateTaken() {
            return dateTaken;
        }

        public String getLocation() {
            return location;
        }

        public String getFormattedDetails() {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            String dateStr = dateTaken != null ? sdf.format(dateTaken) : "Date unknown";
            String locationStr = location != null ? location : "Location unknown";

            String sizeStr;
            if (fileSizeKB >= 1024) {
                float sizeMB = fileSizeKB / 1024f;
                sizeStr = String.format(Locale.getDefault(), "%.2f MB", sizeMB);
            } else {
                sizeStr = fileSizeKB + " KB";
            }

            return String.format(Locale.getDefault(),
                    "Date: %s\nSize: %s\nLocation: %s",
                    dateStr,
                    sizeStr,
                    locationStr);
        }
    }

    /**
     * Mock MoodEvent class for testing
     */
    static class MoodEvent implements Serializable {
        private String moodTitle;
        private String moodEmoji;
        private String trigger;
        private String reasonWhy;
        private String documentId;
        private String socialSituation;
        private String timestamp;
        private int intensity = 5;
        private Photograph photograph;
        private Date date;
        private String subtitle;

        // Default constructor
        public MoodEvent() {
        }

        // Constructor with basic fields
        public MoodEvent(String moodTitle, String moodEmoji, String reasonWhy, String trigger, String socialSituation) {
            this.moodTitle = moodTitle;
            this.moodEmoji = moodEmoji;
            this.reasonWhy = reasonWhy;
            this.trigger = trigger;
            this.socialSituation = socialSituation;
            this.timestamp = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date());
        }

        // Getters and setters
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

        public void setMoodTitle(String moodTitle) {
            this.moodTitle = moodTitle;
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

        public int getIntensity() {
            return intensity;
        }

        public void setIntensity(int intensity) {
            this.intensity = intensity;
        }

        public String getReasonWhy() {
            return reasonWhy;
        }

        public void setReasonWhy(String reasonWhy) {
            this.reasonWhy = reasonWhy;
        }

        public String getMoodTitle() {
            return moodTitle;
        }

        public String getTrigger() {
            return trigger;
        }

        public String getSocialSituation() {
            return socialSituation;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public Photograph getPhotograph() {
            return photograph;
        }

        public String getPhotoUri() {
            if (photograph != null && photograph.getImageUri() != null) {
                return photograph.getImageUri().toString();
            }
            return "N/A";
        }
    }

    /**
     * Test implementation of the core profile page logic without Android/Firebase dependencies
     */
    private static class ProfilePageLogic {
        // Lists to store mood events
        private List<MoodEvent> allMoodEvents;
        private List<MoodEvent> filteredMoodEvents;

        // State tracking
        private boolean loadingVisible = false;
        private boolean emptyStateVisible = false;
        private boolean recyclerViewVisible = true;
        private String toastMessage = null;
        private boolean confirmationDialogShown = false;

        // User profile data
        private Map<String, Object> userProfile;

        // Mock Firebase operations results
        private boolean databaseOperationSuccess = true;

        public ProfilePageLogic() {
            allMoodEvents = new ArrayList<>();
            filteredMoodEvents = new ArrayList<>();
            userProfile = new HashMap<>();
        }

        /**
         * Adds a test mood event to the collection
         */
        public void addMoodEvent(MoodEvent moodEvent) {
            allMoodEvents.add(moodEvent);
            updateMoodEventsList();
        }

        /**
         * Updates the filtered list to match the full list (simulates initial load)
         */
        private void updateMoodEventsList() {
            filteredMoodEvents = new ArrayList<>(allMoodEvents);

            // Update UI visibility based on data
            if (filteredMoodEvents.isEmpty()) {
                recyclerViewVisible = false;
                emptyStateVisible = true;
            } else {
                recyclerViewVisible = true;
                emptyStateVisible = false;
            }
        }

        /**
         * Sets user profile data
         */
        public void setUserProfile(String fullName, String username, String bio, String profilePicUri) {
            userProfile.put("fullName", fullName);
            userProfile.put("username", username);
            userProfile.put("bio", bio);
            userProfile.put("profilePicUri", profilePicUri);
        }

        /**
         * Simulates loading user profile from database
         */
        public Map<String, Object> loadUserProfile() {
            return new HashMap<>(userProfile);
        }

        /**
         * Simulates Firebase operation success/failure
         */
        public void setDatabaseOperationSuccess(boolean success) {
            this.databaseOperationSuccess = success;
        }

        /**
         * Applies filters similar to the ProfilePage activity with option for custom reference date
         */
        public void applyFilters(String timeFilter, boolean isHappy, boolean isSad,
                                 boolean isAfraid, boolean isConfused, Date referenceDate) {
            loadingVisible = true;

            List<MoodEvent> newFilteredList = new ArrayList<>();
            long currentTime = referenceDate != null ? referenceDate.getTime() : System.currentTimeMillis();

            for (MoodEvent moodEvent : allMoodEvents) {
                long moodEventTime = moodEvent.getDate().getTime();
                boolean isWithinTime = false;

                switch (timeFilter) {
                    case "last_24_hours":
                        isWithinTime = (currentTime - moodEventTime) <= 86400000;
                        break;
                    case "3Days":
                        isWithinTime = (currentTime - moodEventTime) <= 259200000;
                        break;
                    case "last_week":
                        isWithinTime = (currentTime - moodEventTime) <= 604800000;
                        break;
                    case "last_month":
                        isWithinTime = (currentTime - moodEventTime) <= 2592000000L;
                        break;
                    case "all_time":
                        isWithinTime = true;
                        break;
                }

                if (isWithinTime) {
                    if ((isHappy && moodEvent.getMoodTitle().equals("HAPPY")) ||
                            (isSad && moodEvent.getMoodTitle().equals("SAD")) ||
                            (isAfraid && moodEvent.getMoodTitle().equals("AFRAID")) ||
                            (isConfused && moodEvent.getMoodTitle().equals("CONFUSED")) ||
                            (!isHappy && !isSad && !isAfraid && !isConfused)) {
                        newFilteredList.add(moodEvent);
                    }
                }
            }

            // Update the filtered list
            filteredMoodEvents = newFilteredList;

            // Update UI state
            loadingVisible = false;

            if (filteredMoodEvents.isEmpty()) {
                recyclerViewVisible = false;
                emptyStateVisible = true;
            } else {
                recyclerViewVisible = true;
                emptyStateVisible = false;
            }
        }

        /**
         * Convenience method for using the current system time
         */
        public void applyFilters(String timeFilter, boolean isHappy, boolean isSad,
                                 boolean isAfraid, boolean isConfused) {
            applyFilters(timeFilter, isHappy, isSad, isAfraid, isConfused, null);
        }

        /**
         * Simulates deleting a mood event
         */
        public boolean deleteMoodEvent(int position) {
            if (position < 0 || position >= allMoodEvents.size()) {
                toastMessage = "Invalid position";
                return false;
            }

            confirmationDialogShown = true;
            loadingVisible = true;

            if (databaseOperationSuccess) {
                // Simulate successful deletion
                MoodEvent removedEvent = allMoodEvents.remove(position);

                // Update filtered list if it contained the deleted item
                filteredMoodEvents.remove(removedEvent);

                toastMessage = "Mood deleted successfully";

                // Check if list is now empty
                if (allMoodEvents.isEmpty()) {
                    recyclerViewVisible = false;
                    emptyStateVisible = true;
                }

                loadingVisible = false;
                return true;
            } else {
                // Simulate failed deletion
                toastMessage = "Error deleting mood";
                loadingVisible = false;
                return false;
            }
        }

        /**
         * Simulates updating a mood event
         */
        public boolean updateMoodEvent(int position, String emoji, String mood, String trigger,
                                       String reasonWhy, String socialSituation, int intensity, String photoUri) {
            if (position < 0 || position >= allMoodEvents.size()) {
                toastMessage = "Invalid position";
                return false;
            }

            loadingVisible = true;

            if (databaseOperationSuccess) {
                // Get the mood to update
                MoodEvent moodToUpdate = allMoodEvents.get(position);

                // Update the mood
                moodToUpdate.setMoodEmoji(emoji);
                moodToUpdate.setMoodTitle(mood);
                moodToUpdate.setTrigger(trigger);
                moodToUpdate.setSocialSituation(socialSituation);
                moodToUpdate.setIntensity(intensity);
                moodToUpdate.setReasonWhy(reasonWhy);

                // Update photo
                if (photoUri != null && !photoUri.equals("N/A")) {
                    // Create a Photograph object for testing
                    Photograph photograph = new Photograph(
                            photoUri,  // Use the photoUri string directly
                            100L,      // Default size in KB
                            new Date(),
                            "Unknown"
                    );
                    moodToUpdate.setPhotograph(photograph);
                } else {
                    moodToUpdate.setPhotograph(null);
                }

                // Update subtitle
                StringBuilder subtitle = new StringBuilder();
                if (trigger != null && !trigger.isEmpty()) {
                    subtitle.append("Trigger: ").append(trigger);
                }

                if (socialSituation != null && !socialSituation.isEmpty()) {
                    if (subtitle.length() > 0) {
                        subtitle.append(" | ");
                    }
                    subtitle.append("Social: ").append(socialSituation);
                }

                moodToUpdate.setSubtitle(subtitle.toString());

                toastMessage = "Mood updated successfully";
                loadingVisible = false;
                return true;
            } else {
                // Simulate failed update
                toastMessage = "Error updating mood";
                loadingVisible = false;
                return false;
            }
        }

        // Getters for the test to verify the state
        public List<MoodEvent> getAllMoodEvents() {
            return allMoodEvents;
        }

        public List<MoodEvent> getFilteredMoodEvents() {
            return filteredMoodEvents;
        }

        public boolean isLoadingVisible() {
            return loadingVisible;
        }

        public boolean isEmptyStateVisible() {
            return emptyStateVisible;
        }

        public boolean isRecyclerViewVisible() {
            return recyclerViewVisible;
        }

        public String getToastMessage() {
            return toastMessage;
        }

        public boolean isConfirmationDialogShown() {
            return confirmationDialogShown;
        }
    }

    private ProfilePageLogic profilePageLogic;
    private SimpleDateFormat dateFormat;

    @Before
    public void setUp() {
        profilePageLogic = new ProfilePageLogic();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

        // Add some test user profile data
        profilePageLogic.setUserProfile("John Doe", "johndoe123", "Test bio", "https://example.com/profile.jpg");

        // Prepare some test mood events
        setupTestMoodEvents();
    }

    private void setupTestMoodEvents() {
        try {
            Calendar cal = Calendar.getInstance();

            // Fixed dates relative to our reference date (Jan 10, 2023)
            cal.setTime(REFERENCE_DATE);

            // Happy mood from yesterday (Jan 9, 2023)
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date yesterdayDate = cal.getTime();

            // Sad mood from 5 days ago (Jan 5, 2023)
            cal.setTime(REFERENCE_DATE);
            cal.add(Calendar.DAY_OF_MONTH, -5);
            Date fiveDaysAgoDate = cal.getTime();

            // Afraid mood from 20 days ago (Dec 21, 2022)
            cal.setTime(REFERENCE_DATE);
            cal.add(Calendar.DAY_OF_MONTH, -20);
            Date twentyDaysAgoDate = cal.getTime();

            // Create Happy Mood from yesterday
            MoodEvent happyMood = new MoodEvent();
            happyMood.setMoodEmoji("😊");
            happyMood.setMoodTitle("HAPPY");
            happyMood.setTrigger("Good news");
            happyMood.setSocialSituation("With friends");
            happyMood.setReasonWhy("Got a job offer");
            happyMood.setIntensity(8);
            happyMood.setDocumentId("happy_doc_id");

            // Set the fixed yesterday date
            happyMood.setDate(yesterdayDate);
            happyMood.setTimestamp(dateFormat.format(yesterdayDate));

            // Build subtitle
            StringBuilder subtitle1 = new StringBuilder();
            subtitle1.append("Trigger: Good news").append(" | ").append("Social: With friends");
            happyMood.setSubtitle(subtitle1.toString());

            // Create Sad Mood from 5 days ago
            MoodEvent sadMood = new MoodEvent();
            sadMood.setMoodEmoji("😢");
            sadMood.setMoodTitle("SAD");
            sadMood.setTrigger("Bad weather");
            sadMood.setSocialSituation("Alone");
            sadMood.setReasonWhy("Rainy day");
            sadMood.setIntensity(6);
            sadMood.setDocumentId("sad_doc_id");

            // Set the fixed 5 days ago date
            sadMood.setDate(fiveDaysAgoDate);
            sadMood.setTimestamp(dateFormat.format(fiveDaysAgoDate));

            // Build subtitle
            StringBuilder subtitle2 = new StringBuilder();
            subtitle2.append("Trigger: Bad weather").append(" | ").append("Social: Alone");
            sadMood.setSubtitle(subtitle2.toString());

            // Create Afraid Mood from 20 days ago
            MoodEvent afraidMood = new MoodEvent();
            afraidMood.setMoodEmoji("😨");
            afraidMood.setMoodTitle("AFRAID");
            afraidMood.setTrigger("Loud noise");
            afraidMood.setSocialSituation("With family");
            afraidMood.setReasonWhy("Thunder");
            afraidMood.setIntensity(7);

            // Add a photo
            Photograph photo = new Photograph(
                    "https://example.com/photo.jpg",  // URI as string
                    100L,                            // Size in KB
                    twentyDaysAgoDate,               // Date taken
                    "Unknown"                        // Location
            );
            afraidMood.setPhotograph(photo);
            afraidMood.setDocumentId("afraid_doc_id");

            // Set the fixed 20 days ago date
            afraidMood.setDate(twentyDaysAgoDate);
            afraidMood.setTimestamp(dateFormat.format(twentyDaysAgoDate));

            // Build subtitle
            StringBuilder subtitle3 = new StringBuilder();
            subtitle3.append("Trigger: Loud noise").append(" | ").append("Social: With family");
            afraidMood.setSubtitle(subtitle3.toString());

            // Add the test mood events
            profilePageLogic.addMoodEvent(happyMood);
            profilePageLogic.addMoodEvent(sadMood);
            profilePageLogic.addMoodEvent(afraidMood);

        } catch (Exception e) {
            // Handle any date parsing exceptions
            e.printStackTrace();
        }
    }

    /**
     * Test filter by time - last 24 hours
     */
    @Test
    public void testFilterByLastDay() {
        // Apply filter for last 24 hours, all emotions, using reference date
        profilePageLogic.applyFilters("last_24_hours", true, true, true, true, REFERENCE_DATE);

        // Should only include the happy mood from yesterday
        assertEquals(1, profilePageLogic.getFilteredMoodEvents().size());
        assertEquals("HAPPY", profilePageLogic.getFilteredMoodEvents().get(0).getMoodTitle());
    }

    /**
     * Test filter by time - last week
     */
    @Test
    public void testFilterByLastWeek() {
        // Apply filter for last week, all emotions, using reference date
        profilePageLogic.applyFilters("last_week", true, true, true, true, REFERENCE_DATE);

        // Should include happy and sad moods (not the afraid one which is older)
        assertEquals(2, profilePageLogic.getFilteredMoodEvents().size());
    }

    /**
     * Test filter by time - last month
     */
    @Test
    public void testFilterByLastMonth() {
        // Apply filter for last month, all emotions, using reference date
        profilePageLogic.applyFilters("last_month", true, true, true, true, REFERENCE_DATE);

        // Should include all three moods
        assertEquals(3, profilePageLogic.getFilteredMoodEvents().size());
    }

    /**
     * Test filter by emotion - happy only
     */
    @Test
    public void testFilterByHappyEmotion() {
        // Apply filter for all time, only happy emotion
        profilePageLogic.applyFilters("all_time", true, false, false, false, REFERENCE_DATE);

        // Should include only the happy mood
        assertEquals(1, profilePageLogic.getFilteredMoodEvents().size());
        assertEquals("HAPPY", profilePageLogic.getFilteredMoodEvents().get(0).getMoodTitle());
    }

    /**
     * Test filter by emotion - sad only
     */
    @Test
    public void testFilterBySadEmotion() {
        // Apply filter for all time, only sad emotion
        profilePageLogic.applyFilters("all_time", false, true, false, false, REFERENCE_DATE);

        // Should include only the sad mood
        assertEquals(1, profilePageLogic.getFilteredMoodEvents().size());
        assertEquals("SAD", profilePageLogic.getFilteredMoodEvents().get(0).getMoodTitle());
    }

    /**
     * Test filter by emotion - afraid only
     */
    @Test
    public void testFilterByAfraidEmotion() {
        // Apply filter for all time, only afraid emotion
        profilePageLogic.applyFilters("all_time", false, false, true, false, REFERENCE_DATE);

        // Should include only the afraid mood
        assertEquals(1, profilePageLogic.getFilteredMoodEvents().size());
        assertEquals("AFRAID", profilePageLogic.getFilteredMoodEvents().get(0).getMoodTitle());
    }

    /**
     * Test filter by multiple emotions
     */
    @Test
    public void testFilterByMultipleEmotions() {
        // Apply filter for all time, happy and sad emotions
        profilePageLogic.applyFilters("all_time", true, true, false, false, REFERENCE_DATE);

        // Should include only happy and sad moods
        assertEquals(2, profilePageLogic.getFilteredMoodEvents().size());
    }

    /**
     * Test filter by time and emotion combined
     */
    @Test
    public void testFilterByTimeAndEmotion() {
        // Apply filter for last week, only happy emotion
        profilePageLogic.applyFilters("last_week", true, false, false, false, REFERENCE_DATE);

        // Should include only the happy mood from yesterday
        assertEquals(1, profilePageLogic.getFilteredMoodEvents().size());
        assertEquals("HAPPY", profilePageLogic.getFilteredMoodEvents().get(0).getMoodTitle());
    }

    /**
     * Test UI visibility when no results after filtering
     */
    @Test
    public void testEmptyResultsAfterFiltering() {
        // Apply filter for last 24 hours, only confused emotion (none in our test data)
        profilePageLogic.applyFilters("last_24_hours", false, false, false, true, REFERENCE_DATE);

        // Should have no results
        assertEquals(0, profilePageLogic.getFilteredMoodEvents().size());

        // UI state should be updated for empty state
        assertTrue("Empty state should be visible", profilePageLogic.isEmptyStateVisible());
        assertFalse("Recycler view should be hidden", profilePageLogic.isRecyclerViewVisible());
    }

    /**
     * Test delete mood event
     */
    @Test
    public void testDeleteMoodEvent() {
        // Get the initial count
        int initialCount = profilePageLogic.getAllMoodEvents().size();

        // Delete the first mood
        boolean result = profilePageLogic.deleteMoodEvent(0);

        // Verify
        assertTrue("Delete operation should succeed", result);
        assertEquals("Toast message should indicate success", "Mood deleted successfully", profilePageLogic.getToastMessage());
        assertEquals("Mood list should have one less item", initialCount - 1, profilePageLogic.getAllMoodEvents().size());
        assertTrue("Confirmation dialog should be shown", profilePageLogic.isConfirmationDialogShown());
    }

    /**
     * Test delete mood event - failed operation
     */
    @Test
    public void testDeleteMoodEventFailure() {
        // Simulate database operation failure
        profilePageLogic.setDatabaseOperationSuccess(false);

        // Get the initial count
        int initialCount = profilePageLogic.getAllMoodEvents().size();

        // Try to delete the first mood
        boolean result = profilePageLogic.deleteMoodEvent(0);

        // Verify
        assertFalse("Delete operation should fail", result);
        assertEquals("Toast message should indicate error", "Error deleting mood", profilePageLogic.getToastMessage());
        assertEquals("Mood list should remain unchanged", initialCount, profilePageLogic.getAllMoodEvents().size());
    }

    /**
     * Test update mood event
     */
    @Test
    public void testUpdateMoodEvent() {
        // Update the first mood
        boolean result = profilePageLogic.updateMoodEvent(0, "😃", "HAPPY", "Updated trigger",
                "Updated reason", "Updated social", 9, null);

        // Verify
        assertTrue("Update operation should succeed", result);
        assertEquals("Toast message should indicate success", "Mood updated successfully", profilePageLogic.getToastMessage());

        // Check updated values
        MoodEvent updatedMood = profilePageLogic.getAllMoodEvents().get(0);
        assertEquals("Emoji should be updated", "😃", updatedMood.getMoodEmoji());
        assertEquals("Mood title should be updated", "HAPPY", updatedMood.getMoodTitle());
        assertEquals("Trigger should be updated", "Updated trigger", updatedMood.getTrigger());
        assertEquals("Reason should be updated", "Updated reason", updatedMood.getReasonWhy());
        assertEquals("Social situation should be updated", "Updated social", updatedMood.getSocialSituation());
        assertEquals("Intensity should be updated", 9, updatedMood.getIntensity());
    }

    /**
     * Test update mood event - with photo
     */
    @Test
    public void testUpdateMoodEventWithPhoto() {
        // Update the first mood with a photo
        String photoUri = "https://example.com/new_photo.jpg";
        boolean result = profilePageLogic.updateMoodEvent(0, "😃", "HAPPY", "Updated trigger",
                "Updated reason", "Updated social", 9, photoUri);

        // Verify
        assertTrue("Update operation should succeed", result);

        // Check photo-related fields
        MoodEvent updatedMood = profilePageLogic.getAllMoodEvents().get(0);
        assertTrue("Has photo should be true", updatedMood.getPhotograph() != null);
        assertEquals("Photo URI should be updated", photoUri, updatedMood.getPhotoUri());
    }

    /**
     * Test user profile loading
     */
    @Test
    public void testLoadUserProfile() {
        // Load the user profile
        Map<String, Object> profile = profilePageLogic.loadUserProfile();

        // Verify the loaded data
        assertEquals("Full name should match", "John Doe", profile.get("fullName"));
        assertEquals("Username should match", "johndoe123", profile.get("username"));
        assertEquals("Bio should match", "Test bio", profile.get("bio"));
        assertEquals("Profile pic URI should match", "https://example.com/profile.jpg", profile.get("profilePicUri"));
    }
}