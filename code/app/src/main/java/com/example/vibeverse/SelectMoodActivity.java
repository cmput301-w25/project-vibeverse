package com.example.vibeverse;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SelectMoodActivity extends AppCompatActivity {

    private TextView selectedMoodEmoji, selectedMoodText;
    private EditText triggerInput, socialSituationInput;
    private SeekBar moodIntensitySlider;
    private Button continueButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer; // Main screen background

    private String selectedMood = "Angry"; // Default mood
    private String selectedEmoji = "😡";
    private int selectedColor = Color.RED;

    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mood);

        mainContainer = findViewById(R.id.mainContainer); // Get the main layout
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        triggerInput = findViewById(R.id.triggerInput);
        socialSituationInput = findViewById(R.id.socialSituationInput);
        continueButton = findViewById(R.id.continueButton);

        // Define Mood Colors & Emojis
        moodColors.put("Angry", Color.RED);
        moodColors.put("Confused", Color.parseColor("#6A5ACD")); // Slate Blue
        moodColors.put("Disgusted", Color.parseColor("#228B22")); // Forest Green
        moodColors.put("Afraid", Color.parseColor("#1E3A5F")); // Dark Blue
        moodColors.put("Happy", Color.parseColor("#FFD700")); // Gold
        moodColors.put("Sad", Color.parseColor("#4682B4")); // Steel Blue
        moodColors.put("Shameful", Color.parseColor("#C71585")); // Medium Violet Red
        moodColors.put("Surprised", Color.parseColor("#FFA500")); // Orange

        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Shameful", "😳");
        moodEmojis.put("Surprised", "😲");

        // Populate Mood Selection Grid
        GridLayout moodGrid = findViewById(R.id.moodGrid);
        for (String mood : moodEmojis.keySet()) {
            MaterialButton moodButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            moodButton.setText(moodEmojis.get(mood));
            moodButton.setCornerRadius(20);
            moodButton.setTextSize(32);
            moodButton.setPadding(16, 16, 16, 16);
            moodButton.setBackgroundColor(moodColors.get(mood));
            moodButton.setOnClickListener(view -> selectMood(mood));
            moodGrid.addView(moodButton);
        }

        // Continue Button Click - Send Mood to MainActivity
        continueButton.setOnClickListener(view -> {
            Intent intent = new Intent(SelectMoodActivity.this, MainActivity.class);
            intent.putExtra("selectedMood", selectedMood);
            intent.putExtra("selectedEmoji", selectedEmoji);
            intent.putExtra("trigger", triggerInput.getText().toString().trim());
            intent.putExtra("socialSituation", socialSituationInput.getText().toString().trim());

            // Add Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            intent.putExtra("timestamp", currentTime);

            startActivity(intent);
        });
    }

    private void selectMood(String mood) {
        selectedMood = mood;
        selectedEmoji = moodEmojis.get(mood);
        selectedColor = moodColors.get(mood);

        selectedMoodText.setText(selectedMood);
        selectedMoodEmoji.setText(selectedEmoji);
        selectedMoodContainer.setBackgroundColor(selectedColor);

        // Apply a gradient background
        applyGradientBackground(selectedColor);
    }

    private void applyGradientBackground(int baseColor) {
        int lighterColor = adjustColorBrightness(baseColor, 1.5f); // Lighter shade
        int darkerColor = adjustColorBrightness(baseColor, 0.8f); // Darker shade

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{lighterColor, baseColor, darkerColor}
        );
        gradientDrawable.setCornerRadius(0f);

        mainContainer.setBackground(gradientDrawable);
    }

    // Method to adjust color brightness
    private int adjustColorBrightness(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.rgb(r, g, b);
    }
}
