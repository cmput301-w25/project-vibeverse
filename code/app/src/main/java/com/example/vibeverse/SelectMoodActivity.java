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
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.transition.TransitionManager;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SelectMoodActivity provides a sleek, professional UI for users to select their mood,
 * adjust mood intensity, optionally add social situation information, and choose an image.
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
    private EditText reasonWhyInput;
    private Spinner socialSituationInput;
    private Button continueButton;

    private ImageView backButton;

    private androidx.appcompat.widget.SwitchCompat visibilitySwitch;


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
    private long photoSize;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder, imgSelected;
    private TextView imageHintText;

    private static final int REQUEST_LOCATION_AUTOCOMPLETE = 3;
    private FrameLayout locationButton;
    private TextView selectedLocationText;
    private String selectedLocationName = null;
    private LatLng selectedLocationCoords = null;

    // Location fields
    private LocationManager locationManager;
    private Location currentLocation;
    private String currentLocationAddress = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

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

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }

        // Initialize UI references
        mainContainer = findViewById(R.id.mainContainer);
        selectedMoodEmoji = findViewById(R.id.selectedMoodEmoji);
        selectedMoodText = findViewById(R.id.selectedMoodText);
        selectedMoodContainer = findViewById(R.id.selectedMoodContainer);
        moodIntensitySlider = findViewById(R.id.moodIntensitySlider);
        socialSituationInput = findViewById(R.id.socialSituationSpinner);
        continueButton = findViewById(R.id.continueButton);
        backButton = findViewById(R.id.backArrow);
        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);
        reasonWhyInput = findViewById(R.id.reasonWhyInput);
        visibilitySwitch = findViewById(R.id.visibilitySwitch);
        locationButton = findViewById(R.id.btnLocation);
        selectedLocationText = findViewById(R.id.selectedLocationText);

        // Request location permission and get current location
        requestLocationPermission();
        getCurrentLocation();

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
                        String socialSituation = socialSituationInput.getSelectedItem().toString().trim();
                        String reasonWhy = reasonWhyInput.getText().toString().trim();

                        // Check if reasonWhy is empty
                        if (reasonWhy.isEmpty()) {
                            reasonWhyInput.setError("Reason why is required.");
                            reasonWhyInput.requestFocus();
                            return;
                        }


                        // Error handling for reasonWhy input
                        if (reasonWhy.length() > 200) {
                            reasonWhyInput.setError("Reason why must be 200 characters or less.");
                            reasonWhyInput.requestFocus();
                            return;
                        }


                        // Store the intensity value in the MoodEvent
                        int intensity = moodIntensitySlider.getProgress();
                        boolean isPublic = visibilitySwitch.isChecked();


                        MoodEvent moodEvent;
                        if (imageUri != null && currentBitmap != null) {
                            // If an image is selected, create a Photograph instance and attach it to the mood event
                            // Using the existing Photograph constructor that matches your implementation
                            Photograph photograph = new Photograph(
                                    imageUri,
                                    photoSize, // Estimate file size in KB
                                    currentBitmap,
                                    new Date(),
                                    "VibeVerse Location" // Default location - get location functionality not yet implemented
                            );

                            moodEvent = new MoodEvent(userId,selectedMood, selectedEmoji, reasonWhy, socialSituation, photograph, isPublic);
                            // Add intensity to the mood event
                            moodEvent.setIntensity(intensity);
                        } else {
                            Log.d("SelectMoodActivity", "onClick: Emoji before assignment= " + selectedEmoji);
                            moodEvent = new MoodEvent(userId,selectedMood, selectedEmoji, reasonWhy, socialSituation, isPublic);
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
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
            startActivity(intent);
            finish();
        });

        // Set the click listener for the location button
        locationButton.setOnClickListener(v -> {
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
                // If current location isn't available, just open the search
                startPlacesAutocomplete();
            }
        });
    }

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
     * Request location permissions
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
     * Get the current location
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
                        Toast.makeText(SelectMoodActivity.this,
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
     * Convert location coordinates to address
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

                // Update UI to indicate current location is available
                runOnUiThread(() -> {
                    if (selectedLocationText != null) {
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
     * Saves the provided MoodEvent to Firestore.
     *
     * @param moodEvent The MoodEvent to save.
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
        moodData.put("ownerUserId", userId);
        moodData.put("isPublic", moodEvent.isPublic());

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

        // Generate a document ID for the mood event (using current time in millis as an example)
        String docId = String.valueOf(System.currentTimeMillis());

        // Save the mood event under the user's moods collection.
        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .document(docId)
                .set(moodData)
                .addOnSuccessListener(aVoid -> {
                    // After saving the mood, create an empty subcollection for comments.
                    // Adding a dummy document to "comments" ensures the subcollection is created.
                    Map<String, Object> initData = new HashMap<>();
                    initData.put("init", true); // This field is optional.
                    db.collection("Usermoods")
                            .document(userId)
                            .collection("moods")
                            .document(docId)
                            .collection("comments")
                            .document("init")
                            .set(initData)
                            .addOnSuccessListener(x -> {
                                // Successfully created comments subcollection.
                                Toast.makeText(SelectMoodActivity.this, "Mood saved successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear back stack
                                startActivity(intent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Handle error creating the comments subcollection.
                                Toast.makeText(SelectMoodActivity.this, "Mood saved, but error creating comments section: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    // Handle the error saving the mood event.
                    Toast.makeText(SelectMoodActivity.this, "Error saving mood: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    // Navigate back so the user isn't stuck.
                    Intent intent = new Intent(SelectMoodActivity.this, ProfilePage.class);
                    startActivity(intent);
                    finish();
                });
    }


    /**
     * Sets up a custom toolbar.
     */
    private void setupToolbar() {
        Toolbar toolbar = new Toolbar(this);
        toolbar.setTitle("");
        toolbar.setBackgroundColor(Color.TRANSPARENT);

        // Use the standard navigation icon for the back button
        toolbar.setNavigationIcon(getResources().getIdentifier(
                "abc_ic_ab_back_material", "drawable", getPackageName()));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        mainContainer.addView(toolbar, 0);
    }

    /**
     * Sets up the continue button with professional styling.
     */
    private void setupContinueButton() {
        int buttonColor = Color.BLACK;
        continueButton.setBackgroundTintList(null);
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setCornerRadius(dpToPx(24));
        buttonBg.setColor(buttonColor);
        continueButton.setBackgroundTintList(null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            continueButton.setElevation(dpToPx(4));
        }

        continueButton.setBackground(buttonBg);
        continueButton.setPadding(dpToPx(24), dpToPx(12), dpToPx(24), dpToPx(12));
        continueButton.setText("Continue");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            continueButton.setTextAppearance(android.R.style.TextAppearance_Medium);
        }
        continueButton.setTextColor(Color.WHITE);
        continueButton.setTypeface(null, Typeface.BOLD);

        if (continueButton.getParent() instanceof FrameLayout) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) continueButton.getLayoutParams();
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.setMargins(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));
            continueButton.setLayoutParams(params);
        }
    }

    /**
     * Sets up the mood intensity slider with visual feedback effects.
     */
    private void setupMoodIntensitySlider() {
        TextView sliderLabel = new TextView(this);
        sliderLabel.setText("Mood Intensity");
        sliderLabel.setTextColor(Color.WHITE);
        sliderLabel.setTypeface(null, Typeface.BOLD);
        sliderLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        sliderLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        LinearLayout sliderContainer = new LinearLayout(this);
        sliderContainer.setOrientation(LinearLayout.VERTICAL);

        ViewGroup sliderParent = (ViewGroup) moodIntensitySlider.getParent();
        int sliderIndex = sliderParent.indexOfChild(moodIntensitySlider);
        sliderParent.removeView(moodIntensitySlider);

        moodIntensitySlider.setMax(10);
        moodIntensitySlider.setProgress(5); // Default to middle
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));
        moodIntensitySlider.setThumbTintList(ColorStateList.valueOf(Color.WHITE));

        LinearLayout minMaxContainer = new LinearLayout(this);
        minMaxContainer.setOrientation(LinearLayout.HORIZONTAL);

        TextView minLabel = new TextView(this);
        minLabel.setText("Low");
        minLabel.setTextColor(Color.WHITE);
        minLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);

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

        sliderContainer.addView(sliderLabel);
        sliderContainer.addView(moodIntensitySlider);
        sliderContainer.addView(minMaxContainer);

        sliderParent.addView(sliderContainer, sliderIndex);

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

        moodIntensitySlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateIntensityDisplay(intensityDisplay, progress);
                float emojiScale = 0.7f + (progress / 10f * 0.6f); // Scale from 0.7 to 1.3
                selectedMoodEmoji.setScaleX(emojiScale);
                selectedMoodEmoji.setScaleY(emojiScale);
                int adjustedColor = adjustColorIntensity(selectedColor, progress);
                GradientDrawable moodContainerBg = (GradientDrawable) selectedMoodContainer.getBackground();
                moodContainerBg.setColor(adjustedColor);
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
                pulseSet.start();
            }
        });

        updateIntensityDisplay(intensityDisplay, moodIntensitySlider.getProgress());
    }

    /**
     * Updates the visual intensity display based on slider progress.
     *
     * @param intensityDisplay The TextView showing intensity.
     * @param progress         The current slider progress.
     */
    private void updateIntensityDisplay(TextView intensityDisplay, int progress) {
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
     * Adjusts the color intensity based on slider progress.
     *
     * @param baseColor The original mood color.
     * @param intensity The intensity value (0-10).
     * @return The adjusted color.
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
     * Adjusts the saturation of a color.
     *
     * @param color  The original color.
     * @param factor The factor to adjust saturation by (>1 for more saturation, <1 for less).
     * @return The adjusted color.
     */
    private int adjustColorSaturation(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] * factor);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * (factor > 1 ? 0.9f : 1.1f)));
        return Color.HSVToColor(hsv);
    }

    /**
     * Sets up the input fields with labels and styled components.
     */
    private void setupInputFields() {
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


        int socialIndex = mainContainer.indexOfChild(socialSituationInput);
        mainContainer.addView(socialLabel, socialIndex);


    }

    /**
     * Sets up the image selector with improved styling.
     */
    private void setupImageSelector() {
        GradientDrawable imageBtnBg = new GradientDrawable();
        imageBtnBg.setCornerRadius(dpToPx(12));
        imageBtnBg.setColor(Color.WHITE);
        imageBtnBg.setStroke(1, Color.parseColor("#E0E0E0"));

        FrameLayout btnTestImage = findViewById(R.id.btnImage);
        btnTestImage.setBackground(imageBtnBg);

        imgPlaceholder.setColorFilter(Color.parseColor("#AAAAAA"));

        TextView imageLabel = new TextView(this);
        imageLabel.setText("Add a photo (optional)");
        imageLabel.setTextColor(Color.WHITE);
        imageLabel.setTypeface(null, Typeface.BOLD);
        imageLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        imageLabel.setPadding(0, (int) dpToPx(16), 0, (int) dpToPx(4));

        int imageIndex = mainContainer.indexOfChild(btnTestImage);
        mainContainer.addView(imageLabel, imageIndex);

        imageHintText = new TextView(this);
        imageHintText.setText("Tap to add a photo");
        imageHintText.setTextColor(Color.parseColor("#757575"));
        imageHintText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        imageHintText.setGravity(Gravity.CENTER);
        btnTestImage.addView(imageHintText);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        params.bottomMargin = (int) dpToPx(20);
        imageHintText.setLayoutParams(params);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            btnTestImage.setForeground(getDrawable(android.R.drawable.list_selector_background));
        }

        btnTestImage.setOnClickListener(v -> showImagePickerDialog());
    }

    /**
     * Creates mood selection buttons within the provided GridLayout.
     * Each button is styled uniformly with rounded corners, elevation, and a consistent margin.
     *
     * @param moodGrid The GridLayout where mood buttons will be added.
     */
    private void createMoodButtons(GridLayout moodGrid) {
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
            emojiView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
            emojiView.setGravity(Gravity.CENTER);

            TextView moodNameView = new TextView(this);
            moodNameView.setText(mood);
            moodNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            moodNameView.setTextColor(Color.WHITE);
            moodNameView.setGravity(Gravity.CENTER);
            moodNameView.setTypeface(null, Typeface.BOLD);

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
     * Converts dp (density-independent pixels) to px (pixels) based on device density.
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
     * Updates the UI when a mood is selected.
     * Sets the selected mood, emoji, and color; updates the mood display area; and applies a gradient background.
     *
     * @param mood The mood selected by the user.
     */
    private void selectMood(String mood) {
        selectedMood = mood;
        selectedEmoji = moodEmojis.get(mood);
        selectedColor = moodColors.get(mood);

        selectedMoodContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            selectedMoodText.setText(mood);
            selectedMoodEmoji.setText(selectedEmoji);

            GradientDrawable moodContainerBg = new GradientDrawable();
            moodContainerBg.setColor(selectedColor);
            moodContainerBg.setCornerRadius(dpToPx(12));
            selectedMoodContainer.setBackground(moodContainerBg);

            selectedMoodContainer.animate().alpha(1f).setDuration(300).start();
        }).start();

        applyGradientBackground(selectedColor);
        moodIntensitySlider.setProgressTintList(ColorStateList.valueOf(selectedColor));
        setupContinueButton();
    }

    /**
     * Utility class for color manipulation.
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

    /**
     * Applies a gradient background to the main container.
     * The gradient starts with a neutral color (#2D2D3A) and transitions through lighter and medium shades to the base mood color.
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
     * Displays a dialog allowing the user to choose between taking a photo,
     * selecting one from the gallery, or removing the current photo.
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
     * Requests the necessary permissions (Camera and Storage) at runtime.
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
}