package com.example.vibeverse;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.bumptech.glide.Glide;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * EditMoodActivity allows users to update an existing mood event.
 * <p>
 * This activity uses the same layout as SelectMoodActivity for consistency.
 * It receives mood details from the calling activity (e.g., MainActivity),
 * displays the current values, and allows the user to update the mood,
 * social situation, intensity, and an optional image.
 * When the user clicks "Update Mood", the updated details are sent back to the caller.
 * </p>
 */
public class EditMoodActivity extends AppCompatActivity {

    // UI Elements
    private TextView selectedMoodEmoji, selectedMoodText;
    private EditText reasonWhyInput;
    private Spinner socialSituationInput;
    private SeekBar moodIntensitySlider;
    private Button updateButton;

    private ImageView backButton;

    private androidx.appcompat.widget.SwitchCompat visibilitySwitch;
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

    private long photoSize;
    private String photoDateTaken;
    private String photoLocation;
    private Bitmap currentBitmap;

    private boolean isPublic;

    // Location-related fields
    private static final int REQUEST_LOCATION_AUTOCOMPLETE = 3;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private FrameLayout locationButton;
    private TextView selectedLocationText;
    private String selectedLocationName = null;
    private LatLng selectedLocationCoords = null;
    private LocationManager locationManager;
    private Location currentLocation;
    private String currentLocationAddress = null;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String selectedTheme, userId;

    private boolean userRemovedLocation = false;

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

        // Initialize Places API if needed
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        updateButton = findViewById(R.id.continueButton); // Reuse the same button ID
        backButton = findViewById(R.id.backArrow);
        imgSelected = findViewById(R.id.imgSelected);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        visibilitySwitch = findViewById(R.id.visibilitySwitch);

        // Initialize location-related UI elements
        locationButton = findViewById(R.id.btnLocation);
        selectedLocationText = findViewById(R.id.selectedLocationText);

        // Request location permissions and get current location
        requestLocationPermission();
        getCurrentLocation();

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID or device ID
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }

        // Pull the user's selected theme from Firestore and then initialize the rest
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("selectedTheme")) {
                        selectedTheme = documentSnapshot.getString("selectedTheme");
                    } else {
                        selectedTheme = "clown"; // Fallback default
                    }
                    // Continue with the UI initialization after theme is loaded
                    initializeUI();
                })
                .addOnFailureListener(e -> {
                    // In case of error, fallback to default and initialize UI
                    selectedTheme = "clown";
                    initializeUI();
                });
    }

    /**
     * Continues initializing the activity's UI components after the user's theme is set.
     */
    private void initializeUI() {
        // Create a custom toolbar without a title
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle(""); // Remove the "Edit Mood" text
        toolbar.setBackgroundColor(Color.TRANSPARENT);
        toolbar.setNavigationIcon(getResources().getIdentifier("abc_ic_ab_back_material", "drawable", getPackageName()));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        mainContainer.addView(toolbar, 0);

        // Check for existing title text and add one if needed
        boolean textAlreadyExists = false;
        for (int i = 0; i < mainContainer.getChildCount(); i++) {
            View child = mainContainer.getChildAt(i);
            if (child instanceof TextView) {
                TextView textView = (TextView) child;
                if (textView.getText().toString().contains("Choose how you're feeling")) {
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
                    textView.setTypeface(null, Typeface.BOLD);
                    textAlreadyExists = true;
                    break;
                }
            }
        }
        if (!textAlreadyExists) {
            TextView chooseTextView = new TextView(this);
            chooseTextView.setText("Choose how you're feeling right now");
            chooseTextView.setTextColor(Color.WHITE);
            chooseTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
            chooseTextView.setTypeface(null, Typeface.BOLD);
            chooseTextView.setGravity(Gravity.CENTER);
            chooseTextView.setPadding(0, dpToPx(16), 0, dpToPx(20));
            mainContainer.addView(chooseTextView, 1);
        }

        // Update button text and styling
        updateButton.setText("Update Mood");
        updateButton.setBackgroundTintList(null);

        // Set consistent typeface and text sizes
        selectedMoodText.setTypeface(null, Typeface.BOLD);
        // Instead of using a string emoji with setTextSize, we now use PNG drawable
        selectedMoodEmoji.setTextSize(TypedValue.COMPLEX_UNIT_SP, 64);
        selectedMoodText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        // Create styling for input fields
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setCornerRadius(dpToPx(8));
        inputBg.setColor(Color.WHITE);
        inputBg.setStroke(1, Color.parseColor("#E0E0E0"));
        GradientDrawable socialBg = (GradientDrawable) inputBg.getConstantState().newDrawable().mutate();
        GradientDrawable reasonWhyBg = (GradientDrawable) inputBg.getConstantState().newDrawable().mutate();
        socialSituationInput.setBackground(socialBg);
        reasonWhyInput.setBackground(reasonWhyBg);
        int paddingPx = (int) dpToPx(12);
        socialSituationInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
        reasonWhyInput.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);

        // Add labels above input fields
        TextView socialLabel = new TextView(this);
        socialLabel.setText("Social situation");
        socialLabel.setTextColor(Color.WHITE);
        socialLabel.setTypeface(null, Typeface.BOLD);
        socialLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        socialLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));
        TextView reasonWhyLabel = new TextView(this);
        reasonWhyLabel.setText("Reason why you feel this way");
        reasonWhyLabel.setTextColor(Color.WHITE);
        reasonWhyLabel.setTypeface(null, Typeface.BOLD);
        reasonWhyLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        reasonWhyLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));
        int reasonWhyIndex = mainContainer.indexOfChild(reasonWhyInput);
        mainContainer.addView(reasonWhyLabel, reasonWhyIndex);
        int socialIndex = mainContainer.indexOfChild(socialSituationInput);
        mainContainer.addView(socialLabel, socialIndex);

        // Setup mood intensity slider and image picker button
        setupMoodIntensitySlider();
        GradientDrawable imageBtnBg = new GradientDrawable();
        imageBtnBg.setCornerRadius(dpToPx(12));
        imageBtnBg.setColor(Color.WHITE);
        imageBtnBg.setStroke(1, Color.parseColor("#E0E0E0"));
        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setBackground(imageBtnBg);
        imgPlaceholder.setColorFilter(Color.parseColor("#AAAAAA"));
        TextView imageHintText = new TextView(this);
        imageHintText.setText("Add an image (optional)");
        imageHintText.setTextColor(Color.parseColor("#757575"));
        imageHintText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        imageHintText.setGravity(Gravity.CENTER);
        btnTestImage.addView(imageHintText);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.bottomMargin = (int) dpToPx(20);
        imageHintText.setLayoutParams(params);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnTestImage.setForeground(getDrawable(android.R.drawable.list_selector_background));
        }

        // Initialize mood colors and emojis
        initializeMoodColors();
        initializeMoodEmojis();

        // Retrieve mood info from the Intent
        Intent intent = getIntent();
        selectedMood = intent.getStringExtra("selectedMood");
        selectedEmoji = intent.getStringExtra("selectedEmoji");
        selectedColor = moodColors.getOrDefault(selectedMood, Color.GRAY);
        moodPosition = intent.getIntExtra("moodPosition", -1);
        isPublic = intent.getBooleanExtra("isPublic", false);
        String timestamp = intent.getStringExtra("timestamp");
        String reasonWhy = intent.getStringExtra("reasonWhy");
        String socialSituation = intent.getStringExtra("socialSituation");
        String currentPhotoUri = intent.getStringExtra("photoUri");

        // Retrieve and set location information if available
        if (intent.hasExtra("moodLocation")) {
            selectedLocationName = intent.getStringExtra("moodLocation");
            double latitude = intent.getDoubleExtra("moodLatitude", 0);
            double longitude = intent.getDoubleExtra("moodLongitude", 0);
            if (latitude != 0 || longitude != 0) {
                selectedLocationCoords = new LatLng(latitude, longitude);
            }
            if (selectedLocationName != null && !selectedLocationName.isEmpty()) {
                selectedLocationText.setText(selectedLocationName);
                selectedLocationText.setTextColor(Color.BLACK);
            }
        }

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.social_situation_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        socialSituationInput.setAdapter(adapter);

        photoDateTaken = intent.getStringExtra("photoDateTaken");
        photoLocation = intent.getStringExtra("photoLocation");
        photoSize = intent.getLongExtra("photoSizeKB", 0);

        // Set the intensity value from the Intent (default 5 if not provided)
        int intensity = intent.getIntExtra("intensity", 5);
        moodIntensitySlider.setProgress(intensity);

        if (intensityDisplay != null) {
            StringBuilder intensityBuilder = new StringBuilder();
            for (int i = 0; i <= 10; i++) {
                intensityBuilder.append(i <= intensity ? "●" : "○");
            }
            intensityDisplay.setText(intensityBuilder.toString());
        }

        // Adjust emoji scale based on intensity.
        float emojiScale = 0.7f + (intensity / 10f * 0.6f);
        selectedMoodEmoji.setScaleX(emojiScale);
        selectedMoodEmoji.setScaleY(emojiScale);

        // Update mood text based on intensity.
        if (intensity <= 3) {
            selectedMoodText.setText("Slightly " + selectedMood);
        } else if (intensity <= 7) {
            selectedMoodText.setText(selectedMood);
        } else {
            selectedMoodText.setText("Very " + selectedMood);
        }

        // IMPORTANT: Update the emoji drawable using the intensity-adjusted scale.
        updateSelectedMoodEmoji();

        // Update UI fields with the retrieved values
        selectedMoodText.setText(selectedMood);
        reasonWhyInput.setText(reasonWhy);
        if (socialSituation != null) {
            int spinnerPosition = adapter.getPosition(socialSituation);
            socialSituationInput.setSelection(spinnerPosition);
        }
        visibilitySwitch.setChecked(isPublic);

        // Apply the refined gradient background
        applyGradientBackground(selectedColor);
        GradientDrawable moodContainerBg = new GradientDrawable();
        moodContainerBg.setColor(selectedColor);
        moodContainerBg.setCornerRadius(dpToPx(12));
        selectedMoodContainer.setBackground(moodContainerBg);

        // Style the update button
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setCornerRadius(dpToPx(24));
        buttonBg.setColor(Color.parseColor("#5C4B99"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            updateButton.setElevation(dpToPx(4));
        }
        updateButton.setBackground(buttonBg);
        updateButton.setPadding((int) dpToPx(24), (int) dpToPx(12), (int) dpToPx(24), (int) dpToPx(12));
        updateButton.setTextColor(Color.WHITE);
        updateButton.setTypeface(null, Typeface.BOLD);

        // Load existing photo if available
        currentImageUri = currentPhotoUri;
        if (currentImageUri != null && !currentImageUri.equals("N/A")) {
            Glide.with(this).load(currentImageUri).into(imgSelected);
            imgSelected.setVisibility(View.VISIBLE);
            imageHintText.setVisibility(View.GONE);
            imgPlaceholder.setVisibility(View.GONE);
        }

        // Fade-in animation for the mood container
        selectedMoodContainer.setAlpha(0f);
        selectedMoodContainer.animate().alpha(1f).setDuration(300).start();

        // Set click listeners for image picker and location button
        btnTestImage.setOnClickListener(v -> showImagePickerDialog());
        locationButton.setOnClickListener(v -> {
            if (selectedLocationName != null && !selectedLocationName.isEmpty()) {
                // Location already exists for this mood
                List<LocationOption> options = new ArrayList<>();

                options.add(new LocationOption(
                        "Keep existing: " + selectedLocationName,
                        android.R.drawable.ic_menu_mylocation,
                        () -> {
                            // Keep existing location - no action needed
                        }
                ));

                options.add(new LocationOption(
                        "Search for a different location",
                        android.R.drawable.ic_menu_search,
                        this::startPlacesAutocomplete
                ));

                options.add(new LocationOption(
                        "Remove location",
                        android.R.drawable.ic_menu_close_clear_cancel,
                        () -> {
                            // Remove location
                            selectedLocationName = null;
                            selectedLocationCoords = null;
                            selectedLocationText.setText("Add Location (Optional)");
                            selectedLocationText.setTextColor(Color.parseColor("#757575"));
                            userRemovedLocation = true;
                        }
                ));

                showCustomLocationDialog("Choose Location", options);

            } else {
                if (currentLocationAddress != null && !currentLocationAddress.isEmpty()) {

                    // Current device location is available
                    List<LocationOption> options = new ArrayList<>();

                    options.add(new LocationOption(
                            "Use Current Location: " + currentLocationAddress,
                            android.R.drawable.ic_menu_mylocation,
                            () -> {
                                // Use current location
                                selectedLocationName = currentLocationAddress;
                                if (currentLocation != null) {
                                    selectedLocationCoords = new LatLng(
                                            currentLocation.getLatitude(),
                                            currentLocation.getLongitude());
                                }
                                selectedLocationText.setText(selectedLocationName);
                                selectedLocationText.setTextColor(Color.BLACK);
                            }
                    ));

                    options.add(new LocationOption(
                            "Search for a different location",
                            android.R.drawable.ic_menu_search,
                            this::startPlacesAutocomplete
                    ));

                    showCustomLocationDialog("Choose Location", options);
                } else {
                    startPlacesAutocomplete();
                }
            }
        });

        // Update button click listener with animation
        updateButton.setOnClickListener(view -> {
            String newReasonWhy = reasonWhyInput.getText().toString().trim();
            if (newReasonWhy.isEmpty()) {
                reasonWhyInput.setError("Reason why is required.");
                reasonWhyInput.requestFocus();
                return;
            }
            if (newReasonWhy.length() > 200) {
                reasonWhyInput.setError("Reason why must be 200 characters or less.");
                reasonWhyInput.requestFocus();
                return;
            }
            isPublic = visibilitySwitch.isChecked();
            Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show();
            mainContainer.animate()
                    .alpha(0.8f)
                    .setDuration(200)
                    .withEndAction(() -> {

                        MoodEvent updatedMoodEvent;
                        Date currentDate = new Date();
                        // Assuming selectedMood and selectedEmoji are available in the current scope.
                        String tempSocialSituation = socialSituationInput.getSelectedItem().toString().trim();
                        if (currentImageUri != null && currentBitmap != null) {
                            // Create a Photograph instance and attach it to the mood event
                            Photograph photograph = new Photograph(
                                    currentImageUri,
                                    photoSize, // Estimated file size in KB
                                    currentDate,
                                    "VibeVerse Location" // Default location or get from your location functionality
                            );
                            updatedMoodEvent = new MoodEvent(userId, selectedMood, selectedEmoji, newReasonWhy, tempSocialSituation, photograph, isPublic);
                        } else {
                            updatedMoodEvent = new MoodEvent(userId, selectedMood, selectedEmoji, newReasonWhy, tempSocialSituation, isPublic);
                        }
                        updatedMoodEvent.setIntensity(moodIntensitySlider.getProgress());
                        updatedMoodEvent.setDate(currentDate);

                        // Instantiate AchievementChecker and perform visibility and photo achievement checks
                        AchievementChecker achievementChecker = new AchievementChecker(userId);
                        if (isPublic) {
                            achievementChecker.checkAch7(updatedMoodEvent);
                        } else {
                            achievementChecker.checkAch8(updatedMoodEvent);
                        }
                        if (updatedMoodEvent.getPhotograph() != null) {
                            achievementChecker.checkAch22(updatedMoodEvent);
                        }

                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("updatedMood", selectedMood);
                        resultIntent.putExtra("updatedEmoji", selectedEmoji);
                        resultIntent.putExtra("updatedReasonWhy", reasonWhyInput.getText().toString().trim());
                        resultIntent.putExtra("updatedSocialSituation", socialSituationInput.getSelectedItem().toString().trim());
                        resultIntent.putExtra("timestamp", new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(new Date()));
                        resultIntent.putExtra("moodPosition", moodPosition);
                        resultIntent.putExtra("updatedPhotoUri", (currentImageUri != null) ? currentImageUri : "N/A");
                        resultIntent.putExtra("updatedIntensity", moodIntensitySlider.getProgress());
                        resultIntent.putExtra("updatedphotoDateTaken", photoDateTaken);
                        resultIntent.putExtra("updatedphotoLocation", photoLocation);
                        resultIntent.putExtra("updatedphotoSizeKB", photoSize);
                        resultIntent.putExtra("isPublic", isPublic);

                        if (userRemovedLocation) {
                            // Only mark as removed if user explicitly chose to remove it
                            resultIntent.putExtra("locationRemoved", true);
                        } else if (selectedLocationName != null && selectedLocationCoords != null) {
                            // User selected or kept a location
                            resultIntent.putExtra("updatedMoodLocation", selectedLocationName);
                            resultIntent.putExtra("updatedMoodLatitude", selectedLocationCoords.latitude);
                            resultIntent.putExtra("updatedMoodLongitude", selectedLocationCoords.longitude);
                        }

                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }).start();
        });

        backButton.setOnClickListener(v -> {
            Intent goBackIntent = new Intent(EditMoodActivity.this, ProfilePage.class);
            goBackIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(goBackIntent);
            finish();
        });
    }

    /**
     * Displays a custom dialog with location options.
     *
     * @param title   The title of the dialog.
     * @param options A list of LocationOption objects representing the available choices.
     */
    private void showCustomLocationDialog(String title, List<LocationOption> options) {
        // Create dialog
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_location_choice);

        // Set dialog width to 90% of screen width
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Set dialog title
        TextView dialogTitle = dialog.findViewById(R.id.dialog_title);
        dialogTitle.setText(title);

        // Get container for options
        LinearLayout optionsContainer = dialog.findViewById(R.id.location_options_container);

        // Add each option as a button
        for (LocationOption option : options) {
            View optionView = getLayoutInflater().inflate(R.layout.item_location_option, optionsContainer, false);

            ImageView optionIcon = optionView.findViewById(R.id.option_icon);
            TextView optionText = optionView.findViewById(R.id.option_text);

            // Set option text and icon
            optionIcon.setImageResource(option.iconResId);
            optionText.setText(option.text);

            // Set click listener
            optionView.setOnClickListener(v -> {
                option.clickListener.run();
                dialog.dismiss();
            });

            optionsContainer.addView(optionView);
        }

        dialog.show();
    }

    // Class to represent location options
    private static class LocationOption {
        String text;
        int iconResId;
        Runnable clickListener;

        LocationOption(String text, int iconResId, Runnable clickListener) {
            this.text = text;
            this.iconResId = iconResId;
            this.clickListener = clickListener;
        }
    }

    /**
     * Starts the Places Autocomplete activity for location selection.
     * If current location is available, it biases the search results toward the current location.
     */
    private void startPlacesAutocomplete() {
        // Define the place fields to return
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

        // Create the autocomplete intent builder
        Autocomplete.IntentBuilder intentBuilder =
                new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields);

        // If we have the current location, use it to bias the search results
        if (currentLocation != null) {
            // Create a bias around the current location (approximately 10km radius)
            double radiusDegrees = 0.1; // Roughly 10km at the equator
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            RectangularBounds bounds = RectangularBounds.newInstance(
                    new LatLng(currentLatLng.latitude - radiusDegrees, currentLatLng.longitude - radiusDegrees),
                    new LatLng(currentLatLng.latitude + radiusDegrees, currentLatLng.longitude + radiusDegrees));

            intentBuilder.setLocationBias(bounds);
        }

        // Start the autocomplete intent
        Intent intent = intentBuilder.build(this);
        startActivityForResult(intent, REQUEST_LOCATION_AUTOCOMPLETE);
    }

    /**
     * Requests location permissions from the user.
     */
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Retrieves the current device location.
     */
    private void getCurrentLocation() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Define location listener
                LocationListener locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        currentLocation = location;
                        getAddressFromLocation(location);
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(String provider) {}

                    @Override
                    public void onProviderDisabled(String provider) {
                        Toast.makeText(EditMoodActivity.this,
                                "Please enable location services for better experience",
                                Toast.LENGTH_SHORT).show();
                    }
                };

                // Request location updates
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0,
                            0,
                            locationListener);
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            0,
                            0,
                            locationListener);
                }

                // Try to get last known location immediately
                Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnownLocation != null) {
                    currentLocation = lastKnownLocation;
                    getAddressFromLocation(lastKnownLocation);
                } else {
                    // Try with network provider
                    lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastKnownLocation != null) {
                        currentLocation = lastKnownLocation;
                        getAddressFromLocation(lastKnownLocation);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error getting location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Converts a location into an address string.
     *
     * @param location The location to convert.
     */
    private void getAddressFromLocation(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();

                // Get address details
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        sb.append(", ");
                    }
                }

                currentLocationAddress = sb.toString();

                // Only update UI if location is not already set from intent
                runOnUiThread(() -> {
                    if (selectedLocationText != null && (selectedLocationName == null || selectedLocationName.isEmpty())) {
                        selectedLocationText.setText("Tap to use current location");
                        selectedLocationText.setTextColor(Color.parseColor("#0277BD")); // Light Blue
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        // Update intensity display text.
        updateIntensityDisplay(intensityDisplay, progress);

        // Calculate a scale factor from the intensity.
        float emojiScale = 0.7f + (progress / 10f * 0.6f);
        int baseSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80, getResources().getDisplayMetrics());
        int newSize = (int) (baseSize * emojiScale);

        // Re-load the themed drawable and update its bounds.
        Drawable drawable = ContextCompat.getDrawable(this, getEmojiResourceId(selectedMood, selectedTheme));
        if (drawable != null) {
            drawable.setBounds(0, 0, newSize, newSize);
            selectedMoodEmoji.setCompoundDrawables(null, drawable, null, null);
            selectedMoodEmoji.setCompoundDrawablePadding(dpToPx(8));
            selectedMoodEmoji.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) selectedMoodEmoji.getLayoutParams();
            params.height = dpToPx(100);
            selectedMoodEmoji.setLayoutParams(params);
        }

        // Adjust container background color based on intensity.
        int adjustedColor = adjustColorIntensity(selectedColor, progress);
        GradientDrawable moodContainerBg;
        if (!(selectedMoodContainer.getBackground() instanceof GradientDrawable)) {
            moodContainerBg = new GradientDrawable();
            moodContainerBg.setCornerRadius(dpToPx(12));
            selectedMoodContainer.setBackground(moodContainerBg);
        } else {
            moodContainerBg = (GradientDrawable) selectedMoodContainer.getBackground();
        }
        moodContainerBg.setColor(adjustedColor);

        // Update mood text to reflect intensity.
        if (progress <= 3) {
            selectedMoodText.setText("Slightly " + selectedMood);
        } else if (progress <= 7) {
            selectedMoodText.setText(selectedMood);
        } else {
            selectedMoodText.setText("Very " + selectedMood);
        }

        selectedMoodEmoji.setScaleX(emojiScale);
        selectedMoodEmoji.setScaleY(emojiScale);
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
     * Called when permission results are returned.
     *
     * @param requestCode  The permission request code.
     * @param permissions  The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Handles results from camera, gallery, or location autocomplete intents.
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
                    photoSize = sizeKB;
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
            } else if (requestCode == REQUEST_LOCATION_AUTOCOMPLETE) {
                // Handle location selection from Places Autocomplete
                Place place = Autocomplete.getPlaceFromIntent(data);
                selectedLocationName = place.getName() + ", " + place.getAddress();
                selectedLocationCoords = place.getLatLng();
                selectedLocationText.setText(selectedLocationName);
                selectedLocationText.setTextColor(Color.BLACK);
                Log.d("EditMoodActivity", "Location selected: " + selectedLocationName);
            }
        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
            if (requestCode == REQUEST_LOCATION_AUTOCOMPLETE) {
                // Handle the error
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("EditMoodActivity", "Place selection error: " + status.getStatusMessage());
                Toast.makeText(this, "Error selecting location", Toast.LENGTH_SHORT).show();
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

    /**
     * Retrieves the drawable resource ID for a given emoji based on the mood and theme.
     *
     * @param moodId The mood identifier.
     * @param theme  The theme identifier.
     * @return The drawable resource ID.
     */
    private int getEmojiResourceId(String moodId, String theme) {
        String resourceName = "emoji_" + moodId.toLowerCase() + "_" + theme.toLowerCase();
        return getResources().getIdentifier(resourceName, "drawable", getPackageName());
    }

    /**
     * Updates the selected mood emoji and the background of the mood container.
     */
    private void updateSelectedMoodEmoji() {
        if (selectedMood == null) return;

        selectedEmoji = moodEmojis.get(selectedMood);
        selectedColor = moodColors.get(selectedMood);

        // Clear any previous text and background.
        selectedMoodEmoji.setText("");
        selectedMoodEmoji.setBackground(null);

        Drawable drawable = ContextCompat.getDrawable(this, getEmojiResourceId(selectedMood, selectedTheme));
        if (drawable != null) {
            int baseSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 80, getResources().getDisplayMetrics());
            // Use the current slider progress for scaling
            float emojiScale = 0.7f + (moodIntensitySlider.getProgress() / 10f * 0.6f);
            int newSize = (int) (baseSize * emojiScale);
            drawable.setBounds(0, 0, newSize, newSize);
            selectedMoodEmoji.setCompoundDrawables(null, drawable, null, null);
            selectedMoodEmoji.setCompoundDrawablePadding(dpToPx(8));
        }

        // Update the container background.
        GradientDrawable moodContainerBg = new GradientDrawable();
        moodContainerBg.setColor(selectedColor);
        moodContainerBg.setCornerRadius(dpToPx(12));
        selectedMoodContainer.setBackground(moodContainerBg);
    }
}