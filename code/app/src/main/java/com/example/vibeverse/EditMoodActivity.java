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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
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
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;

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
 * This activity uses a dedicated layout for editing mood events.
 * It receives mood details from the calling activity (e.g., MainActivity),
 * displays the current values, and allows the user to update the mood,
 * trigger, social situation, intensity, and an optional image.
 * When the user clicks "Update Mood", the updated details are sent back to the caller.
 * </p>
 */
public class EditMoodActivity extends AppCompatActivity {

    // UI Elements
    private TextView selectedMoodEmoji, selectedMoodText, intensityDisplay;
    private EditText triggerInput, reasonWhyInput;
    private Spinner socialSituationInput;
    private SeekBar moodIntensitySlider;
    private Button updateButton;
    private ImageView backButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer;
    private ImageView imgSelected, imgPlaceholder;
    private TextView imageHintText;

    // Mood properties
    private String selectedMood;
    private String selectedEmoji;
    private int selectedColor;

    // Maps for mood colors & emojis
    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    private int moodPosition; // Position of the mood in the list

    private String currentImageUri;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;

    private long photoSize;
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
        setContentView(R.layout.activity_edit_mood);

        initializeUIElements();
        initializeMoodData();
        populateDataFromIntent();
        setupEventListeners();
        applyGradientBackground(selectedColor);
    }

    /**
     * Initialize references to all UI elements
     */
    private void initializeUIElements() {
        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        intensityDisplay = findViewById(R.id.intensityDisplay);
        triggerInput = findViewById(R.id.triggerInput);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        updateButton = findViewById(R.id.updateButton);
        backButton = findViewById(R.id.backArrow);
        imgSelected = findViewById(R.id.imgSelected);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imageHintText = findViewById(R.id.imageHintText);
    }

    /**
     * Initialize mood colors and emojis maps
     */
    private void initializeMoodData() {
        // Initialize mood colors
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink

        // Initialize mood emojis
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
     * Populate the UI with data from the intent
     */
    private void populateDataFromIntent() {
        // Retrieve mood info from the Intent
        Intent intent = getIntent();
        selectedMood = intent.getStringExtra("selectedMood");
        selectedEmoji = intent.getStringExtra("selectedEmoji");
        selectedColor = moodColors.getOrDefault(selectedMood, Color.GRAY);
        moodPosition = intent.getIntExtra("moodPosition", -1);

        String trigger = intent.getStringExtra("trigger");
        String reasonWhy = intent.getStringExtra("reasonWhy");
        String socialSituation = intent.getStringExtra("socialSituation");
        String currentPhotoUri = intent.getStringExtra("photoUri");
        int intensity = intent.getIntExtra("intensity", 5);

        photoDateTaken = intent.getStringExtra("photoDateTaken");
        photoLocation = intent.getStringExtra("photoLocation");
        photoSize = intent.getLongExtra("photoSizeKB", 0);

        // Set UI fields with the retrieved values
        selectedMoodText.setText(selectedMood);
        selectedMoodEmoji.setText(selectedEmoji);
        triggerInput.setText(trigger);
        reasonWhyInput.setText(reasonWhy);

        // Set up the spinner with social situation options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.social_situation_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationInput.setAdapter(adapter);

        if (socialSituation != null) {
            int spinnerPosition = adapter.getPosition(socialSituation);
            socialSituationInput.setSelection(spinnerPosition);
        }

        // Apply the mood color to the container
        GradientDrawable moodContainerBg = new GradientDrawable();
        moodContainerBg.setColor(selectedColor);
        moodContainerBg.setCornerRadius(dpToPx(12));
        selectedMoodContainer.setBackground(moodContainerBg);

        // Set the intensity slider value
        moodIntensitySlider.setProgress(intensity);
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));

        // Update the intensity display
        updateIntensityDisplay(intensity);

        // Adjust emoji scale based on intensity
        float emojiScale = 0.7f + (intensity / 10f * 0.6f);
        selectedMoodEmoji.setScaleX(emojiScale);
        selectedMoodEmoji.setScaleY(emojiScale);

        // Update mood text based on intensity
        updateMoodTextBasedOnIntensity(intensity);

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
    }

    /**
     * Set up UI event listeners
     */
    private void setupEventListeners() {
        // Set up mood intensity slider listener
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
                animateMoodContainerPulse();
            }
        });

        // Set click listener for image picker button
        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setOnClickListener(v -> showImagePickerDialog());

        // Set click listener for the update button
        updateButton.setOnClickListener(view -> {
            String newReasonWhy = reasonWhyInput.getText().toString().trim();

            // Validate input
            if (!validateInput(newReasonWhy)) {
                return;
            }

            // Show updating toast
            Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show();
            mainContainer.animate()
                    .alpha(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Return updated mood data to caller
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
                        resultIntent.putExtra("updatedphotoSizeKB", photoSize);

                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .start();
        });

        // Set click listener for back button
        backButton.setOnClickListener(v -> {
            Intent goBackIntent = new Intent(EditMoodActivity.this, ProfilePage.class);
            goBackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
            startActivity(goBackIntent);
            finish();
        });
    }

    /**
     * Validate the input fields
     *
     * @param reasonWhy The reason why text to validate
     * @return true if input is valid, false otherwise
     */
    private boolean validateInput(String reasonWhy) {
        // Check if reasonWhy is empty
        if (reasonWhy.isEmpty()) {
            reasonWhyInput.setError("Reason why is required.");
            reasonWhyInput.requestFocus();
            return false;
        }

        // Validate character count
        if (reasonWhy.length() > 20) {
            reasonWhyInput.setError("Reason why must be 20 characters or less.");
            reasonWhyInput.requestFocus();
            return false;
        }

        // Validate word count
        String[] words = reasonWhy.split("\\s+");
        if (words.length > 3) {
            reasonWhyInput.setError("Reason why must be 3 words or less.");
            reasonWhyInput.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Applies a gradient background to the main container using the base mood color.
     *
     * @param baseColor The base mood color.
     */
    private void applyGradientBackground(int baseColor) {
        int lighterColor = blendColors(baseColor, Color.WHITE, 0.7f);
        int mediumColor = blendColors(baseColor, Color.WHITE, 0.3f);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#2D2D3A"), lighterColor, mediumColor, baseColor}
        );
        gradient.setCornerRadius(0f);
        TransitionManager.beginDelayedTransition(mainContainer);
        mainContainer.setBackground(gradient);
    }

    /**
     * Updates the intensity display based on the slider progress
     *
     * @param progress The current intensity value (0-10)
     */
    private void updateIntensityDisplay(int progress) {
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
     * Updates the mood text based on the intensity level
     *
     * @param intensity The current intensity value (0-10)
     */
    private void updateMoodTextBasedOnIntensity(int intensity) {
        if (intensity <= 3) {
            selectedMoodText.setText("Slightly " + selectedMood);
        } else if (intensity <= 7) {
            selectedMoodText.setText(selectedMood);
        } else {
            selectedMoodText.setText("Very " + selectedMood);
        }
    }

    /**
     * Animate a pulse effect on the mood container
     */
    private void animateMoodContainerPulse() {
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
        pulseSet.start();
    }

    /**
     * Applies UI changes based on the selected mood intensity.
     *
     * @param progress The current intensity value (0-10).
     */
    private void applyIntensityEffects(int progress) {
        // Update intensity display text
        updateIntensityDisplay(progress);

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
        updateMoodTextBasedOnIntensity(progress);
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
            return blendColors(baseColor, Color.GRAY, blendRatio);
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
                        imageHintText.setVisibility(View.VISIBLE);
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
                    photoSize = sizeKB;
                    currentBitmap = bitmap;
                    currentImageUri = uri.toString();
                    imgPlaceholder.setVisibility(View.GONE);
                    imageHintText.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    photoSize = sizeKB;
                    currentBitmap = bitmap;
                    currentImageUri = uri.toString();
                    imgPlaceholder.setVisibility(View.GONE);
                    imageHintText.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            }
        }
    }

    /**
     * Converts dp (density-independent pixels) to actual pixel units.
     *
     * @param dp The dp value.
     * @return The equivalent pixel value.
     */
    private int dpToPx(float dp) {
        return Math.round(getResources().getDisplayMetrics().density * dp);
    }

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