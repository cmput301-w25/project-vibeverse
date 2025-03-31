package com.example.vibeverse;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AchievementChecker class provides methods to check and update achievements for a user.
 */
public class AchievementChecker {

    private FirebaseFirestore db;
    private String userId;

    /**
     * Constructs an AchievementChecker with the given userId.
     *
     * @param userId the unique identifier of the user.
     */
    public AchievementChecker(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    /**
     * Utility method to get a reference to an achievement document.
     */
    private DocumentReference getAchievementDoc(String achievementId) {
        return db.collection("users")
                .document(userId)
                .collection("achievements")
                .document(achievementId);
    }


    /**
     * Generic helper method to update an achievement document.
     * Increments progress and marks as "unclaimed" if threshold is met.
     *
     * @param achievementId The id of the achievement (e.g., "ach1")
     * @param requiredCount The total count needed to unlock the achievement.
     */
    private void updateAchievement(String achievementId, int requiredCount) {
        final DocumentReference doc = getAchievementDoc(achievementId);
        doc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String status = documentSnapshot.getString("completion_status");
                if ("incomplete".equals(status)) {
                    Long progress = documentSnapshot.getLong("progress");
                    progress = (progress == null ? 0 : progress) + 1;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("progress", progress);
                    if (progress >= requiredCount) {
                        updates.put("completion_status", "unclaimed");
                    }
                    doc.update(updates);
                }
            }
        });
    }


    /**
     * Unified checker for mood-event achievements:
     * - ach1 ("First Mood") requires 1 mood event.
     * - ach2 ("Mood Starter") requires 3 mood events.
     * - ach3 ("Mood Enthusiast") requires 10 mood events.
     * - ach4 ("Mood Machine") requires 50 mood events.
     *
     * This method should be called each time a mood event is posted.
     *
     * @param mood The mood event that was just created.
     */
    public void checkMoodEventAchievements(MoodEvent mood) {
        updateAchievement("ach1", 1);
        updateAchievement("ach2", 3);
        updateAchievement("ach3", 10);
        updateAchievement("ach4", 50);
    }


    /**
     * Checker for "Early Bird" (ach5).
     * Requirement: Post a mood event before 6AM.
     */
    public void checkAch5(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            // Condition met: mood posted before 6AM.
            updateAchievement("ach5", 1);
        }
    }

    /**
     * Checker for "Night Owl" (ach6).
     * Requirement: Post a mood event after midnight.
     */
    public void checkAch6(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        // Assuming "after midnight" means in the first hour after midnight.
        if (hour == 0) {
            updateAchievement("ach6", 1);
        }
    }

    /**
     * Checker for "Social Butterfly" (ach7).
     * Requirement: Share your mood publicly.
     */
    public void checkAch7(MoodEvent mood) {
        if (mood.isPublic()) {
            updateAchievement("ach7", 1);
        }
    }

    /**
     * Checker for "Secret Journal" (ach8).
     * Requirement: Post a private mood event.
     */
    public void checkAch8(MoodEvent mood) {
        if (!mood.isPublic()) {
            updateAchievement("ach8", 1);
        }
    }



    /**
     * Checker for "Emoji Explorer I" (ach9)
     * Requirements: Use 4 distinct emojis in mood events.
     * Assumes the Mood model has a method getEmoji() that returns a String.
     */
    public void checkAch9(MoodEvent mood) {
        final DocumentReference doc = getAchievementDoc("ach9");
        doc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Retrieve the stored array of unique emojis.
                List<String> uniqueEmojis = (List<String>) documentSnapshot.get("unique_entities");
                if (uniqueEmojis == null) {
                    uniqueEmojis = new ArrayList<>();
                }
                String  moodTitle = mood.getMoodTitle();
                if ( moodTitle != null && !uniqueEmojis.contains( moodTitle)) {
                    uniqueEmojis.add( moodTitle);
                }
                int progress = uniqueEmojis.size();
                Map<String, Object> updates = new HashMap<>();
                updates.put("unique_entities", uniqueEmojis);
                updates.put("progress", progress);
                if (progress >= 4) {
                    updates.put("completion_status", "unclaimed");
                }
                doc.update(updates);
            }
        });
    }

    /**
     * Checker for "Emoji Explorer II" (ach10).
     * Requirement: Use all available emojis (assumed total: 8 distinct emojis) at least once.
     * Assumes the mood has a getEmoji() method.
     */
    public void checkAch10(MoodEvent mood) {
        final DocumentReference doc = getAchievementDoc("ach10");
        doc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Retrieve stored unique emojis from the achievement document.
                List<String> uniqueEmojis = (List<String>) documentSnapshot.get("unique_entities");
                if (uniqueEmojis == null) {
                    uniqueEmojis = new ArrayList<>();
                }
                String  moodTitle = mood.getMoodTitle();
                if (moodTitle != null && !uniqueEmojis.contains( moodTitle)) {
                    uniqueEmojis.add( moodTitle);
                }
                int progress = uniqueEmojis.size();
                Map<String, Object> updates = new HashMap<>();
                updates.put("unique_entities", uniqueEmojis);
                updates.put("progress", progress);
                if (progress >= 8) {
                    updates.put("completion_status", "unclaimed");
                }
                doc.update(updates);
            }
        });
    }

    /**
     * Checker for "It's a brand new world" (ach11).
     * Requirement: Edit your profile.
     * This method should be called when a user successfully edits their profile.
     *
     */
    public void checkAch11() {
        // For a profile edit achievement, we assume a single edit qualifies.
        updateAchievement("ach11", 1);
    }

    /**
     * Checker for "I don't like you anymore" (ach12).
     * Requirement: Unfollow someone.
     * This method should be called when a user unfollows another participant.
     */
    public void checkAch12() {
        updateAchievement("ach12", 1);
    }


    /**
     * Checker for "On the radar" (ach13).
     * Requirement: Attach your location to a mood event.
     * This method should be called when a mood event includes location data.
     */
    public void checkAch13() {
        updateAchievement("ach13", 1);
    }

    /**
     * Checker for "Commentator" (ach14)
     * Requirements: Comment on a mood event.
     */
    public void checkAch14() {
        final DocumentReference doc = getAchievementDoc("ach14");
        doc.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String status = documentSnapshot.getString("completion_status");
                if ("incomplete".equals(status)) {
                    Long progress = documentSnapshot.getLong("progress");
                    progress = (progress == null ? 0 : progress) + 1;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("progress", progress);
                    // Since this achievement requires just 1 comment, mark as unclaimed.
                    if (progress >= 1) {
                        updates.put("completion_status", "unclaimed");
                    }
                    doc.update(updates);
                }
            }
        });
    }

    /**
     * Checker for "Making Connections I" (ach15).
     * Requirement: Follow another participant.
     * This method should be called when a user follows someone.
     */
    public void checkAch15() {
        updateAchievement("ach15", 1);
    }

    /**
     * Checker for "Making Connections II" (ach16).
     * Requirement: Follow 5 other participants.
     * This method should be called each time a follow occurs.
     */
    public void checkAch16() {
        updateAchievement("ach16", 5);
    }

    /**
     * Checker for "Making Connections III" (ach17).
     * Requirement: Follow 20 other participants.
     * This method should be called each time a follow occurs.
     */
    public void checkAch17() {
        updateAchievement("ach17", 20);
    }

    /**
     * Checker for "Target on your back I" (ach18).
     * Requirement: Grow your influence – get 5 followers.
     * This method should be called when your follower count increases.
     */
    public void checkAch18() {
        updateAchievement("ach18", 5);
    }

    /**
     * Checker for "Target on your back II" (ach19).
     * Requirement: Make an impact – have 20 followers.
     * This method should be called when your follower count increases.
     */
    public void checkAch19() {
        updateAchievement("ach19", 20);
    }

    /**
     * Checker for "It's getting spooky" (ach20)
     * Requirements: Post a mood event on October 31.
     * Assumes the Mood model has a method getDate() returning a Date object.
     */
    public void checkAch20(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        // Calendar.MONTH is 0-based (January = 0), so October is 9.
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month == 9 && day == 31) {
            final DocumentReference doc = getAchievementDoc("ach20");
            doc.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String status = documentSnapshot.getString("completion_status");
                    if ("incomplete".equals(status)) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("progress", 1);
                        updates.put("completion_status", "unclaimed");
                        doc.update(updates);
                    }
                }
            });
        }
    }

    /**
     * Checker for "Love is in the Air" (ach21).
     * Requirement: Post a mood event on 14th February.
     * Each mood event on Feb 14 increments the achievement progress.
     */
    public void checkAch21(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        // Calendar.MONTH is zero-based: January=0, February=1
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month == 1 && day == 14) {
            updateAchievement("ach21", 30);
        }
    }

    /**
     * Checker for "Photo Mood" (ach22).
     * Requirement: Enhance your mood with a photo.
     * This method should be called when a mood event includes a photo.
     */
    public void checkAch22(MoodEvent mood) {
        if (mood.getPhotograph() != null) {
            updateAchievement("ach22", 1);
        }
    }

    /**
     * Checker for "Mood Marathon" (ach23).
     * Requirement: Keep the streak alive – post mood events every day for a week (7 consecutive days).
     *
     * @param currentStreak The current consecutive days streak.
     */
    public void checkAch23(int currentStreak) {
        final DocumentReference doc = getAchievementDoc("ach23");
        doc.get().addOnSuccessListener(documentSnapshot -> {
            Map<String, Object> updates = new HashMap<>();
            updates.put("progress", currentStreak);
            // Mark as unclaimed if streak reaches 7 or more
            if (currentStreak >= 7) {
                updates.put("completion_status", "unclaimed");
            } else {
                updates.put("completion_status", "incomplete");
            }
            doc.update(updates);
        });
    }

    /**
     * Checker for "Ho Ho Ho!" (ach24).
     * Requirement: Post a mood event on 24th December.
     */
    public void checkAch24(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        // December is 11 (0-based)
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month == 11 && day == 24) {
            updateAchievement("ach24", 1);
        }
    }

    /**
     * Checker for "New Year, New Me" (ach25).
     * Requirement: Post a mood event on 1st January.
     */
    public void checkAch25(MoodEvent mood) {
        Date moodDate = mood.getDate();
        Calendar cal = Calendar.getInstance();
        cal.setTime(moodDate);
        // January is 0 (0-based)
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (month == 0 && day == 1) {
            updateAchievement("ach25", 1);
        }
    }
}
