package com.example.vibeverse;

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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
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
  
    private long fileSizeKB;

  
   /**
     * Bitmap of the selected profile picture.
     */
    private Bitmap currentBitmap;
    /**
     * ImageView for the profile picture placeholder.
     */
    private ImageView profilePicturePlaceholder;
    /**
     * ImageView for the selected profile picture.
     */
    private ImageView profilePictureSelected;

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

        // Set onClickListener for the continue button
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleContinueButtonClick();
            }
        });

        // Set onClickListener for the date of birth field to show a DatePickerDialog
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
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


            if (imageUri != null) {
                userData.put("hasProfilePic", true);
                userData.put("profilePicUri", imageUri.toString());
                userData.put("profilePicSizeKB", fileSizeKB);

            }

            else{
                userData.put("hasProfilePic", false);
            }



            // Get Firestore instance and save data
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(user.getUid())
                    .set(userData)
                    .addOnSuccessListener(aVoid -> {
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
                    fileSizeKB = sizeKB;
                    imageUri = uri;
                    currentBitmap = bitmap;
                    profilePicturePlaceholder.setVisibility(View.GONE);
                    profilePictureSelected.setVisibility(View.VISIBLE);
                    profilePictureSelected.setImageBitmap(bitmap);
                });
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                imageUri = data.getData();
                ImageUtils.processImage(this, imageUri, (bitmap, uri, sizeKB) -> {
                    fileSizeKB = sizeKB;
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
}