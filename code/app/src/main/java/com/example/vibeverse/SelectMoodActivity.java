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
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
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
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    private TextView selectedMoodEmoji, selectedMoodText, intensityDisplay;
    private SeekBar moodIntensitySlider;
    private EditText reasonWhyInput;
    private Spinner socialSituationInput;
    private Button continueButton;
    private ImageView backButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer;
    private TextView imageHintText;

    private static final int REQUEST_LOCATION_AUTOCOMPLETE = 3;
    private FrameLayout locationButton;
    private TextView selectedLocationText;
    private String selectedLocationName = null;
    private LatLng selectedLocationCoords = null;

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
    private long photoSize;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder, imgSelected;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mood);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        // Initialize UI references
        initializeUIElements();

        // Initialize Firebase8
        initializeFirebase();

        // Initialize mood data (colors and emojis)
        initializeMoodData();

        // Create mood buttons in the grid
        createMoodButtons();

        // Setup UI event listeners
        setupEventListeners();

        // Set the initial mood
        selectMood(selectedMood);
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
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backArrow);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);
        imageHintText = findViewById(R.id.imageHintText);
        locationButton = findViewById(R.id.btnLocation);
        selectedLocationText = findViewById(R.id.selectedLocationText);
    }

    /**
     * Initialize Firebase Auth and Firestore
     */
    private void initializeFirebase() {
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
    }

    /**
     * Initialize mood colors and emojis
     */
    private void initializeMoodData() {
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink

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
     * Create mood buttons within the GridLayout
     */
    private void createMoodButtons() {
        GridLayout moodGrid = findViewById(R.id.moodGrid);
        moodGrid.removeAllViews();

        for (String mood : moodEmojis.keySet()) {
            MaterialCardView cardView = new MaterialCardView(this);
            cardView.setCardElevation(dpToPx(2));
            cardView.setRadius(dpToPx(12));
            cardView.setCardBackgroundColor(moodColors.get(mood));
            cardView.setStrokeWidth(0);
            cardView.setUseCompatPadding(true);

            LinearLayout buttonContent = new LinearLayout(this);
            buttonContent.setOrientation(LinearLayout.VERTICAL);
            buttonContent.setGravity(Gravity.CENTER);
            buttonContent.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

            TextView emojiView = new TextView(this);
            emojiView.setText(moodEmojis.get(mood));
            emojiView.setTextSize(32);
            emojiView.setGravity(Gravity.CENTER);

            TextView moodNameView = new TextView(this);
            moodNameView.setText(mood);
            moodNameView.setTextSize(12);
            moodNameView.setTextColor(Color.WHITE);
            moodNameView.setGravity(Gravity.CENTER);
            moodNameView.setTypeface(null, android.graphics.Typeface.BOLD);

            buttonContent.addView(emojiView);
            buttonContent.addView(moodNameView);
            cardView.addView(buttonContent);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));
            cardView.setLayoutParams(params);

            final String moodName = mood;
            cardView.setOnClickListener(v -> selectMood(moodName));

            moodGrid.addView(cardView);
        }
    }

    /**
     * Setup event listeners for user interactions
     */
    private void setupEventListeners() {
        // Set initial intensity display
        updateIntensityDisplay(moodIntensitySlider.getProgress());

        // Mood intensity slider listener
        moodIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateIntensityDisplay(progress);
                float emojiScale = 0.7f + (progress / 10f * 0.6f); // Scale from 0.7 to 1.3
                selectedMoodEmoji.setScaleX(emojiScale);
                selectedMoodEmoji.setScaleY(emojiScale);
                int adjustedColor = adjustColorIntensity(selectedColor, progress);
                updateMoodContainerColor(adjustedColor);
                updateMoodTextBasedOnIntensity(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No action needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                animateMoodContainerPulse();
            }
        });

        // Image button listener
        findViewById(R.id.btnImage).setOnClickListener(v -> showImagePickerDialog());

        // Back button listener
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
            startActivity(intent);
            finish();
        });

        locationButton.setOnClickListener(v -> startPlacesAutocomplete());

        // Continue button listener
        continueButton.setOnClickListener(v -> {
            mainContainer.animate()
                    .alpha(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {
                        // Validate input
                        if (validateInput()) {
                            // Create and save mood event
                            saveMoodEvent();
                        } else {
                            // Restore opacity if validation fails
                            mainContainer.animate().alpha(1f).setDuration(200).start();
                        }
                    })
                    .start();
        });
    }

    /**
     * Validate the user input before saving
     */
    private boolean validateInput() {
        String reasonWhy = reasonWhyInput.getText().toString().trim();

        // Check if reasonWhy is empty
        if (reasonWhy.isEmpty()) {
            reasonWhyInput.setError("Reason why is required.");
            reasonWhyInput.requestFocus();
            return false;
        }

        // Error handling for reasonWhy length
        if (reasonWhy.length() > 20) {
            reasonWhyInput.setError("Reason why must be 20 characters or less.");
            reasonWhyInput.requestFocus();
            return false;
        }

        // Error handling for reasonWhy word count
        String[] words = reasonWhy.split("\\s+");
        if (words.length > 3) {
            reasonWhyInput.setError("Reason why must be 3 words or less.");
            reasonWhyInput.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * Create mood event and save to Firestore
     */
    private void saveMoodEvent() {
        String socialSituation = socialSituationInput.getSelectedItem().toString().trim();
        String reasonWhy = reasonWhyInput.getText().toString().trim();
        int intensity = moodIntensitySlider.getProgress();

        // Create the mood event
        MoodEvent moodEvent;
        if (imageUri != null && currentBitmap != null) {
            // If an image is selected, create a Photograph instance
            Photograph photograph = new Photograph(
                    imageUri,
                    photoSize,
                    currentBitmap,
                    new Date(),
                    "VibeVerse Location" // Default location
            );
            moodEvent = new MoodEvent(selectedMood, selectedEmoji, reasonWhy, "", socialSituation, photograph);
        } else {
            moodEvent = new MoodEvent(selectedMood, selectedEmoji, reasonWhy, "", socialSituation);
        }

        // Set intensity
        moodEvent.setIntensity(intensity);

        // Save to Firestore
        saveMoodToFirestore(moodEvent);
    }

    /**
     * Updates the visual intensity display based on slider progress
     *
     * @param progress The current slider progress (0-10)
     */
    private void updateIntensityDisplay(int progress) {
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
     * Updates the mood container background color
     *
     * @param color The color to set for the container background
     */
    private void updateMoodContainerColor(int color) {
        GradientDrawable moodContainerBg = (GradientDrawable) selectedMoodContainer.getBackground();
        moodContainerBg.setColor(color);
    }

    /**
     * Adjusts the color intensity based on slider progress
     *
     * @param baseColor The original mood color
     * @param intensity The intensity value (0-10)
     * @return The adjusted color
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
     * Adjusts the saturation of a color
     *
     * @param color  The original color
     * @param factor The factor to adjust saturation by (>1 for more saturation, <1 for less)
     * @return The adjusted color
     */
    private int adjustColorSaturation(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] * factor);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * (factor > 1 ? 0.9f : 1.1f)));
        return Color.HSVToColor(hsv);
    }

    /**
     * Animates the mood container with a pulse effect
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
     * Updates the mood text based on intensity
     *
     * @param intensity The current slider progress (0-10)
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
     * Updates the UI when a mood is selected
     *
     * @param mood The mood selected by the user
     */
    private void selectMood(String mood) {
        selectedMood = mood;
        selectedEmoji = moodEmojis.get(mood);
        selectedColor = moodColors.get(mood);

        selectedMoodContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            selectedMoodText.setText(mood);
            selectedMoodEmoji.setText(selectedEmoji);

            updateMoodContainerColor(selectedColor);

            selectedMoodContainer.animate().alpha(1f).setDuration(300).start();
        }).start();

        applyGradientBackground(selectedColor);
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));

        // Update display based on current intensity
        int intensity = moodIntensitySlider.getProgress();
        updateIntensityDisplay(intensity);
        updateMoodTextBasedOnIntensity(intensity);
    }

    /**
     * Applies a gradient background to the main container
     *
     * @param baseColor The base mood color
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
     * Displays a dialog allowing the user to choose between taking a photo,
     * selecting one from the gallery, or removing the current photo
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
                        imageUri = null;
                        imgSelected.setVisibility(View.GONE);
                        imgPlaceholder.setVisibility(View.VISIBLE);
                        imageHintText.setVisibility(View.VISIBLE);
                    }
                })
                .show();
    }

    /**
     * Dispatches an intent to capture an image using the device camera
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
     * Dispatches an intent to pick an image from the gallery
     */
    private void dispatchPickImageIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
    }

    /**
     * Requests necessary permissions for camera and storage access
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
     * Called when permission requests complete
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startPlacesAutocomplete() {
        // Define the place fields to return
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

        // Start the autocomplete intent
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, REQUEST_LOCATION_AUTOCOMPLETE);
    }

    /**
     * Handles results from camera or gallery intents
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                ImageUtils.processImage(this, imageUri, (bitmap, downloadUrl, sizeKB) -> {
                    photoSize = sizeKB;
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
                    photoSize = sizeKB;
                    currentBitmap = bitmap;
                    imgPlaceholder.setVisibility(View.GONE);
                    imageHintText.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                    imageUri = downloadUrl;
                });
            } else if (requestCode == REQUEST_LOCATION_AUTOCOMPLETE) {
                // Handle location selection
                Place place = Autocomplete.getPlaceFromIntent(data);
                selectedLocationName = place.getName() + ", " + place.getAddress();
                selectedLocationCoords = place.getLatLng();
                selectedLocationText.setText(selectedLocationName);
                selectedLocationText.setTextColor(Color.BLACK);
                Toast.makeText(this, "Location selected: " + selectedLocationName, Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            if (requestCode == REQUEST_LOCATION_AUTOCOMPLETE) {
                // Handle the error
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("SelectMoodActivity", "Place selection error: " + status.getStatusMessage());
                Toast.makeText(this, "Error selecting location", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Saves the provided MoodEvent to Firestore
     *
     * @param moodEvent The MoodEvent to save
     */
    private void saveMoodToFirestore(MoodEvent moodEvent) {
        // Show a loading indicator
        Toast.makeText(this, "Saving your mood...", Toast.LENGTH_SHORT).show();

        // Convert MoodEvent to Map for Firestore
        Map<String, Object> moodData = new HashMap<>();
        moodData.put("emoji", moodEvent.getEmoji());
        moodData.put("mood", moodEvent.getMoodTitle());
        moodData.put("socialSituation", moodEvent.getSocialSituation());
        moodData.put("timestamp", moodEvent.getTimestamp());
        moodData.put("intensity", moodEvent.getIntensity());
        moodData.put("reasonWhy", moodEvent.getReasonWhy());

        if (selectedLocationName != null && selectedLocationCoords != null) {
            moodData.put("moodLocation", selectedLocationName);
            moodData.put("moodLatitude", selectedLocationCoords.latitude);
            moodData.put("moodLongitude", selectedLocationCoords.longitude);
        }

        // Handle photograph if present
        if (moodEvent.getPhotograph() != null) {
            moodData.put("hasPhoto", true);
            moodData.put("photoUri", moodEvent.getPhotoUri());
            moodData.put("photoDateTaken", moodEvent.getPhotograph().getDateTaken().getTime());
            moodData.put("photoLocation", moodEvent.getPhotograph().getLocation());
            moodData.put("photoSize", moodEvent.getPhotograph().getFileSize());
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
     * Converts dp to pixels
     *
     * @param dp The value in dp
     * @return The value in pixels
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    /**
     * Utility class for color manipulation
     */
    private static class ColorUtils {
        /**
         * Blends two colors together using the specified ratio
         *
         * @param color1 The first color
         * @param color2 The second color
         * @param ratio  The blending ratio (0.0 to 1.0)
         * @return The resulting blended color
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