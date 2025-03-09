package com.example.vibeverse;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * SelectMoodActivity provides a sleek, professional UI for users to select their mood,
 * adjust mood intensity, optionally add trigger and social situation information, and choose an image.
 * <p>
 * The activity displays a grid of mood buttons, a large mood display area with a dynamic gradient background,
 * a smooth Material Slider for mood intensity, and input fields with rounded corners.
 * It also supports capturing or picking an image.
 * </p>
 */
public class SelectMoodActivity extends AppCompatActivity {

    // UI Elements
    private TextView selectedMoodEmoji, selectedMoodText;
    private SeekBar moodIntensitySlider;
    private EditText triggerInput, reasonWhyInput;
    private Spinner socialSituationInput;
    private Button continueButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer; // Container for gradient background and transitions

    // Mood properties
    private String selectedMood = "Happy"; // Default mood
    private String selectedEmoji = "😃";
    private int selectedColor = Color.parseColor("#FBC02D");
    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    // Image handling constants and fields
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;
    private long photoSizeKB;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder, imgSelected;
    private TextView imageHintText;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    /**
     * Called when the activity is starting. Initializes the UI, sets up mood data,
     * creates mood buttons, configures the image picker, and sets the click listener for the continue button.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mood);

        // Initialize UI references
        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        triggerInput = findViewById(R.id.triggerInput);
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        continueButton = findViewById(R.id.continueButton);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);


        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID or use a device ID if not logged in
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);

            // If no device ID exists, create one
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }

        // Create a custom toolbar
        setupToolbar();

        // REMOVE THIS SECTION THAT ADDS THE DUPLICATE TEXT
        // TextView chooseTextView = new TextView(this);
        // chooseTextView.setText("Choose how you're feeling right now");
        // chooseTextView.setTextColor(Color.WHITE);
        // chooseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        // chooseTextView.setTypeface(null, Typeface.BOLD);
        // chooseTextView.setGravity(Gravity.CENTER);
        // chooseTextView.setPadding(0, dpToPx(16), 0, dpToPx(20));
        // mainContainer.addView(chooseTextView, 1);

        // Set continue button text and style
        setupContinueButton();

        // Initialize mood colors and emojis
        initializeMoodData();

        // Style the mood intensity slider
        setupMoodIntensitySlider();

        // Style the input fields
        setupInputFields();

        // Enhance the image selector
        setupImageSelector();

        // Build the mood selection grid with a polished, uniform design
        GridLayout moodGrid = findViewById(R.id.moodGrid);
        createMoodButtons(moodGrid);

        // Set the initial mood and apply its style
        selectMood(selectedMood);

        // Set up continue button to create a MoodEvent and pass it to MainActivity
        continueButton.setOnClickListener(v -> {

            Log.d("SelectMoodActivity", "onClick: selectedEmoji before animation = " + selectedEmoji);
            // Create a subtle animation before continuing
            mainContainer.animate()

                    .alpha(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        Log.d("SelectMoodActivity", "onClick: selectedEmoji after animation = " + selectedEmoji);
                        String trigger = triggerInput.getText().toString().trim();
                        String socialSituation = socialSituationInput.getSelectedItem().toString().trim();
                        String reasonWhy = reasonWhyInput.getText().toString().trim();


                        // Error handling for reasonWhy input
                        if (reasonWhy.length() > 20) {
                            reasonWhyInput.setError("Reason why must be 20 characters or less.");
                            reasonWhyInput.requestFocus();
                            return;
                        }

                        String[] words = reasonWhy.split("\\s+");
                        if (words.length > 3) {
                            reasonWhyInput.setError("Reason why must be 3 words or less.");
                            reasonWhyInput.requestFocus();
                            return;
                        }

                        // Store the intensity value in the MoodEvent
                        int intensity = moodIntensitySlider.getProgress();

                        MoodEvent moodEvent;
                        if (imageUri != null && currentBitmap != null) {
                            // If an image is selected, create a Photograph instance and attach it to the mood event
                            // Using the existing Photograph constructor that matches your implementation
                            Photograph photograph = new Photograph(
                                    imageUri,
                                    photoSizeKB, // Estimate file size in KB
                                    currentBitmap,
                                    new Date(),
                                    "VibeVerse Location" // Default location - get location functionality not yet implemented
                            );


                            moodEvent = new MoodEvent(selectedMood, selectedEmoji, reasonWhy, trigger, socialSituation, photograph);
                            // Add intensity to the mood event
                            moodEvent.setIntensity(intensity);
                        } else {
                            Log.d("SelectMoodActivity", "onClick: Emoji before assignment= " + selectedEmoji);
                            moodEvent = new MoodEvent(selectedMood, selectedEmoji, reasonWhy, trigger, socialSituation);
                            Log.d("SelectMoodActivity", "onClick: Emoji after assignment= " + moodEvent.getEmoji());
                            Log.d("SelectMoodActivity", "onClick: Title after assignment= " + moodEvent.getMoodTitle());
                            // Add intensity to the mood event
                            moodEvent.setIntensity(intensity);
                        }

                        // Save to Firestore
                        saveMoodToFirestore(moodEvent);
                    })
                    .start();
        });
    }

    private void saveMoodToFirestore(MoodEvent moodEvent) {
        // Show a loading indicator
        Toast.makeText(this, "Saving your mood...", Toast.LENGTH_SHORT).show();

        // Convert MoodEvent to Map for Firestore
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("emoji", moodEvent.getEmoji());
        moodData.put("mood", moodEvent.getMoodTitle());
        moodData.put("trigger", moodEvent.getTrigger());
        moodData.put("socialSituation", moodEvent.getSocialSituation());
        moodData.put("timestamp", moodEvent.getTimestamp());
        moodData.put("intensity", moodEvent.getIntensity());
        moodData.put("reasonWhy", moodEvent.getReasonWhy());

        // Handle photograph if present
        if (moodEvent.getPhotograph() != null) {
            moodData.put("hasPhoto", true);
            moodData.put("photoUri", moodEvent.getPhotoUri());



            moodData.put("photoDateTaken", moodEvent.getPhotograph().getDateTaken().getTime());
            moodData.put("photoLocation", moodEvent.getPhotograph().getLocation());
            moodData.put("photoSizeKB", moodEvent.getPhotograph().getFileSizeKB());

            // Note: For a complete solution, you would upload the actual image to Firebase Storage
            // and store the download URL. This simplified version just stores the local URI.
        } else {
            moodData.put("hasPhoto", false);
        }

        // Add to Firestore - create a document with timestamp as ID
        String docId = String.valueOf(System.currentTimeMillis());

        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .document(docId)
                .set(moodData)
                .addOnSuccessListener(aVoid -> {
                    // Success! Now return to ProfilePage
                    Toast.makeText(SelectMoodActivity.this, "Mood saved successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
                    startActivity(intent);

                    // Apply a fade-out transition
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Handle the error
                    Toast.makeText(SelectMoodActivity.this, "Error saving mood: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();

                    // Still navigate back to avoid user being stuck
                    Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
                    startActivity(intent);
                    finish();
                });
    }
    /**
     * Sets up a custom toolbar
     */
    private void setupToolbar() {
        Toolbar toolbar = new Toolbar(this);

        // Remove the "Add New Mood" title text as requested
        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.TRANSPARENT);

        // Use the standard navigation icon for the back button
        toolbar.setNavigationIcon(getResources().getIdentifier(
                "abc_ic_ab_back_material", "drawable", getPackageName()));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Add to the top of the main container
        mainContainer.addView(toolbar, 0);
    }

    /**
     * Sets up the continue button with professional styling
     */
    private void setupContinueButton() {
        // Style the continue button to match the purple color from the screenshot
        int buttonColor = Color.parseColor("#5C4B99");  // Deep purple color

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setCornerRadius(dpToPx(24));
        buttonBg.setColor(buttonColor);

        // Apply elevation for a modern look
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            continueButton.setElevation(dpToPx(4));
        }

        continueButton.setBackground(buttonBg);

        // Proper padding for the button
        continueButton.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));

        // Update text and style
        continueButton.setText("Continue");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            continueButton.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        continueButton.setTextColor(Color.WHITE);
        continueButton.setTypeface(null, Typeface.BOLD);

        // Ensure the button is properly positioned at the bottom right
        if (continueButton.getParent() instanceof FrameLayout) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) continueButton.getLayoutParams();
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            continueButton.setLayoutParams(params);
        }
    }

    /**
     * Sets up the mood intensity slider with visual feedback effects
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
        moodIntensitySlider.setProgress(5); // Default to middle
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));
        moodIntensitySlider.setThumbTintList(ColorStateList.valueOf(Color.WHITE));

        // Set up min/max labels with dynamic intensity indicator
        LinearLayout minMaxContainer = new LinearLayout(this);
        minMaxContainer.setOrientation(LinearLayout.HORIZONTAL);

        TextView minLabel = new TextView(this);
        minLabel.setText("Low");
        minLabel.setTextColor(Color.WHITE);
        minLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        // Create intensity display text that will change with slider
        TextView intensityDisplay = new TextView(this);
        intensityDisplay.setText("••••••");
        intensityDisplay.setTextColor(Color.WHITE);
        intensityDisplay.setTypeface(null, Typeface.BOLD);
        intensityDisplay.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        intensityDisplay.setGravity(Gravity.CENTER);

        TextView maxLabel = new TextView(this);
        maxLabel.setText("High");
        maxLabel.setTextColor(Color.WHITE);
        maxLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

        // Set up layout parameters
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

        // Add views to container
        sliderContainer.addView(sliderLabel);
        sliderContainer.addView(moodIntensitySlider);
        sliderContainer.addView(minMaxContainer);

        // Add the container back to the parent
        sliderParent.addView(sliderContainer, sliderIndex);

        // Create a pulse animation for the selected mood container
        ObjectAnimator pulseAnimator = ObjectAnimator.ofFloat(selectedMoodContainer, "scaleX", 1f, 1.05f);
        pulseAnimator.setDuration(300);
        pulseAnimator.setRepeatCount(1);
        pulseAnimator.setRepeatMode(ValueAnimator.REVERSE);

        // Create another animator for Y scale
        ObjectAnimator pulseAnimatorY = ObjectAnimator.ofFloat(selectedMoodContainer, "scaleY", 1f, 1.05f);
        pulseAnimatorY.setDuration(300);
        pulseAnimatorY.setRepeatCount(1);
        pulseAnimatorY.setRepeatMode(ValueAnimator.REVERSE);

        // Combine into animation set
        AnimatorSet pulseSet = new AnimatorSet();
        pulseSet.playTogether(pulseAnimator, pulseAnimatorY);

        // Set up listener for the slider to update visual elements
        moodIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update intensity display dots
                updateIntensityDisplay(intensityDisplay, progress);

                // Dynamically adjust emoji size based on intensity
                float emojiScale = 0.7f + (progress / 10f * 0.6f); // Scale from 0.7 to 1.3
                selectedMoodEmoji.setScaleX(emojiScale);
                selectedMoodEmoji.setScaleY(emojiScale);

                // Change color intensity of the mood display
                int adjustedColor = adjustColorIntensity(selectedColor, progress);
                GradientDrawable moodContainerBg = (GradientDrawable) selectedMoodContainer.getBackground();
                moodContainerBg.setColor(adjustedColor);

                // Update text display based on intensity
                if (progress <= 3) {
                    selectedMoodText.setText("Slightly " + selectedMood);
                } else if (progress <= 7) {
                    selectedMoodText.setText(selectedMood);
                } else {
                    selectedMoodText.setText("Very " + selectedMood);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Optional: Add effect when user starts touching slider
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Play pulse animation when slider is released
                pulseSet.start();
            }
        });

        // Initialize with default value
        updateIntensityDisplay(intensityDisplay, moodIntensitySlider.getProgress());
    }

    /**
     * Updates the visual intensity display based on slider progress
     *
     * @param intensityDisplay The TextView showing intensity
     * @param progress The current slider progress
     */
    private void updateIntensityDisplay(TextView intensityDisplay, int progress) {
        // Create a dynamic visual representation of intensity
        StringBuilder intensityBuilder = new StringBuilder();

        // Use filled and empty circles to represent intensity
        for (int i = 0; i <= 10; i++) {
            if (i <= progress) {
                intensityBuilder.append("●"); // Filled circle for active levels
            } else {
                intensityBuilder.append("○"); // Empty circle for inactive levels
            }
        }

        intensityDisplay.setText(intensityBuilder.toString());

        // Animate the text change
        intensityDisplay.setAlpha(0.7f);
        intensityDisplay.animate().alpha(1.0f).setDuration(200).start();
    }

    /**
     * Adjusts color intensity based on slider position
     *
     * @param baseColor The original mood color
     * @param intensity The intensity value (0-10)
     * @return An adjusted color based on intensity
     */
    private int adjustColorIntensity(int baseColor, int intensity) {
        // For low intensity, blend with gray to reduce saturation
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
     * Increases or decreases the saturation of a color
     *
     * @param color The original color
     * @param factor Factor to adjust saturation by (>1 for more saturation, <1 for less)
     * @return The adjusted color
     */
    private int adjustColorSaturation(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);

        // Adjust saturation (component 1)
        hsv[1] = Math.min(1f, hsv[1] * factor);

        // Optionally adjust value/brightness (component 2) for more dramatic effect
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * (factor > 1 ? 0.9f : 1.1f)));

        return Color.HSVToColor(hsv);
    }

    /**
     * Sets up the input fields with labels and styled components
     */
    private void setupInputFields() {
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


        // Get the parent container
        int reasonWhyIndex = mainContainer.indexOfChild(reasonWhyInput);
        mainContainer.addView(reasonWhyLabel, reasonWhyIndex);

        int triggerIndex = mainContainer.indexOfChild(triggerInput);
        mainContainer.addView(triggerLabel, triggerIndex);

        int socialIndex = mainContainer.indexOfChild(socialSituationInput);
        mainContainer.addView(socialLabel, socialIndex);



        // Update the hints to be more professional
        triggerInput.setHint("What caused this feeling? (Optional)");
    }

    /**
     * Sets up the image selector with improved styling
     */
    private void setupImageSelector() {
        // Enhance the image picker button
        GradientDrawable imageBtnBg = new GradientDrawable();
        imageBtnBg.setCornerRadius(dpToPx(12));
        imageBtnBg.setColor(Color.WHITE);
        imageBtnBg.setStroke(1, Color.parseColor("#E0E0E0"));

        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setBackground(imageBtnBg);

        // Style the image placeholder for consistency
        imgPlaceholder.setColorFilter(Color.parseColor("#AAAAAA"));

        // Add an image label
        TextView imageLabel = new TextView(this);
        imageLabel.setText("Add a photo (optional)");
        imageLabel.setTextColor(Color.WHITE);
        imageLabel.setTypeface(null, Typeface.BOLD);
        imageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        imageLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        // Add the label before the image container
        int imageIndex = mainContainer.indexOfChild(btnTestImage);
        mainContainer.addView(imageLabel, imageIndex);

        // Add an image hint text
        imageHintText = new TextView(this);
        imageHintText.setText("Tap to add a photo");
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

        // Add click listener
        btnTestImage.setOnClickListener(v -> showImagePickerDialog());
    }

    /**
     * Creates the mood selection buttons within the provided GridLayout.
     * Each button is styled uniformly with rounded corners, elevation, and a consistent margin.
     *
     * @param moodGrid The GridLayout where mood buttons will be added.
     */
    private void createMoodButtons(GridLayout moodGrid) {
        // Clear existing buttons if any
        moodGrid.removeAllViews();

        for (String mood : moodEmojis.keySet()) {
            // Create a card view for each mood button for better appearance
            MaterialCardView cardView = new MaterialCardView(this);
            cardView.setCardElevation(dpToPx(2));
            cardView.setRadius(dpToPx(12));
            cardView.setCardBackgroundColor(moodColors.get(mood));
            cardView.setStrokeWidth(0);
            cardView.setUseCompatPadding(true);

            // Create a layout for the card content
            LinearLayout buttonContent = new LinearLayout(this);
            buttonContent.setOrientation(LinearLayout.VERTICAL);
            buttonContent.setGravity(Gravity.CENTER);
            buttonContent.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

            // Create emoji text view
            TextView emojiView = new TextView(this);
            emojiView.setText(moodEmojis.get(mood));
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            emojiView.setGravity(Gravity.CENTER);

            // Create mood name text view
            TextView moodNameView = new TextView(this);
            moodNameView.setText(mood);
            moodNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            moodNameView.setTextColor(Color.WHITE);
            moodNameView.setGravity(Gravity.CENTER);
            moodNameView.setTypeface(null, Typeface.BOLD);

            // Add views to layout
            buttonContent.addView(emojiView);
            buttonContent.addView(moodNameView);
            cardView.addView(buttonContent);

            // Set up the card layout parameters
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            cardView.setLayoutParams(params);

            // Set click listener
            final String moodName = mood;
            cardView.setOnClickListener(v -> selectMood(moodName));

            // Add to grid
            moodGrid.addView(cardView);
        }
    }

    /**
     * Converts dp (density-independent pixels) to px (pixels) based on the device density.
     *
     * @param dp The value in dp.
     * @return The converted value in pixels.
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Initializes the mood colors and emojis with a professional, consistent palette.
     */
    private void initializeMoodData() {
        // Define mood colors with a professional palette
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink

        // Define mood emojis
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
     * Updates the UI when a mood is selected.
     * Sets the selected mood, emoji, and color; updates the mood display area; and applies a gradient background.
     *
     * @param mood The mood selected by the user.
     */
    private void selectMood(String mood) {
        selectedMood = mood;
        selectedEmoji = moodEmojis.get(mood);
        selectedColor = moodColors.get(mood);

        // Update the emoji and text display with animation
        selectedMoodContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            selectedMoodText.setText(mood);
            selectedMoodEmoji.setText(selectedEmoji);

            // Apply polished styling to the mood container
            GradientDrawable moodContainerBg = new GradientDrawable();
            moodContainerBg.setColor(selectedColor);
            moodContainerBg.setCornerRadius(dpToPx(12));
            selectedMoodContainer.setBackground(moodContainerBg);

            // Fade back in
            selectedMoodContainer.animate().alpha(1f).setDuration(300).start();
        }).start();

        // Apply a gradient background with a smooth transition
        applyGradientBackground(selectedColor);

        // Update the slider color to match mood
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));

        // DO NOT update the continue button color, it remains purple
    }

    /**
     * Applies a gradient background to the main container.
     * The gradient starts with a neutral color (#FAFAFA) at the top to ensure readability,
     * then transitions through lighter and darker shades of the mood color.
     *
     * @param baseColor The base mood color.
     */
    private void applyGradientBackground(int baseColor) {
        // Create more subtle, professional gradients
        int lighterColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.7f);
        int mediumColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.3f);

        // More professional gradient with smoother transitions
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2D2D3A"), lighterColor, mediumColor, baseColor}
        );
        gradient.setCornerRadius(0f);

        TransitionManager.beginDelayedTransition(mainContainer);
        mainContainer.setBackground(gradient);
    }

    /**
     * Displays a dialog for the user to choose between taking a photo or selecting one from the gallery.
     */
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Remove Photo"}, (dialog, which) -> {
                    if (which == 0) {
                        dispatchTakePictureIntent();
                    } else if (which == 1) {
                        dispatchPickImageIntent();
                    } else if (which == 2 && imageUri != null) {
                        // Remove photo option
                        imageUri = null;
                        imgSelected.setVisibility(View.GONE);
                        imgPlaceholder.setVisibility(View.VISIBLE);
                        imageHintText.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }

    /**
     * Dispatches an intent to capture an image using the device camera.
     * Creates a temporary file for the photo and requests necessary permissions.
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
     * Dispatches an intent to pick an image from the device gallery.
     */
    private void dispatchPickImageIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
    }

    /**
     * Requests the necessary permissions (Camera and Storage) at runtime for Android M and above.
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
     * Called when permission requests complete.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
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
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode  The integer result code returned by the child activity.
     * @param data        An Intent that can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                ImageUtils.processImage(this, imageUri, (bitmap, downloadUrl, sizeKB) -> {
                    photoSizeKB = sizeKB;
                    currentBitmap = bitmap;
                    imgPlaceholder.setVisibility(View.GONE);
                    imageHintText.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                    imageUri = downloadUrl;
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                Log.d("SelectMoodActivity", "galleryPhotoUri: " + imageUri);
                ImageUtils.processImage(this, imageUri, (bitmap, downloadUrl, sizeKB) -> {
                    photoSizeKB = sizeKB;
                    currentBitmap = bitmap;
                    imgPlaceholder.setVisibility(View.GONE);
                    imageHintText.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                    imageUri = downloadUrl;

                });
            }
        }
    }

    /**
     * Utility class for color manipulation
     */
    private static class ColorUtils {
        public static int blendColors(int color1, int color2, float ratio) {
            final float inverseRatio = 1f - ratio;
            float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRatio);
            float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRatio);
            float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRatio);
            return Color.rgb((int) r, (int) g, (int) b);
        }
    }
}