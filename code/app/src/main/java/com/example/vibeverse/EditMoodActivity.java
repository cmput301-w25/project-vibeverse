package com.example.vibeverse;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * EditMoodActivity allows users to update an existing mood event.
 * <p>
 * This activity uses the same layout as SelectMoodActivity for consistency.
 * It receives mood details from the calling activity (e.g., MainActivity),
 * displays the current values, and allows the user to update the mood,
 * trigger, social situation, intensity, and an optional image.
 * When the user clicks "Update Mood", the updated details are sent back to the caller.
 * </p>
 */
public class EditMoodActivity extends AppCompatActivity {

    // UI Elements
    private TextView selectedMoodEmoji, selectedMoodText;
    private EditText triggerInput, reasonWhyInput;
    private Spinner socialSituationInput;
    private SeekBar moodIntensitySlider;
    private Button updateButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer; // Main screen background container
    private TextView intensityDisplay;

    // Mood properties
    private String selectedMood;
    private String selectedEmoji;
    private int selectedColor;

    // Maps for mood colors & emojis
    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    private int moodPosition; // Position of the mood in the list
    private ImageView imgSelected, imgPlaceholder;

    private String currentImageUri;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;

    private long photoSizeKB;
    private String photoDateTaken;
    private String photoLocation;
    private Bitmap currentBitmap;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components, loads mood colors and emojis,
     * sets the current mood values from the intent, applies a refined gradient background,
     * loads an image if available, and sets click listeners for updating mood and picking images.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the same layout as SelectMoodActivity
        setContentView(R.layout.activity_select_mood);

        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        triggerInput = findViewById(R.id.triggerInput);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        updateButton = findViewById(R.id.continueButton); // Reuse the same button ID
        imgSelected = findViewById(R.id.imgSelected);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);

        // Create a custom toolbar without a title
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(""); // Remove the "Edit Mood" text
        toolbar.setBackgroundColor(Color.TRANSPARENT);

        // Use the standard navigation icon for the back button
        toolbar.setNavigationIcon(getResources().getIdentifier(
                "abc_ic_ab_back_material", "drawable", getPackageName()));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Add to the top of the main container
        mainContainer.addView(toolbar, 0);

        // Instead of adding another TextView, check if one already exists
        boolean textAlreadyExists = false;
        for (int i = 0; i < mainContainer.getChildCount(); i++) {
            View child = mainContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (textView.getText().toString().contains("Choose how you're feeling")) {
                    // Text already exists, center it and stop
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    textView.setTypeface(null, Typeface.BOLD);
                    textAlreadyExists = true;
                    break;
                }
            }
        }

        // Only add new text if none was found
        if (!textAlreadyExists) {
            TextView chooseTextView = new TextView(this);
            chooseTextView.setText("Choose how you're feeling right now");
            chooseTextView.setTextColor(Color.WHITE);
            chooseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            chooseTextView.setTypeface(null, Typeface.BOLD);
            chooseTextView.setGravity(Gravity.CENTER); // Center justify
            chooseTextView.setPadding(0, dpToPx(16), 0, dpToPx(20));
            // Add this text view right after the toolbar
            mainContainer.addView(chooseTextView, 1);
        }

        // Change the button text to "Update Mood" for clarity
        updateButton.setText("Update Mood");

        // Set consistent typeface and text sizes
        selectedMoodText.setTypeface(null, Typeface.BOLD);
        selectedMoodEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        selectedMoodText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        // Create consistent styling for input fields
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dpToPx(8));
        inputBg.setColor(Color.WHITE);
        inputBg.setStroke(1, Color.parseColor("#E0E0E0"));

        // Clone the drawable for each input to avoid shared state issues
        GradientDrawable triggerBg = (GradientDrawable) inputBg.getConstantState().newDrawable().mutate();
        GradientDrawable socialBg = (GradientDrawable) inputBg.getConstantState().newDrawable().mutate();
        GradientDrawable reasonWhyBg = (GradientDrawable) inputBg.getConstantState().newDrawable().mutate();


        triggerInput.setBackground(triggerBg);
        socialSituationInput.setBackground(socialBg);
        reasonWhyInput.setBackground(reasonWhyBg);

        // Set padding for the input fields
        int paddingPx = (int) dpToPx(12);
        triggerInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        socialSituationInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        reasonWhyInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Add labels above input fields for clarity
        TextView triggerLabel = new TextView(this);
        triggerLabel.setText("What triggered this mood?");
        triggerLabel.setTextColor(Color.WHITE);
        triggerLabel.setTypeface(null, Typeface.BOLD);
        triggerLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        triggerLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        TextView socialLabel = new TextView(this);
        socialLabel.setText("Social situation");
        socialLabel.setTextColor(Color.WHITE);
        socialLabel.setTypeface(null, Typeface.BOLD);
        socialLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        socialLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        TextView reasonWhyLabel = new TextView(this);
        reasonWhyLabel.setText("Reason why you feel this way");
        reasonWhyLabel.setTextColor(Color.WHITE);
        reasonWhyLabel.setTextColor(Color.WHITE);
        reasonWhyLabel.setTypeface(null, Typeface.BOLD);
        reasonWhyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        reasonWhyLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));
      
      // Get the parent container and add the labels before the respective inputs



        int reasonWhyIndex = mainContainer.indexOfChild(reasonWhyInput);
        mainContainer.addView(reasonWhyLabel, reasonWhyIndex);


        int triggerIndex = mainContainer.indexOfChild(triggerInput);
        mainContainer.addView(triggerLabel, triggerIndex);
        int socialIndex = mainContainer.indexOfChild(socialSituationInput);
        mainContainer.addView(socialLabel, socialIndex);

        // Setup the enhanced mood intensity slider with visual indicators
        setupMoodIntensitySlider();

        // Enhance the image picker button
        GradientDrawable imageBtnBg = new GradientDrawable();
        imageBtnBg.setCornerRadius(dpToPx(12));
        imageBtnBg.setColor(Color.WHITE);
        imageBtnBg.setStroke(1, Color.parseColor("#E0E0E0"));

        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setBackground(imageBtnBg);

        // Style the image placeholder for consistency
        imgPlaceholder.setColorFilter(Color.parseColor("#AAAAAA"));

        // Add an image hint text
        TextView imageHintText = new TextView(this);
        imageHintText.setText("Add an image (optional)");
        imageHintText.setTextColor(Color.parseColor("#757575"));
        imageHintText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        imageHintText.setGravity(Gravity.CENTER);
        btnTestImage.addView(imageHintText);

        // Position the hint text below the placeholder
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.bottomMargin = (int) dpToPx(20);
        imageHintText.setLayoutParams(params);

        // Make the image container appear clickable
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnTestImage.setForeground(getDrawable(android.R.drawable.list_selector_background));
        }

        initializeMoodColors();
        initializeMoodEmojis();

        // Retrieve mood info from the Intent
        Intent intent = getIntent();
        selectedMood = intent.getStringExtra("selectedMood");
        selectedEmoji = intent.getStringExtra("selectedEmoji");
        selectedColor = moodColors.getOrDefault(selectedMood, Color.GRAY);
        moodPosition = intent.getIntExtra("moodPosition", -1);

        String timestamp = intent.getStringExtra("timestamp");
        String trigger = intent.getStringExtra("trigger");
        String reasonWhy = intent.getStringExtra("reasonWhy");
        String socialSituation = intent.getStringExtra("socialSituation");
        String currentPhotoUri = intent.getStringExtra("photoUri");

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.social_situation_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationInput.setAdapter(adapter);


        photoDateTaken = intent.getStringExtra("photoDateTaken");
        photoLocation = intent.getStringExtra("photoLocation");
        photoSizeKB = intent.getLongExtra("photoSizeKB", 0);


        // Get the intensity value from the intent (using default of 5 if not found)
        int intensity = intent.getIntExtra("intensity", 5);

        // Set UI fields with the retrieved values
        selectedMoodText.setText(selectedMood);
        selectedMoodEmoji.setText(selectedEmoji);
        triggerInput.setText(trigger);
        reasonWhyInput.setText(reasonWhy);
        if (socialSituation != null) {
            int spinnerPosition = adapter.getPosition(socialSituation);
            socialSituationInput.setSelection(spinnerPosition);
        }

        // Apply the refined gradient background for consistency
        applyGradientBackground(selectedColor);

        // Ensure selectedMoodContainer has a GradientDrawable background
        GradientDrawable moodContainerBg = new GradientDrawable();
        moodContainerBg.setColor(selectedColor);
        moodContainerBg.setCornerRadius(dpToPx(12));
        selectedMoodContainer.setBackground(moodContainerBg);

        // Set the intensity slider value
        moodIntensitySlider.setProgress(intensity);

        // Initialize the intensity display text
        if (intensityDisplay != null) {
            StringBuilder intensityBuilder = new StringBuilder();
            for (int i = 0; i <= 10; i++) {
                if (i <= intensity) {
                    intensityBuilder.append("●");
                } else {
                    intensityBuilder.append("○");
                }
            }
            intensityDisplay.setText(intensityBuilder.toString());
        }

        // Adjust emoji scale based on intensity
        float emojiScale = 0.7f + (intensity / 10f * 0.6f);
        selectedMoodEmoji.setScaleX(emojiScale);
        selectedMoodEmoji.setScaleY(emojiScale);

        // Update mood text based on intensity
        if (intensity <= 3) {
            selectedMoodText.setText("Slightly " + selectedMood);
        } else if (intensity <= 7) {
            selectedMoodText.setText(selectedMood);
        } else {
            selectedMoodText.setText("Very " + selectedMood);
        }

        // Style the update button
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setCornerRadius(dpToPx(24));
        buttonBg.setColor(Color.parseColor("#5C4B99"));  // Use consistent purple color

        // Apply elevation for a modern look
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateButton.setElevation(dpToPx(4));
        }
        updateButton.setBackground(buttonBg);
        updateButton.setPadding(
                (int) dpToPx(24),
                (int) dpToPx(12),
                (int) dpToPx(24),
                (int) dpToPx(12)
        );
        updateButton.setTextColor(Color.WHITE);
        updateButton.setTypeface(null, Typeface.BOLD);

        // Load existing photo if available
        currentImageUri = currentPhotoUri;
        if (currentPhotoUri != null && !currentPhotoUri.equals("N/A")) {
            Glide.with(this)
                    .load(currentPhotoUri)
                    .into(imgSelected);
            imgSelected.setVisibility(View.VISIBLE);
            imageHintText.setVisibility(View.GONE);
            imgPlaceholder.setVisibility(View.GONE);
        }

        // Fade-in animation for the mood container
        selectedMoodContainer.setAlpha(0f);
        selectedMoodContainer.animate()
                .alpha(1f)
                .setDuration(300)
                .start();

        // Set click listener for image picker button
        btnTestImage.setOnClickListener(v -> showImagePickerDialog());

        // Set click listener for the update button with animation
        updateButton.setOnClickListener(view -> {


            String newreasonWhy = reasonWhyInput.getText().toString().trim();

            // Validate character count
            if (newreasonWhy.length() > 20) {
                reasonWhyInput.setError("Reason why must be 20 characters or less.");
                reasonWhyInput.requestFocus();
                return;
            }

            // Validate word count
            String[] words = newreasonWhy.split("\\s+");
            if (words.length > 3) {
                reasonWhyInput.setError("Reason why must be 3 words or less.");
                reasonWhyInput.requestFocus();
                return;
            }

            // Show updating toast
            Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show();
            mainContainer.animate()
                    .alpha(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {



                        // Original code for handling the update

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("updatedMood", selectedMood);
                        resultIntent.putExtra("updatedEmoji", selectedEmoji);
                        resultIntent.putExtra("updatedReasonWhy", reasonWhyInput.getText().toString().trim());
                        resultIntent.putExtra("updatedTrigger", triggerInput.getText().toString().trim());
                        resultIntent.putExtra("updatedSocialSituation", socialSituationInput.getSelectedItem().toString().trim());
                        resultIntent.putExtra("timestamp", new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date()));
                        resultIntent.putExtra("moodPosition", moodPosition);
                        resultIntent.putExtra("updatedPhotoUri", (currentImageUri != null) ? currentImageUri : "N/A");
                        resultIntent.putExtra("updatedIntensity", moodIntensitySlider.getProgress());
                        resultIntent.putExtra("updatedphotoDateTaken", photoDateTaken);
                        resultIntent.putExtra("updatedphotoLocation", photoLocation);
                        resultIntent.putExtra("updatedphotoSizeKB", photoSizeKB);


                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .start();
        });
    }

    /**
     * Applies a gradient background to the main container using the base mood color.
     *
     * @param baseColor The base mood color.
     */
    private void applyGradientBackground(int baseColor) {
        int lighterColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.7f);
        int mediumColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.3f);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2D2D3A"), lighterColor, mediumColor, baseColor}
        );
        gradient.setCornerRadius(0f);
        TransitionManager.beginDelayedTransition(mainContainer);
        mainContainer.setBackground(gradient);
    }

    /**
     * Adjusts the brightness of a given color by a specified factor.
     *
     * @param color  The original color.
     * @param factor The multiplier for brightness.
     * @return The adjusted color.
     */
    private int adjustColorBrightness(int color, float factor) {
        int r = Math.min(255, (int) (Color.red(color) * factor));
        int g = Math.min(255, (int) (Color.green(color) * factor));
        int b = Math.min(255, (int) (Color.blue(color) * factor));
        return Color.rgb(r, g, b);
    }

    /**
     * Converts dp (density-independent pixels) to actual pixel units.
     *
     * @param dp The dp value.
     * @return The equivalent pixel value.
     */
    private int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }

    /**
     * Initializes the mood colors map.
     */
    private void initializeMoodColors() {
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink
    }

    /**
     * Initializes the mood emojis map.
     */
    private void initializeMoodEmojis() {
        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Surprised", "😲");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Shameful", "😳");
    }

    /**
     * Sets up the mood intensity slider with dynamic visual feedback.
     */
    private void setupMoodIntensitySlider() {
        // Create a slider label
        TextView sliderLabel = new TextView(this);
        sliderLabel.setText("Mood Intensity");
        sliderLabel.setTextColor(Color.WHITE);
        sliderLabel.setTypeface(null, Typeface.BOLD);
        sliderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sliderLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        LinearLayout sliderContainer = new LinearLayout(this);
        sliderContainer.setOrientation(LinearLayout.VERTICAL);

        // Get slider's parent
        ViewGroup sliderParent = (ViewGroup) moodIntensitySlider.getParent();
        int sliderIndex = sliderParent.indexOfChild(moodIntensitySlider);

        // Remove slider from its current parent
        sliderParent.removeView(moodIntensitySlider);

        // Style the slider
        moodIntensitySlider.setMax(10);
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));
        moodIntensitySlider.setThumbTintList(ColorStateList.valueOf(Color.WHITE));

        // Set up min/max labels with dynamic intensity indicator
        LinearLayout minMaxContainer = new LinearLayout(this);
        minMaxContainer.setOrientation(LinearLayout.HORIZONTAL);

        TextView minLabel = new TextView(this);
        minLabel.setText("Low");
        minLabel.setTextColor(Color.WHITE);
        minLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        // Create intensity display text that updates with slider changes
        intensityDisplay = new TextView(this);
        intensityDisplay.setText("●●●●●○○○○○");
        intensityDisplay.setTextColor(Color.WHITE);
        intensityDisplay.setTypeface(null, Typeface.BOLD);
        intensityDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        intensityDisplay.setGravity(Gravity.CENTER);

        TextView maxLabel = new TextView(this);
        maxLabel.setText("High");
        maxLabel.setTextColor(Color.WHITE);
        maxLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        // Set up layout parameters for labels
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        minParams.weight = 1;
        minLabel.setLayoutParams(minParams);

        LinearLayout.LayoutParams intensityParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        intensityParams.weight = 3;
        intensityDisplay.setLayoutParams(intensityParams);

        LinearLayout.LayoutParams maxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        maxParams.weight = 1;
        maxLabel.setLayoutParams(maxParams);

        minMaxContainer.addView(minLabel);
        minMaxContainer.addView(intensityDisplay);
        minMaxContainer.addView(maxLabel);

        // Add slider components to the container and reattach to parent
        sliderContainer.addView(sliderLabel);
        sliderContainer.addView(moodIntensitySlider);
        sliderContainer.addView(minMaxContainer);
        sliderParent.addView(sliderContainer, sliderIndex);

        // Create a pulse animation for the mood container
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(selectedMoodContainer, "scaleX", 1f, 1.05f);
        pulseAnimator.setDuration(300);
        pulseAnimator.setRepeatCount(1);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        ObjectAnimator pulseAnimatorY = ObjectAnimator.ofFloat(selectedMoodContainer, "scaleY", 1f, 1.05f);
        pulseAnimatorY.setDuration(300);
        pulseAnimatorY.setRepeatCount(1);
        pulseAnimatorY.setRepeatMode(ValueAnimator.REVERSE);

        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseAnimator, pulseAnimatorY);

        // Listener to update intensity effects as slider changes
        moodIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                applyIntensityEffects(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional behavior when touch starts
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Play pulse animation when slider is released
                pulseSet.start();
            }
        });
    }

    /**
     * Applies UI changes based on the selected mood intensity.
     *
     * @param progress The current intensity value (0-10).
     */
    private void applyIntensityEffects(int progress) {
        // Update intensity display text
        updateIntensityDisplay(intensityDisplay, progress);

        // Adjust emoji size dynamically
        float emojiScale = 0.7f + (progress / 10f * 0.6f); // Scale from 0.7 to 1.3
        selectedMoodEmoji.setScaleX(emojiScale);
        selectedMoodEmoji.setScaleY(emojiScale);

        // Adjust background color based on intensity
        int adjustedColor = adjustColorIntensity(selectedColor, progress);

        // Update the mood container background color
        if (!(selectedMoodContainer.getBackground() instanceof GradientDrawable)) {
            GradientDrawable newBg = new GradientDrawable();
            newBg.setColor(adjustedColor);
            newBg.setCornerRadius(dpToPx(12));
            selectedMoodContainer.setBackground(newBg);
        } else {
            GradientDrawable moodContainerBg = (GradientDrawable) selectedMoodContainer.getBackground();
            moodContainerBg.setColor(adjustedColor);
        }

        // Update mood text to reflect intensity
        if (progress <= 3) {
            selectedMoodText.setText("Slightly " + selectedMood);
        } else if (progress <= 7) {
            selectedMoodText.setText(selectedMood);
        } else {
            selectedMoodText.setText("Very " + selectedMood);
        }
    }

    /**
     * Updates the intensity display (dots) based on the slider progress.
     *
     * @param intensityDisplay The TextView showing intensity representation.
     * @param progress         The current slider progress (0-10).
     */
    private void updateIntensityDisplay(TextView intensityDisplay, int progress) {
        if (intensityDisplay == null) return;
        StringBuilder intensityBuilder = new StringBuilder();
        for (int i = 0; i <= 10; i++) {
            if (i <= progress) {
                intensityBuilder.append("●"); // Filled circle for active levels
            } else {
                intensityBuilder.append("○"); // Empty circle for inactive levels
            }
        }
        intensityDisplay.setText(intensityBuilder.toString());
        intensityDisplay.setAlpha(0.7f);
        intensityDisplay.animate().alpha(1.0f).setDuration(200).start();
    }

    /**
     * Adjusts the base color based on the selected intensity.
     *
     * @param baseColor The original mood color.
     * @param intensity The intensity value (0-10).
     * @return The color adjusted for intensity.
     */
    private int adjustColorIntensity(int baseColor, int intensity) {
        if (intensity < 5) {
            float blendRatio = 0.5f + (intensity / 10f); // 0.5 to 1.0
            return ColorUtils.blendColors(baseColor, Color.GRAY, blendRatio);
        }
        // For high intensity, make more vibrant/darker
        else if (intensity > 5) {
            // Increase saturation and adjust brightness
            float factor = 1.0f + ((intensity - 5) / 5f * 0.3f); // 1.0 to 1.3
            return adjustColorSaturation(baseColor, factor);
        }
        // Middle intensity, return base color
        else {
            return baseColor;
        }
    }

    /**
     * Adjusts the saturation of the given color.
     *
     * @param color  The original color.
     * @param factor The factor to adjust saturation (>1 for increased, <1 for decreased).
     * @return The color with adjusted saturation.
     */
    private int adjustColorSaturation(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] * factor);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * (factor > 1 ? 0.9f : 1.1f)));
        return Color.HSVToColor(hsv);
    }

    /**
     * Called when permission results are returned.
     *
     * @param requestCode  The permission request code.
     * @param permissions  The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Handles results from camera or gallery intents.
     *
     * @param requestCode The request code identifying the action.
     * @param resultCode  The result code from the child activity.
     * @param data        The intent data returned (if any).
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // For camera, imageUri is already set
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    photoSizeKB = sizeKB;
                    currentBitmap = bitmap;
                    currentImageUri = uri.toString();
                    imgPlaceholder.setVisibility(View.GONE);
                    // Also hide the hint text when image is selected
                    for (int i = 0; i < ((ViewGroup) imgPlaceholder.getParent()).getChildCount(); i++) {
                        View child = ((ViewGroup) imgPlaceholder.getParent()).getChildAt(i);
                        if (child instanceof TextView) {
                            child.setVisibility(View.GONE);
                            break;
                        }
                    }
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    photoSizeKB = sizeKB;
                    currentBitmap = bitmap;
                    currentImageUri = uri.toString();
                    imgPlaceholder.setVisibility(View.GONE);
                    // Hide hint text
                    for (int i = 0; i < ((ViewGroup) imgPlaceholder.getParent()).getChildCount(); i++) {
                        View child = ((ViewGroup) imgPlaceholder.getParent()).getChildAt(i);
                        if (child instanceof TextView) {
                            child.setVisibility(View.GONE);
                            break;
                        }
                    }
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            }
        }
    }

    /**
     * Displays a dialog for the user to choose an image source or remove the current photo.
     */
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Remove Photo"}, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else if (which == 1) {
                        dispatchPickImageIntent();
                    } else {
                        // Remove photo option
                        currentImageUri = "N/A";
                        imgSelected.setVisibility(View.GONE);
                        imgPlaceholder.setVisibility(View.VISIBLE);
                        // Show hint text again
                        for (int i = 0; i < ((ViewGroup) imgPlaceholder.getParent()).getChildCount(); i++) {
                            View child = ((ViewGroup) imgPlaceholder.getParent()).getChildAt(i);
                            if (child instanceof TextView) {
                                child.setVisibility(View.VISIBLE);
                                break;
                            }
                        }
                    }
                })
                .show();
    }

    /**
     * Dispatches an intent to capture an image using the device camera.
     * Creates a temporary file and requests necessary permissions.
     */
    private void dispatchTakePictureIntent() {
        requestPermissions();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = ImageUtils.createImageFile(this);
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                ex.printStackTrace();
                return;
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this,
                        "com.example.vibeverse.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Dispatches an intent to pick an image from the gallery.
     */
    private void dispatchPickImageIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
    }

    /**
     * Requests necessary permissions (Camera and Storage) for Android M and above.
     */
    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Utility class for color blending operations.
     */
    private static class ColorUtils {
        /**
         * Blends two colors together using the specified ratio.
         *
         * @param color1 The first color.
         * @param color2 The second color.
         * @param ratio  The blending ratio (0.0 to 1.0).
         * @return The resulting blended color.
         */
        public static int blendColors(int color1, int color2, float ratio) {
            final float inverseRatio = 1f - ratio;
            float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRatio);
            float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRatio);
            float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRatio);
            return Color.rgb((int) r, (int) g, (int) b);
        }
    }
}