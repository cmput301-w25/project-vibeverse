package com.example.vibeverse;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import com.bumptech.glide.Glide;

/**
 * EditMoodActivity allows users to update an existing mood event.
 * <p>
 * This activity uses the same layout as SelectMoodActivity for consistency.
 * It receives mood details from the calling activity (e.g., MainActivity),
 * displays the current values, and allows the user to update the mood,
 * trigger, social situation, and an optional image. When the user clicks
 * "Update Mood", the updated details are sent back to the caller.
 * </p>
 */
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
    private ImageView imgSelected, imgPlaceholder;

    private String currentImageUri;

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;
    private Bitmap currentBitmap;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the UI components, loads mood colors and emojis,
     * sets the current mood values from the intent, applies the gradient background,
     * loads an image if available, and sets click listeners for updating mood and picking images.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
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
        imgSelected = findViewById(R.id.imgSelected);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
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
        String currentPhotoUri = intent.getStringExtra("photoUri");


        Log.d("EditMoodActivity", "CurrentPhotoUri: " + currentPhotoUri);


        // Set UI values
        selectedMoodText.setText(selectedMood);
        selectedMoodEmoji.setText(selectedEmoji);
        triggerInput.setText(trigger);
        socialSituationInput.setText(socialSituation);

        // Apply Gradient Background
        applyGradientBackground(selectedColor);

        currentImageUri = currentPhotoUri;
        // Load the photo if available (using an image loading library like Glide)
        if (currentPhotoUri != null && !currentPhotoUri.equals("N/A")) {
            // Make sure to add the Glide dependency in your build.gradle file
            Glide.with(this)
                    .load(currentPhotoUri)
                    .into(imgSelected);
            imgSelected.setVisibility(View.VISIBLE);
        }


        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        // Update button click event
        updateButton.setOnClickListener(view -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("updatedMood", selectedMood);
            resultIntent.putExtra("updatedEmoji", selectedEmoji);
            resultIntent.putExtra("updatedTrigger", triggerInput.getText().toString().trim());
            resultIntent.putExtra("updatedSocialSituation", socialSituationInput.getText().toString().trim());
            resultIntent.putExtra("timestamp", new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date()));
            resultIntent.putExtra("moodPosition", moodPosition);

            resultIntent.putExtra("updatedPhotoUri", (currentImageUri != null) ? currentImageUri : "N/A");

            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /**
     * Applies a gradient background to the main container based on the given base color.
     * The gradient transitions from the base color to a darker shade.
     *
     * @param baseColor The base color for the gradient.
     */
    private void applyGradientBackground(int baseColor) {
        int darkerColor = darkenColor(baseColor, 0.7f); // 70% darker shade

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{baseColor, darkerColor}
        );
        mainContainer.setBackground(gradientDrawable);
    }

    /**
     * Darkens the given color by the specified factor.
     *
     * @param color  The original color.
     * @param factor The factor to darken the color (e.g., 0.7 for 70% brightness).
     * @return The darkened color.
     */
    private int darkenColor(int color, float factor) {
        int r = Math.round(Color.red(color) * factor);
        int g = Math.round(Color.green(color) * factor);
        int b = Math.round(Color.blue(color) * factor);
        return Color.rgb(r, g, b);
    }

    /**
     * Initializes the mood colors.
     * <p>
     * These colors are used to style the UI based on the selected mood.
     * </p>
     */
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

    /**
     * Initializes the mood emojis.
     * <p>
     * Each mood is associated with a specific emoji.
     * </p>
     */
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

    /**
     * Handles the result from camera or gallery intents.
     * <p>
     * Processes the selected image using ImageUtils and updates the UI.
     * </p>
     *
     * @param requestCode The request code identifying the image action.
     * @param resultCode  The result code returned by the child activity.
     * @param data        The Intent data returned.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // For camera, imageUri is already set
                ImageUtils.processImage(this, imageUri, new ImageUtils.ImageProcessCallback() {
                    @Override
                    public void onImageConfirmed(Bitmap bitmap, Uri uri) {
                        currentBitmap = bitmap;
                        currentImageUri = uri.toString();
                        imgPlaceholder.setVisibility(View.GONE);
                        imgSelected.setVisibility(View.VISIBLE);
                        imgSelected.setImageBitmap(bitmap);
                    }
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, new ImageUtils.ImageProcessCallback() {
                    @Override
                    public void onImageConfirmed(Bitmap bitmap, Uri uri) {
                        currentBitmap = bitmap;
                        imgPlaceholder.setVisibility(View.GONE);
                        imgSelected.setVisibility(View.VISIBLE);
                        imgSelected.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }

    /**
     * Displays a dialog for the user to select an image source (camera or gallery).
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
     * Creates a temporary file for the image and requests necessary permissions.
     */
    private void dispatchTakePictureIntent() {
        requestPermissions();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
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
     * Requests the necessary permissions (Camera, Read & Write External Storage) at runtime.
     */
    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    /**
     * Callback for the result from permission requests.
     *
     * @param requestCode  The request code passed in requestPermissions().
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
}