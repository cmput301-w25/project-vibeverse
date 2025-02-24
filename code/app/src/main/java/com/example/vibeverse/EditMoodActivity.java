package com.example.vibeverse;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditMoodActivity extends AppCompatActivity {

    private TextView selectedMoodEmoji, selectedMoodText;
    private EditText triggerInput, socialSituationInput;
    private SeekBar moodIntensitySlider;
    private Button updateButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer; // Main screen background

    private String selectedMood;
    private String selectedEmoji;
    private int selectedColor;

    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    private int moodPosition; // Position of the mood in the list

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mood); // Use the same layout

        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        triggerInput = findViewById(R.id.triggerInput);
        socialSituationInput = findViewById(R.id.socialSituationInput);
        updateButton = findViewById(R.id.continueButton); // Use the same button
        updateButton.setText("Update Mood"); // Change text to "Update Mood"

        // Load Mood Colors
        initializeMoodColors();
        initializeMoodEmojis();

        // Get data from MainActivity
        Intent intent = getIntent();
        selectedMood = intent.getStringExtra("selectedMood");
        selectedEmoji = intent.getStringExtra("selectedEmoji");
        selectedColor = moodColors.getOrDefault(selectedMood, Color.GRAY);
        moodPosition = intent.getIntExtra("moodPosition", -1);

        String timestamp = intent.getStringExtra("timestamp");
        String trigger = intent.getStringExtra("trigger");
        String socialSituation = intent.getStringExtra("socialSituation");

        // Set UI values
        selectedMoodText.setText(selectedMood);
        selectedMoodEmoji.setText(selectedEmoji);
        triggerInput.setText(trigger);
        socialSituationInput.setText(socialSituation);

        // Apply Gradient Background
        applyGradientBackground(selectedColor);

        // Update button click event
        updateButton.setOnClickListener(view -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updatedMood", selectedMood);
            resultIntent.putExtra("updatedEmoji", selectedEmoji);
            resultIntent.putExtra("updatedTrigger", triggerInput.getText().toString().trim());
            resultIntent.putExtra("updatedSocialSituation", socialSituationInput.getText().toString().trim());
            resultIntent.putExtra("timestamp", new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date()));
            resultIntent.putExtra("moodPosition", moodPosition);

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    // Function to apply a gradient background based on mood color
    private void applyGradientBackground(int baseColor) {
        int darkerColor = darkenColor(baseColor, 0.7f); // 70% darker shade

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{baseColor, darkerColor}
        );
        mainContainer.setBackground(gradientDrawable);
    }

    // Function to darken color for the gradient effect
    private int darkenColor(int color, float factor) {
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.rgb(r, g, b);
    }

    // Initialize Mood Colors
    private void initializeMoodColors() {
        moodColors.put("Happy", Color.parseColor("#FFD700"));
        moodColors.put("Sad", Color.parseColor("#6495ED"));
        moodColors.put("Angry", Color.parseColor("#FF4500"));
        moodColors.put("Surprised", Color.parseColor("#FFD700"));
        moodColors.put("Afraid", Color.parseColor("#4682B4"));
        moodColors.put("Disgusted", Color.parseColor("#228B22"));
        moodColors.put("Confused", Color.parseColor("#9370DB"));
        moodColors.put("Shameful", Color.parseColor("#8B0000"));
    }

    // Initialize Mood Emojis
    private void initializeMoodEmojis() {
        moodEmojis.put("Happy", "😊");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Surprised", "😲");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Confused", "😕");
        moodEmojis.put("Shameful", "😳");
    }
}
