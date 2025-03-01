package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;
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

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder, imgSelected;

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

        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);
        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });

        // Continue Button Click - Send Mood to MainActivity
        continueButton.setOnClickListener(view -> {
            String trigger = triggerInput.getText().toString().trim();
            String socialSituation = socialSituationInput.getText().toString().trim();
            MoodEvent moodEvent;

            // If the user has selected an image, create a Photograph instance
            if (imageUri != null) {
                Photograph photograph = new Photograph(imageUri, 0, new Date(), "Test Location");
                moodEvent = new MoodEvent(selectedMood, trigger, socialSituation, photograph);
            } else {
                moodEvent = new MoodEvent(selectedMood, trigger, socialSituation);
            }

            // Pass the MoodEvent via the Intent
            Intent intent = new Intent(SelectMoodActivity.this, MainActivity.class);
            intent.putExtra("moodEvent", moodEvent);
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

    private void dispatchPickImageIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
    }

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
