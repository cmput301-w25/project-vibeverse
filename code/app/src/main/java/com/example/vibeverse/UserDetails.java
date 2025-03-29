package com.example.vibeverse;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UserDetails activity collects additional profile information from the user.
 * <p>
 * This activity allows the user to input their full name, username, bio, date of birth,
 * and select a gender from a dropdown list. It also provides an option to choose a profile picture.
 * After filling in all required fields, the user can continue to complete registration.
 * The data is saved to Firestore under the "users" collection.
 * </p>
 */
public class UserDetails extends AppCompatActivity {

    /**
     * EditText for the user's full name.
     */
    private EditText fullName;
    /**
     * EditText for the user's username.
     */
    private EditText username;
    /**
     * EditText for the user's bio.
     */
    private EditText bio;
    /**
     * EditText for the user's date of birth.
     */
    private EditText dob;
    /**
     * Spinner for selecting the user's gender.
     */
    private Spinner genderSpinner;
    /**
     * Button to continue after entering details.
     */
    private Button continueButton;
    /**
     * FirebaseAuth instance for authentication.
     */
    FirebaseAuth auth;
    /**
     * The currently authenticated FirebaseUser.
     */
    FirebaseUser user;
    private TextView usernameValidationText;
    private boolean isUsernameValid = false;

    private Bitmap currentBitmap;

    /**
     * Request codes for image capture and selection.
     */
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int PERMISSION_REQUEST_CODE = 100;
    /**
     * URI of the selected profile picture.
     */
    private Uri imageUri;

    private long fileSize;

    private boolean hasProfilePic;

    /**
     * ImageView for the profile picture placeholder.
     */
    private ImageView profilePicturePlaceholder;
    /**
     * ImageView for the selected profile picture.
     */
    private ImageView profilePictureSelected;

    private String originalUsername = "";
    private boolean isEditMode;



    private interface UsernameSuggestionCallback {
        void onSuggestionGenerated(String suggestion);
    }

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the UI components, sets up the gender spinner with a placeholder,
     * sets click listeners for the continue button and date of birth field, and configures
     * the profile picture selector.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Initialize input fields and UI elements
        fullName = findViewById(R.id.fullName);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);
        dob = findViewById(R.id.dob);
        genderSpinner = findViewById(R.id.genderSpinner);
        continueButton = findViewById(R.id.continueButton);
        profilePicturePlaceholder = findViewById(R.id.profilePicturePlaceholder);
        profilePictureSelected = findViewById(R.id.profilePictureSelected);
        usernameValidationText = findViewById(R.id.usernameValidationText);



        // Set up the profile picture button click listener
        FrameLayout btnProfilePicture = findViewById(R.id.btnProfilePicture);
        btnProfilePicture.setOnClickListener(v -> showImagePickerDialog());


        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        if (user != null) {
            String userDetails = "User ID: " + user.getUid() + "\nEmail: " + user.getEmail();
        }


        // Set the hint for the Date of Birth field
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.gender_options)) {

            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Disable first item (placeholder)
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(Color.parseColor("#908E8E")); // Placeholder color
                } else {
                    textView.setTextColor(Color.WHITE); // Regular text color
                }
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
                return view;
            }


            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.BLACK); // Dropdown items should be black
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);


        username.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                String usernameText = s.toString().trim();
                if (!usernameText.isEmpty()) {
                    validateUsername(usernameText);
                }
            }
        });


        // Set onClickListener for the continue button
        String source = getIntent().getStringExtra("source");
        if ("edit_profile".equals(source)) {
            // Change button text to indicate editing mode
            continueButton.setText(R.string.save_changes_edit_profile);
            loadUserProfileForEditing();
            // Change behavior for continue button to update existing profile
            continueButton.setOnClickListener(v -> updateUserProfile());
            isEditMode = true;
        } else {
            // Default behavior for new registration
            continueButton.setOnClickListener(v -> handleContinueButtonClick());
            isEditMode = false;
        }


        // Set onClickListener for the date of birth field to show a DatePickerDialog
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
    }


    private void validateUsername(String usernameToCheck) {
        if (usernameToCheck.isEmpty()) return;

        if (isEditMode && usernameToCheck.equals(originalUsername)) {
            usernameValidationText.setText("✓ Username available");
            usernameValidationText.setTextColor(Color.GREEN);
            usernameValidationText.setVisibility(View.VISIBLE);
            isUsernameValid = true;
            return;
        }

        // Show loading state
        usernameValidationText.setText("Checking username...");
        usernameValidationText.setTextColor(Color.GRAY);
        usernameValidationText.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users")
                .whereEqualTo("username", usernameToCheck)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Username is available
                            usernameValidationText.setText("✓ Username available");
                            usernameValidationText.setTextColor(Color.GREEN);
                            isUsernameValid = true;
                        } else {
                            // Username is already taken
                            isUsernameValid = false;
                            // Generate a unique username suggestion
                            generateUniqueUsernameSuggestion(usernameToCheck, suggestion -> {
                                String message = "Username already taken. Try " + suggestion + "?";
                                usernameValidationText.setText(message);
                                usernameValidationText.setTextColor(Color.RED);
                            });
                        }
                    } else {
                        // Error checking username
                        usernameValidationText.setText("✗ Error checking username");
                        usernameValidationText.setTextColor(Color.RED);
                        isUsernameValid = false;
                    }
                });
    }


    /**
     * Handles the continue button click by validating input fields and saving user details to Firestore.
     * <p>
     * If all required fields are filled, user details are saved in a HashMap and uploaded to the "users"
     * collection in Firestore. On success, the activity navigates to MainActivity.
     * </p>
     */

    private void handleContinueButtonClick() {
        boolean allFieldsFilled = true;

        // Validate each required field
        if (fullName.getText().toString().trim().isEmpty()) {
            fullName.setError("Required!");
            allFieldsFilled = false;
        }
        if (username.getText().toString().trim().isEmpty()) {
            username.setError("Required!");
            allFieldsFilled = false;
        } else if (!isUsernameValid) {
            username.setError("Username already taken");
            allFieldsFilled = false;
        }
        if (bio.getText().toString().trim().isEmpty()) {
            bio.setError("Required!");
            allFieldsFilled = false;
        }
        if (dob.getText().toString().trim().isEmpty()) {
            dob.setError("Required!");
            allFieldsFilled = false;
        }
        if (genderSpinner.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) genderSpinner.getSelectedView();
            errorText.setError("Required!");
            allFieldsFilled = false;
        }

        if (allFieldsFilled) {
            // Create a HashMap with user data
            Map<String, Object> userData = new HashMap<>();
            userData.put("fullName", fullName.getText().toString().trim());
            userData.put("username", username.getText().toString().trim());
            userData.put("bio", bio.getText().toString().trim());
            userData.put("dateOfBirth", dob.getText().toString().trim());
            userData.put("gender", genderSpinner.getSelectedItem().toString());
            userData.put("email", user.getEmail());
            userData.put("followerCount", 0);
            userData.put("followingCount", 0);
            userData.put("newNotificationCount", 0);
            userData.put("usernameLowercase", username.getText().toString().trim().toLowerCase()); // Lowercase version
            userData.put("selectedTheme", "default");
            userData.put("totalXP", 0);
            userData.put("level", 1);




            if (imageUri != null) {
                userData.put("hasProfilePic", true);
                userData.put("profilePicUri", imageUri.toString());
                userData.put("profilePicSizeKB", fileSize);

            } else {
                userData.put("hasProfilePic", false);
            }

            userData.put("followerCount", 0);
            userData.put("followingCount", 0);
            userData.put("newNotificationCount", 0);

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection("users").document(user.getUid());


            // Get Firestore instance and save data
            userDocRef.set(userData)
                    .addOnSuccessListener(aVoid -> {

                        List<ThemeData> themes = loadThemesFromAssets();
                        List<String> unlockedThemesList = new ArrayList<>();
                        List<String> lockedThemesList = new ArrayList<>();

                        if (themes != null && !themes.isEmpty()) {
                            for (ThemeData theme : themes) {
                                // Default theme is "default" (or whatever id you designate)
                                if ("default".equalsIgnoreCase(theme.getId())) {
                                    unlockedThemesList.add(theme.getId());
                                } else {
                                    lockedThemesList.add(theme.getId());
                                }
                            }
                        }

                        // Create the unlockedThemes subcollection with the default theme
                        Map<String, Object> unlockedThemesMap = new HashMap<>();
                        unlockedThemesMap.put("themeNames", unlockedThemesList);
                        userDocRef.collection("unlockedThemes")
                                .document("list")
                                .set(unlockedThemesMap);

                        // Create the lockedThemes subcollection with the rest of the themes
                        Map<String, Object> lockedThemesMap = new HashMap<>();
                        lockedThemesMap.put("themeNames", lockedThemesList);
                        userDocRef.collection("lockedThemes")
                                .document("list")
                                .set(lockedThemesMap);

                        // (a) followers subcollection with an empty array of follower IDs
                        Map<String, Object> followersMap = new HashMap<>();
                        followersMap.put("followerIds", new ArrayList<String>()); // empty list
                        userDocRef.collection("followers")
                                .document("list")
                                .set(followersMap);

                        // (b) following subcollection with an empty array of following IDs
                        Map<String, Object> followingMap = new HashMap<>();
                        followingMap.put("followingIds", new ArrayList<String>()); // empty list
                        userDocRef.collection("following")
                                .document("list")
                                .set(followingMap);

                        Map<String, Object> followReqMap = new HashMap<>();
                        followReqMap.put("followReqs", new ArrayList<String>()); // empty list
                        userDocRef.collection("followRequests")
                                .document("list")
                                .set(followingMap);


                        userDocRef.collection("notifications")
                                .document("placeholder")
                                .set(new HashMap<String, Object>());

                        // ----- New Code: Load achievements.json and create achievement docs -----
                        List<Achievement> achievements = loadAchievementsFromAssets(); // Your helper to parse achievements.json
                        if (achievements != null && !achievements.isEmpty()) {
                            for (Achievement achievement : achievements) {
                                // For each achievement, create a document with:
                                // progress = 0, completion_status = "incomplete", unique_entities = empty array
                                Map<String, Object> achData = new HashMap<>();
                                achData.put("progress", 0);
                                achData.put("completion_status", "incomplete");
                                achData.put("unique_entities", new ArrayList<String>());

                                userDocRef.collection("achievements")
                                        .document(achievement.getId())
                                        .set(achData);
                            }
                        }

                        Toast.makeText(UserDetails.this, "Profile created successfully!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserDetails.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UserDetails.this, "Error creating profile", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Displays a DatePickerDialog for the user to select their date of birth.
     * <p>
     * The selected date is formatted as "day/month/year" and set to the dob EditText.
     * </p>
     */
    private void showDatePicker() {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a DatePickerDialog with the current date as default
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Format and set the selected date
                String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                dob.setText(date);
            }
        }, year, month, day);

        datePickerDialog.show();
    }

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
                        imageUri = null;
                        profilePictureSelected.setVisibility(View.GONE);
                        profilePicturePlaceholder.setVisibility(View.VISIBLE);
                        // Show hint text again
                        for (int i = 0; i < ((ViewGroup) profilePicturePlaceholder.getParent()).getChildCount(); i++) {
                            View child = ((ViewGroup) profilePicturePlaceholder.getParent()).getChildAt(i);
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
     * <p>
     * Checks for camera permissions, creates a temporary file for the image,
     * and launches the camera app to capture the photo.
     * </p>
     */
    private void dispatchTakePictureIntent() {
        if (!hasCameraPermission()) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
            return;
        }
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
     * Checks and requests the necessary permissions (Camera and Storage) at runtime for Android M and above.
     *
     * @return true if permissions are already granted, false otherwise.
     */
    private boolean requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        return true;
    }

    /**
     * Called when permission requests complete.
     *
     * @param requestCode  The request code passed in requestPermissions().
     * @param permissions  The requested permissions.
     * @param grantResults The results for the corresponding permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d("Permission", permissions[i] + " was denied.");
                }
            }
            if (allGranted) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                dispatchTakePictureIntent();
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
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    fileSize = sizeKB;
                    imageUri = uri;
                    currentBitmap = bitmap;
                    profilePicturePlaceholder.setVisibility(View.GONE);
                    profilePictureSelected.setVisibility(View.VISIBLE);
                    profilePictureSelected.setImageBitmap(bitmap);
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    fileSize = sizeKB;
                    imageUri = uri;
                    currentBitmap = bitmap;
                    profilePicturePlaceholder.setVisibility(View.GONE);
                    profilePictureSelected.setVisibility(View.VISIBLE);
                    profilePictureSelected.setImageBitmap(bitmap);
                });
            }
        }
    }


    private boolean hasCameraPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * Recursively generates a unique username suggestion by appending a random number.
     *
     * @param originalUsername the original username entered by the user.
     * @param callback         a callback to return the generated suggestion.
     */
    private void generateUniqueUsernameSuggestion(String originalUsername, UsernameSuggestionCallback callback) {
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Generate a suggestion by appending a random number (e.g., between 0 and 999)
        String suggestion = originalUsername + ((int) (Math.random() * 1000));

        // Check if this suggestion is already taken
        db.collection("users")
                .whereEqualTo("username", suggestion)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            // Suggestion is unique
                            callback.onSuggestionGenerated(suggestion);
                        } else {
                            // If taken, try generating another suggestion
                            generateUniqueUsernameSuggestion(originalUsername, callback);
                        }
                    } else {
                        // In case of an error, return the suggestion anyway
                        callback.onSuggestionGenerated(suggestion);
                    }
                });
    }

    /**
     * Loads the current user's profile data from Firestore and pre-populates the fields.
     */
    private void loadUserProfileForEditing() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        fullName.setText(documentSnapshot.getString("fullName"));
                        originalUsername = documentSnapshot.getString("username");
                        username.setText(originalUsername);
                        bio.setText(documentSnapshot.getString("bio"));
                        dob.setText(documentSnapshot.getString("dateOfBirth"));

                        // Set gender spinner selection. Assuming your gender options array
                        // has a known order, find the index of the saved gender.
                        String gender = documentSnapshot.getString("gender");
                        ArrayAdapter<String> adapter = (ArrayAdapter<String>) genderSpinner.getAdapter();
                        int spinnerPosition = adapter.getPosition(gender);
                        genderSpinner.setSelection(spinnerPosition);

                        // Optionally, load profile picture if available
                        Boolean hasProfilePic = documentSnapshot.getBoolean("hasProfilePic");
                        if (hasProfilePic != null && hasProfilePic) {
                            String profilePicUri = documentSnapshot.getString("profilePicUri");
                            imageUri = Uri.parse(profilePicUri);
                            if (profilePicUri != null && !profilePicUri.isEmpty()) {
                                // Using Glide to load the image
                                Glide.with(UserDetails.this)
                                        .load(profilePicUri)
                                        .into(profilePictureSelected);
                                profilePicturePlaceholder.setVisibility(View.GONE);
                                profilePictureSelected.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(UserDetails.this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Updates the current user's profile details in Firestore.
     */
    private void updateUserProfile() {
        // Validate fields just as you do in handleContinueButtonClick()
        boolean allFieldsFilled = true;
        if (fullName.getText().toString().trim().isEmpty()) {
            fullName.setError("Required!");
            allFieldsFilled = false;
        }
        if (username.getText().toString().trim().isEmpty()) {
            username.setError("Required!");
            allFieldsFilled = false;
        } else if (!isUsernameValid) {
            username.setError("Username already taken");
            allFieldsFilled = false;
        }
        if (bio.getText().toString().trim().isEmpty()) {
            bio.setError("Required!");
            allFieldsFilled = false;
        }
        if (dob.getText().toString().trim().isEmpty()) {
            dob.setError("Required!");
            allFieldsFilled = false;
        }
        if (genderSpinner.getSelectedItemPosition() == 0) {
            TextView errorText = (TextView) genderSpinner.getSelectedView();
            errorText.setError("Required!");
            allFieldsFilled = false;
        }

        if (allFieldsFilled) {
            Map<String, Object> userData = new HashMap<>();
            userData.put("fullName", fullName.getText().toString().trim());
            userData.put("username", username.getText().toString().trim());
            userData.put("bio", bio.getText().toString().trim());
            userData.put("dateOfBirth", dob.getText().toString().trim());
            userData.put("gender", genderSpinner.getSelectedItem().toString());
            // Other fields you might want to update (like profile picture info)
            Log.d("UserDetails", "imageUri: " + imageUri);
            if (imageUri != null) {
                userData.put("hasProfilePic", true);
                userData.put("profilePicUri", imageUri.toString());
                userData.put("profilePicSizeKB", fileSize);
            }
            else {
                userData.put("hasProfilePic", false);
                userData.put("profilePicUri", null);
                userData.put("profilePicSizeKB", 0);
            }
     ;

            // For consistency, update the lowercase username if needed
            userData.put("usernameLowercase", username.getText().toString().trim().toLowerCase());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid())
                    .update(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UserDetails.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                        // Optionally, navigate back or finish activity
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(UserDetails.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this, "Please fill in all required fields!", Toast.LENGTH_SHORT).show();
        }
    }

    private List<ThemeData> loadThemesFromAssets() {
        try {
            InputStream inputStream = getAssets().open("themes.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Type listType = new TypeToken<List<ThemeData>>() {}.getType();
            List<ThemeData> themes = new Gson().fromJson(reader, listType);
            reader.close();
            return themes;
        } catch (Exception e) {
            Log.e(TAG, "Error loading themes from assets", e);
            return new ArrayList<>();
        }
    }

    private List<Achievement> loadAchievementsFromAssets() {
        try {
            InputStream inputStream = getAssets().open("achievements.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            AchievementsWrapper wrapper = new Gson().fromJson(reader, AchievementsWrapper.class);
            reader.close();
            return wrapper != null ? wrapper.getAchievements() : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error loading achievements from assets", e);
            return new ArrayList<>();
        }
    }



}