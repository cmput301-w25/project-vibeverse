package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
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
    private EditText triggerInput, socialSituationInput;
    private Button continueButton;
    private View selectedMoodContainer;
    private LinearLayout mainContainer; // Container for gradient background and transitions

    // Mood properties
    private String selectedMood = "Angry"; // Default mood
    private String selectedEmoji = "😡";
    private int selectedColor = Color.RED;
    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    // Image handling constants and fields
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder, imgSelected;

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
        socialSituationInput = findViewById(R.id.socialSituationInput);
        continueButton = findViewById(R.id.continueButton);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);

        // Enable layout transitions for smooth updates
        mainContainer.setLayoutTransition(new android.animation.LayoutTransition());

        // Set continue button text from string resource
        continueButton.setText(getString(R.string.continue_text));

        // Initialize mood colors and emojis
        initializeMoodData();

        // Build the mood selection grid with a polished, uniform design
        GridLayout moodGrid = findViewById(R.id.moodGrid);
        moodGrid.setColumnCount(4);
        moodGrid.setRowCount(2);
        createMoodButtons(moodGrid);

        // Set up image picker button click listener
        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setOnClickListener(v -> showImagePickerDialog());

        // Set up continue button to create a MoodEvent and pass it to MainActivity
        continueButton.setOnClickListener(v -> {
            String trigger = triggerInput.getText().toString().trim();
            String socialSituation = socialSituationInput.getText().toString().trim();
            MoodEvent moodEvent;
            if (imageUri != null) {
                // If an image is selected, create a Photograph instance and attach it to the mood event
                Photograph photograph = new Photograph(imageUri, 0, new Date(), "Test Location");
                moodEvent = new MoodEvent(selectedEmoji + " " + selectedMood, trigger, socialSituation, photograph);
            } else {
                moodEvent = new MoodEvent(selectedEmoji + " " + selectedMood, trigger, socialSituation);
            }
            Intent intent = new Intent(SelectMoodActivity.this, MainActivity.class);
            intent.putExtra("moodEvent", moodEvent);
            startActivity(intent);
        });
    }

    /**
     * Creates the mood selection buttons within the provided GridLayout.
     * Each button is styled uniformly with rounded corners, elevation, and a consistent margin.
     *
     * @param moodGrid The GridLayout where mood buttons will be added.
     */
    private void createMoodButtons(GridLayout moodGrid) {
        int padding = dpToPx(12);

        for (String mood : moodEmojis.keySet()) {
            MaterialButton moodButton = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            // Set the button's emoji text and size
            moodButton.setText(moodEmojis.get(mood));
            moodButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f);
            // Apply rounded corners and padding
            moodButton.setCornerRadius(dpToPx(12));
            moodButton.setPadding(padding, padding, padding, padding);
            // Set elevation and stroke for a card-like appearance
            moodButton.setElevation(dpToPx(2));
            moodButton.setStrokeWidth(dpToPx(1));
            moodButton.setStrokeColor(ColorStateList.valueOf(Color.WHITE));
            // Set background tint using ColorStateList.valueOf to apply raw integer color
            int colorInt = moodColors.get(mood);
            moodButton.setBackgroundTintList(ColorStateList.valueOf(colorInt));
            // Set click listener to update the selection
            moodButton.setOnClickListener(v -> selectMood(mood));
            // Set layout parameters to evenly distribute the buttons
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; // expand with weight
            params.height = dpToPx(70);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(dpToPx(6), dpToPx(6), dpToPx(6), dpToPx(6));
            moodButton.setLayoutParams(params);
            // Add the button to the grid
            moodGrid.addView(moodButton);
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
     * The "Happy" color has been slightly darkened to improve readability.
     */
    private void initializeMoodData() {
        // Define mood colors; starting gradient from a neutral top color will be handled later
        moodColors.put("Angry", Color.parseColor("#E53935"));      // Strong red
        moodColors.put("Confused", Color.parseColor("#5C6BC0"));   // Indigo
        moodColors.put("Disgusted", Color.parseColor("#43A047"));  // Vibrant green
        moodColors.put("Afraid", Color.parseColor("#1E88E5"));     // Deep blue
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Slightly darker yellow
        moodColors.put("Sad", Color.parseColor("#1E88E5"));        // Blue variant
        moodColors.put("Shameful", Color.parseColor("#C2185B"));   // Deep pink
        moodColors.put("Surprised", Color.parseColor("#FB8C00"));  // Vivid orange

        // Define mood emojis
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Shameful", "😳");
        moodEmojis.put("Surprised", "😲");
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

        selectedMoodText.setText(mood);
        selectedMoodEmoji.setText(selectedEmoji);
        selectedMoodContainer.setBackgroundColor(selectedColor);

        // Apply a gradient background with a smooth transition
        applyGradientBackground(selectedColor);
    }

    /**
     * Applies a gradient background to the main container.
     * The gradient starts with a neutral color (#FAFAFA) at the top to ensure readability, then transitions through lighter and darker shades of the mood color.
     *
     * @param baseColor The base mood color.
     */
    private void applyGradientBackground(int baseColor) {
        int lighterColor = adjustColorBrightness(baseColor, 1.5f);
        int darkerColor = adjustColorBrightness(baseColor, 0.8f);

        // Gradient: neutral top (#FAFAFA), then lighter, base, and darker colors.
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#FAFAFA"), lighterColor, baseColor, darkerColor}
        );
        gradient.setCornerRadius(0f);

        // Animate the background transition for a polished effect
        TransitionManager.beginDelayedTransition(mainContainer);
        mainContainer.setBackground(gradient);
    }

    /**
     * Adjusts the brightness of a given color by a specified factor.
     *
     * @param color  The original color.
     * @param factor The factor to multiply each RGB component (e.g., >1 for brighter, <1 for darker).
     * @return The adjusted color.
     */
    private int adjustColorBrightness(int color, float factor) {
        int r = Math.min(255, (int)(Color.red(color) * factor));
        int g = Math.min(255, (int)(Color.green(color) * factor));
        int b = Math.min(255, (int)(Color.blue(color) * factor));
        return Color.rgb(r, g, b);
    }

    /**
     * Displays a dialog allowing the user to choose between taking a photo or selecting one from the gallery.
     */
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            dispatchTakePictureIntent();
                        } else {
                            dispatchPickImageIntent();
                        }
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
                ImageUtils.processImage(this, imageUri, (bitmap, uri) -> {
                    currentBitmap = bitmap;
                    imgPlaceholder.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, (bitmap, uri) -> {
                    currentBitmap = bitmap;
                    imgPlaceholder.setVisibility(View.GONE);
                    imgSelected.setVisibility(View.VISIBLE);
                    imgSelected.setImageBitmap(bitmap);
                });
            }
        }
    }
}
