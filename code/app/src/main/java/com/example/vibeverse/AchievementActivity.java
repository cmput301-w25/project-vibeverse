package com.example.vibeverse;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
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
        // 1. Create the sparkle view (an ImageView) with your sparkle drawable.
        final ImageView sparkleView = new ImageView(this);
        sparkleView.setImageResource(R.drawable.ic_sparkle); // your sparkle image resource
        // Optionally, set layout params (for example, 48x48dp):
        int size = (int) getResources().getDimension(R.dimen.sparkle_size); // define in dimens.xml, e.g., 48dp
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(size, size);
        sparkleView.setLayoutParams(params);

        // 2. Find the root view to which we can add the overlay (using the activity's decor view)
        final ViewGroup rootView = (ViewGroup) getWindow().getDecorView();

        // 3. Calculate the starting location (center of startView) and destination (center of xpProgressBar).
        int[] startLoc = new int[2];
        startView.getLocationOnScreen(startLoc);
        int startX = startLoc[0] + startView.getWidth() / 2 - size / 2;
        int startY = startLoc[1] + startView.getHeight() / 2 - size / 2;

        int[] destLoc = new int[2];
        xpProgressBar.getLocationOnScreen(destLoc);
        int destX = destLoc[0] + xpProgressBar.getWidth() / 2 - size / 2;
        int destY = destLoc[1] + xpProgressBar.getHeight() / 2 - size / 2;

        // 4. Set the initial position of the sparkle view.
        sparkleView.setX(startX);
        sparkleView.setY(startY);

        // 5. Add the sparkle view to the overlay of the root view.
        rootView.addView(sparkleView);

        // 6. Create a Path from start to destination. Optionally, add a slight curve.
        Path path = new Path();
        path.moveTo(startX, startY);
        // For a subtle arc, use a control point; adjust these values to your liking.
        float controlX = startX + (destX - startX) / 2;
        float controlY = startY - 200; // 200 pixels above start for a nice arc
        path.quadTo(controlX, controlY, destX, destY);

        // 7. Animate the sparkle along the path.
        ObjectAnimator pathAnimator = ObjectAnimator.ofFloat(sparkleView, View.X, View.Y, path);
        pathAnimator.setDuration(1000); // duration 1 second
        pathAnimator.setInterpolator(new AccelerateInterpolator());

        // 8. Optionally, add a ValueAnimator to update the XP bar progress gradually.
        // Assume you have the current progress and target progress (for example, obtained from updateUI).
        int currentProgress = xpProgressBar.getProgress();
        // Compute new progress based on xpAmount and level thresholds.
        // (You may want to compute the exact new progress from your business logic.)
        int newProgress = currentProgress + xpAmount; // Simplified; adjust as needed.
        ValueAnimator progressAnimator = ValueAnimator.ofInt(currentProgress, newProgress);
        progressAnimator.setDuration(1000);
        progressAnimator.addUpdateListener(animation -> {
            int animatedValue = (int) animation.getAnimatedValue();
            xpProgressBar.setProgress(animatedValue);
        });
        progressAnimator.setInterpolator(new DecelerateInterpolator());

        // 9. When the sparkle animation ends, remove the sparkle and flash the XP bar.
        pathAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                // Remove sparkle view from overlay
                rootView.removeView(sparkleView);
                // Flash the XP bar (simple scale animation)
                ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(xpProgressBar, "scaleX", 1f, 1.2f);
                ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(xpProgressBar, "scaleY", 1f, 1.2f);
                scaleUpX.setDuration(150);
                scaleUpY.setDuration(150);
                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(xpProgressBar, "scaleX", 1.2f, 1f);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(xpProgressBar, "scaleY", 1.2f, 1f);
                scaleDownX.setDuration(150);
                scaleDownY.setDuration(150);

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
        });

        // 10. Start both animations together.
        pathAnimator.start();
        progressAnimator.start();
    }

}
