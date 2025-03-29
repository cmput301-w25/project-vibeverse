package com.example.vibeverse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Path;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class AchievementActivity extends AppCompatActivity {

    private static final String TAG = "AchievementsActivity";

    private TextView headerTextView;
    private TextView levelTextView;
    private ProgressBar xpProgressBar;
    private TextView xpLeftTextView;
    private RecyclerView achievementsRecyclerView;

    // Hold the levels data from levels.json
    private List<Level> levelsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        // Bind views from layout
        headerTextView = findViewById(R.id.headerTextView);
        levelTextView = findViewById(R.id.levelTextView);
        xpProgressBar = findViewById(R.id.xpProgressBar);
        xpLeftTextView = findViewById(R.id.xpLeftTextView);
        achievementsRecyclerView = findViewById(R.id.achievementsRecyclerView);

        // Set header text
        headerTextView.setText("Achievements");

        // Load achievements from assets
        List<Achievement> achievementList = loadAchievementsFromAssets();

        // Set up RecyclerView with the adapter using the loaded achievements
        achievementsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        achievementsRecyclerView.setAdapter(new AchievementAdapter(this, achievementList));

        // First, load levels from assets synchronously
        levelsList = loadLevelsFromAssets();
        if (levelsList == null || levelsList.isEmpty()) {
            Log.e(TAG, "Levels data is empty or could not be loaded.");
            return;
        }

        // Now, retrieve current user's data from Firestore
        loadCurrentUserData();
    }

    /**
     * Loads levels.json from assets and returns a list of Level objects.
     */
    private List<Level> loadLevelsFromAssets() {
        try {
            InputStream inputStream = getAssets().open("levels.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            // Our JSON has an object with a "levels" array
            LevelsWrapper wrapper = new Gson().fromJson(reader, LevelsWrapper.class);
            reader.close();
            return wrapper != null ? wrapper.getLevels() : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error loading levels from assets", e);
            return new ArrayList<>();
        }
    }

    /**
     * Loads achievements.json from assets and returns a list of Achievement objects.
     */
    private List<Achievement> loadAchievementsFromAssets() {
        try {
            InputStream inputStream = getAssets().open("achievements.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            // Our JSON has an object with an "achievements" array.
            AchievementsWrapper wrapper = new Gson().fromJson(reader, AchievementsWrapper.class);
            reader.close();
            return wrapper != null ? wrapper.getAchievements() : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error loading achievements from assets", e);
            return new ArrayList<>();
        }
    }

    /**
     * Retrieves the current user's XP and level from Firestore.
     */
    private void loadCurrentUserData() {
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "User not logged in.");
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            // Assuming your user document contains fields "xp" (number) and "level" (number)
                            Long xpLong = documentSnapshot.getLong("totalXP");
                            Long levelLong = documentSnapshot.getLong("level");
                            if (xpLong == null || levelLong == null) {
                                Log.e(TAG, "User document is missing xp or level fields.");
                                return;
                            }
                            int currentXP = xpLong.intValue();
                            int currentLevel = levelLong.intValue();
                            updateUI(currentXP, currentLevel);
                        } else {
                            Log.e(TAG, "User document does not exist.");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Error retrieving user data", e);
                    }
                });
    }

    /**
     * Updates the UI based on the current user's XP and level.
     */
    private void updateUI(int currentXP, int currentLevel) {
        // Find xp thresholds from the levelsList
        int xpForCurrentLevel = getXPForLevel(currentLevel);
        int xpForNextLevel = getXPForLevel(currentLevel + 1);

        // If next level threshold is not found, assume user is max level
        if (xpForNextLevel == 0) {
            xpForNextLevel = xpForCurrentLevel;
        }

        // Calculate progress within current level
        int xpDiffForLevel = xpForNextLevel - xpForCurrentLevel;
        int xpIntoLevel = currentXP - xpForCurrentLevel;
        int xpLeft = xpForNextLevel - currentXP;

        // Update level view (assumes styling from XML for a ribbon/circular look)
        levelTextView.setText("Level " + currentLevel);

        // Update the progress bar
        xpProgressBar.setMax(xpDiffForLevel);
        xpProgressBar.setProgress(xpIntoLevel);

        // Update XP left text
        xpLeftTextView.setText(xpLeft + " XP left to level up");
    }

    /**
     * Looks up the xpRequired for a given level in the levelsList.
     */
    private int getXPForLevel(int level) {
        for (Level lvl : levelsList) {
            if (lvl.getLevel() == level) {
                return lvl.getXpRequired();
            }
        }
        return 0;
    }

    public void animateXpGain(View startView, int xpAmount) {
        final int sparkleCount = 5; // number of sparkles to create
        final int delayBetweenSparkles = 150; // milliseconds delay between each sparkle
        final ViewGroup rootView = (ViewGroup) getWindow().getDecorView();

        // Get the sparkle size from dimens.xml
        final int sparkleSize = (int) getResources().getDimension(R.dimen.sparkle_size);

        // Get start (claim button) center coordinates
        int[] startLoc = new int[2];
        startView.getLocationOnScreen(startLoc);
        final int startX = startLoc[0] + startView.getWidth() / 2 - sparkleSize / 2;
        final int startY = startLoc[1] + startView.getHeight() / 2 - sparkleSize / 2;

        // Get destination (XP bar) center coordinates
        int[] destLoc = new int[2];
        xpProgressBar.getLocationOnScreen(destLoc);
        final int destX = destLoc[0] + xpProgressBar.getWidth() / 2 - sparkleSize / 2;
        final int destY = destLoc[1] + xpProgressBar.getHeight() / 2 - sparkleSize / 2;

        // Create multiple sparkles with staggered delays
        for (int i = 0; i < sparkleCount; i++) {
            final int index = i;
            xpProgressBar.postDelayed(() -> {
                // Create an ImageView for the sparkle
                final ImageView sparkle = new ImageView(this);
                sparkle.setImageResource(R.drawable.ic_sparkle); // your sparkle vector drawable
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(sparkleSize, sparkleSize);
                sparkle.setLayoutParams(params);

                // Set initial position for the sparkle
                sparkle.setX(startX);
                sparkle.setY(startY);

                // Add sparkle view to the activity's overlay
                rootView.addView(sparkle);

                // Create a Path with a subtle curve. Use variation to spread sparkles slightly.
                Path path = new Path();
                path.moveTo(startX, startY);
                float variation = (index - sparkleCount / 2f) * 20; // variations: e.g., -40, -20, 0, 20, 40
                float controlX = startX + (destX - startX) / 2 + variation;
                float controlY = startY - 150 - (index * 10); // slightly different arc heights
                path.quadTo(controlX, controlY, destX, destY);

                // Animate the sparkle along the path
                ObjectAnimator animator = ObjectAnimator.ofFloat(sparkle, View.X, View.Y, path);
                animator.setDuration(800);
                animator.setInterpolator(new AccelerateInterpolator());
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Remove sparkle after animation
                        rootView.removeView(sparkle);
                        // Each sparkle triggers a flash effect on the XP bar
                        flashXpBar();
                    }
                });
                animator.start();
            }, i * delayBetweenSparkles);
        }

        // Update the XP bar's progress using a ValueAnimator
        int currentProgress = xpProgressBar.getProgress();
        int newProgress = currentProgress + xpAmount; // adjust based on your leveling logic
        ValueAnimator progressAnimator = ValueAnimator.ofInt(currentProgress, newProgress);
        progressAnimator.setDuration(1000);
        progressAnimator.addUpdateListener(animation -> {
            int value = (int) animation.getAnimatedValue();
            xpProgressBar.setProgress(value);

            int xpLeft = Math.max(xpProgressBar.getMax() - value, 0);
            xpLeftTextView.setText(xpLeft + " XP left to level up");

        });

        progressAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Check if the new progress overflowed the current level threshold
                if (newProgress >= xpProgressBar.getMax()) {
                    // Calculate overflow XP
                    int overflowXP = newProgress - xpProgressBar.getMax();

                    // Extract the current level. For example, you could parse it from levelTextView:
                    // Assuming levelTextView's text is in the format "Level X"
                    String levelText = levelTextView.getText().toString();
                    int currentLevel = Integer.parseInt(levelText.replaceAll("[^0-9]", ""));
                    int newLevel = currentLevel + 1;

                    // Trigger the level upgrade animation
                    animateLevelUpgrade(currentLevel, newLevel, overflowXP);
                }
            }
        });
        progressAnimator.setInterpolator(new DecelerateInterpolator());
        progressAnimator.start();
    }

    // Helper method to flash the XP bar
    private void flashXpBar() {
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(xpProgressBar, "scaleX", 1f, 1.2f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(xpProgressBar, "scaleY", 1f, 1.2f);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(xpProgressBar, "scaleX", 1.2f, 1f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(xpProgressBar, "scaleY", 1.2f, 1f);
        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        scaleUpX.start();
        scaleUpY.start();
        scaleUpX.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                scaleDownX.start();
                scaleDownY.start();
            }
        });
    }

    private void animateLevelUpgrade(final int oldLevel, final int newLevel, final int leftoverXP) {
        // Get the existing level container and its TextView
        final View levelCard = findViewById(R.id.levelCard);
        final TextView levelText = findViewById(R.id.levelTextView);

        // Animate the level card sliding out to the left
        ObjectAnimator slideOut = ObjectAnimator.ofFloat(levelCard, "translationX", 0, -levelCard.getWidth());
        slideOut.setDuration(600);
        slideOut.setInterpolator(new AccelerateInterpolator());
        slideOut.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Update the level text to the new level while off-screen
                levelText.setText("Level " + newLevel);

                // Update XP thresholds for the new level
                int xpForCurrentLevel = getXPForLevel(newLevel);
                int xpForNextLevel = getXPForLevel(newLevel + 1);
                if (xpForNextLevel == 0) { // handle max level
                    xpForNextLevel = xpForCurrentLevel;
                }
                int newLevelRange = xpForNextLevel - xpForCurrentLevel;
                xpProgressBar.setMax(newLevelRange);
                xpProgressBar.setProgress(leftoverXP);
                int xpLeft = Math.max(newLevelRange - leftoverXP, 0);
                xpLeftTextView.setText(xpLeft + " XP left to level up");

                // Prepare the card off-screen to the right
                levelCard.setTranslationX(levelCard.getWidth());
                // Animate the level card sliding in from the right
                ObjectAnimator slideIn = ObjectAnimator.ofFloat(levelCard, "translationX", levelCard.getWidth(), 0);
                slideIn.setDuration(600);
                slideIn.setInterpolator(new DecelerateInterpolator());
                slideIn.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Update the user's level in Firestore to make the change persistent
                        String userId = FirebaseAuth.getInstance().getUid();
                        if (userId != null) {
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(userId)
                                    .update("level", newLevel)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "User level updated successfully in Firestore");
                                            // Check and update themes if the new level unlocks one
                                            unlockThemeIfAvailable(newLevel);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.e(TAG, "Error updating user level in Firestore", e);
                                        }
                                    });
                        }
                    }
                });
                slideIn.start();
            }
        });
        slideOut.start();
    }

    /**
     * Checks if the new level unlocks a theme. If so, removes it from lockedThemes and adds it to unlockedThemes.
     */
    private void unlockThemeIfAvailable(int level) {
        String themeToUnlock = levelsList.stream().filter(lvl -> lvl.getLevel() == level).findFirst().map(Level::getUnlocks).orElse("N/A");
        // Find the level from the levelsList
        // If the unlock field is not "N/A", perform the theme transfer
        if (themeToUnlock != null && !themeToUnlock.equals("N/A")) {
            String userId = FirebaseAuth.getInstance().getUid();
            if (userId == null) {
                Log.e(TAG, "User not logged in. Cannot unlock theme.");
                return;
            }
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Remove the theme from lockedThemes
            db.collection("users").document(userId).collection("lockedThemes").document("list")
                    .update("themeNames", FieldValue.arrayRemove(themeToUnlock))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Theme removed from lockedThemes: " + themeToUnlock);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error removing theme from lockedThemes", e);
                    });
            // Add the theme to unlockedThemes
            db.collection("users").document(userId).collection("unlockedThemes").document("list")
                    .update("themeNames", FieldValue.arrayUnion(themeToUnlock))
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Theme added to unlockedThemes: " + themeToUnlock);
                        Intent intent = new Intent(AchievementActivity.this, VibeStoreActivity.class);
                        intent.putExtra("newlyUnlockedTheme", themeToUnlock); // themeToUnlock is the ID you unlocked
                        startActivity(intent);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error adding theme to unlockedThemes", e);
                    });
        }
    }



}
