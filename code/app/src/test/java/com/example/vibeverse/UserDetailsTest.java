package com.example.vibeverse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for the UserDetails activity's logic.
 *
 * These tests focus purely on the business logic without any Android framework dependencies.
 */
@RunWith(JUnit4.class)
public class UserDetailsTest {

    /**
     * Test implementation of the core user details logic without Android dependencies
     */
    private static class UserDetailsLogic {
        // User input fields
        public String fullName;
        public String username;
        public String bio;
        public String dateOfBirth;
        public String gender;
        public String email;

        // UI state tracking
        public boolean profilePicturePlaceholderVisible = true;
        public boolean profilePictureSelectedVisible = false;
        public String toastMessage;
        public String navigatedTo;
        public boolean finishCalled;

        // Image data
        public String imageUri;
        public long fileSizeKB;

        // Input validation errors
        public String fullNameError;
        public String usernameError;
        public String bioError;
        public String dobError;
        public String genderError;

        // Permission state
        public boolean cameraPermissionGranted = false;
        public boolean storagePermissionGranted = false;

        // Database operations
        public boolean databaseSaveSuccessful = true;
        public Map<String, Object> savedUserData;

        /**
         * Utility method to check if a string is empty
         */
        private boolean isEmpty(String str) {
            return str == null || str.trim().length() == 0;
        }

        /**
         * Validates all required input fields
         * @return true if all fields are valid, false otherwise
         */
        public boolean validateAllFields() {
            boolean allFieldsFilled = true;

            // Clear previous errors
            fullNameError = null;
            usernameError = null;
            bioError = null;
            dobError = null;
            genderError = null;

            // Check fields for emptiness
            if (isEmpty(fullName)) {
                fullNameError = "Required!";
                allFieldsFilled = false;
            }

            if (isEmpty(username)) {
                usernameError = "Required!";
                allFieldsFilled = false;
            }

            if (isEmpty(bio)) {
                bioError = "Required!";
                allFieldsFilled = false;
            }

            if (isEmpty(dateOfBirth)) {
                dobError = "Required!";
                allFieldsFilled = false;
            }

            if (isEmpty(gender) || gender.equals("Select Gender")) {
                genderError = "Required!";
                allFieldsFilled = false;
            }

            return allFieldsFilled;
        }

        /**
         * Handles the continue button click
         */
        public void handleContinueButtonClick() {
            if (validateAllFields()) {
                // Create user data HashMap
                saveUserDataToDatabase();
            } else {
                toastMessage = "Please fill in all required fields!";
            }
        }

        /**
         * Simulates saving user data to Firestore
         */
        public void saveUserDataToDatabase() {
            // Create user data map
            savedUserData = new HashMap<>();
            savedUserData.put("fullName", fullName);
            savedUserData.put("username", username);
            savedUserData.put("bio", bio);
            savedUserData.put("dateOfBirth", dateOfBirth);
            savedUserData.put("gender", gender);
            savedUserData.put("email", email);

            if (imageUri != null) {
                savedUserData.put("hasProfilePic", true);
                savedUserData.put("profilePicUri", imageUri);
                savedUserData.put("profilePicSizeKB", fileSizeKB);
            } else {
                savedUserData.put("hasProfilePic", false);
            }

            if (databaseSaveSuccessful) {
                toastMessage = "Profile created successfully!";
                navigatedTo = "MainActivity";
                finishCalled = true;
            } else {
                toastMessage = "Error creating profile";
            }
        }

        /**
         * Simulates showing date picker and setting a date
         */
        public void selectDate(String date) {
            dateOfBirth = date;
        }

        /**
         * Simulates taking a photo with camera
         */
        public void takePhoto(String imageUriString, long size) {
            if (cameraPermissionGranted) {
                setImageData(imageUriString, size);
            } else {
                toastMessage = "Camera permission denied";
            }
        }

        /**
         * Simulates picking an image from gallery
         */
        public void pickImageFromGallery(String imageUriString, long size) {
            if (storagePermissionGranted) {
                setImageData(imageUriString, size);
            } else {
                toastMessage = "Storage permission denied";
            }
        }

        /**
         * Sets image data and updates UI visibility
         */
        private void setImageData(String imageUriString, long size) {
            imageUri = imageUriString;
            fileSizeKB = size;
            profilePicturePlaceholderVisible = false;
            profilePictureSelectedVisible = true;
        }

        /**
         * Grants camera permission
         */
        public void grantCameraPermission() {
            cameraPermissionGranted = true;
            toastMessage = "Camera permission granted";
        }

        /**
         * Grants storage permission
         */
        public void grantStoragePermission() {
            storagePermissionGranted = true;
        }
    }

    private UserDetailsLogic userDetailsLogic;

    @Before
    public void setUp() {
        userDetailsLogic = new UserDetailsLogic();
        // Set default values
        userDetailsLogic.email = "test@example.com";
    }

    /**
     * Test validation when all fields are filled correctly
     */
    @Test
    public void testValidInputs() {
        // Setup with valid inputs
        userDetailsLogic.fullName = "John Doe";
        userDetailsLogic.username = "johndoe123";
        userDetailsLogic.bio = "This is my bio";
        userDetailsLogic.dateOfBirth = "01/01/1990";
        userDetailsLogic.gender = "Male";

        // Validate
        boolean result = userDetailsLogic.validateAllFields();

        // Verify
        assertTrue("All fields should validate successfully", result);
        assertNull("There should be no fullName error", userDetailsLogic.fullNameError);
        assertNull("There should be no username error", userDetailsLogic.usernameError);
        assertNull("There should be no bio error", userDetailsLogic.bioError);
        assertNull("There should be no dob error", userDetailsLogic.dobError);
        assertNull("There should be no gender error", userDetailsLogic.genderError);
    }

    /**
     * Test validation when fields are empty
     */
    @Test
    public void testEmptyInputs() {
        // Setup with empty inputs
        userDetailsLogic.fullName = "";
        userDetailsLogic.username = "";
        userDetailsLogic.bio = "";
        userDetailsLogic.dateOfBirth = "";
        userDetailsLogic.gender = "Select Gender";

        // Validate
        boolean result = userDetailsLogic.validateAllFields();

        // Verify
        assertFalse("Validation should fail with empty inputs", result);
        assertEquals("Required!", userDetailsLogic.fullNameError);
        assertEquals("Required!", userDetailsLogic.usernameError);
        assertEquals("Required!", userDetailsLogic.bioError);
        assertEquals("Required!", userDetailsLogic.dobError);
        assertEquals("Required!", userDetailsLogic.genderError);
    }

    /**
     * Test handling continue button click with valid inputs
     */
    @Test
    public void testContinueButtonWithValidInputs() {
        // Setup with valid inputs
        userDetailsLogic.fullName = "John Doe";
        userDetailsLogic.username = "johndoe123";
        userDetailsLogic.bio = "This is my bio";
        userDetailsLogic.dateOfBirth = "01/01/1990";
        userDetailsLogic.gender = "Male";

        // Simulate button click
        userDetailsLogic.handleContinueButtonClick();

        // Verify
        assertEquals("Profile created successfully!", userDetailsLogic.toastMessage);
        assertEquals("MainActivity", userDetailsLogic.navigatedTo);
        assertTrue("Activity should finish", userDetailsLogic.finishCalled);

        // Check saved data
        assertNotNull("User data should be saved", userDetailsLogic.savedUserData);
        assertEquals("John Doe", userDetailsLogic.savedUserData.get("fullName"));
        assertEquals("johndoe123", userDetailsLogic.savedUserData.get("username"));
        assertEquals("This is my bio", userDetailsLogic.savedUserData.get("bio"));
        assertEquals("01/01/1990", userDetailsLogic.savedUserData.get("dateOfBirth"));
        assertEquals("Male", userDetailsLogic.savedUserData.get("gender"));
        assertEquals("test@example.com", userDetailsLogic.savedUserData.get("email"));
        assertEquals(false, userDetailsLogic.savedUserData.get("hasProfilePic"));
    }

    /**
     * Test handling continue button click with invalid inputs
     */
    @Test
    public void testContinueButtonWithInvalidInputs() {
        // Setup with empty inputs
        userDetailsLogic.fullName = "";
        userDetailsLogic.username = "";
        userDetailsLogic.bio = "";
        userDetailsLogic.dateOfBirth = "";
        userDetailsLogic.gender = "Select Gender";

        // Simulate button click
        userDetailsLogic.handleContinueButtonClick();

        // Verify
        assertEquals("Please fill in all required fields!", userDetailsLogic.toastMessage);
        assertNull("Should not navigate anywhere", userDetailsLogic.navigatedTo);
        assertFalse("Activity should not finish", userDetailsLogic.finishCalled);
        assertNull("User data should not be saved", userDetailsLogic.savedUserData);
    }

    /**
     * Test database save failure
     */
    @Test
    public void testDatabaseSaveFailure() {
        // Setup with valid inputs but database failure
        userDetailsLogic.fullName = "John Doe";
        userDetailsLogic.username = "johndoe123";
        userDetailsLogic.bio = "This is my bio";
        userDetailsLogic.dateOfBirth = "01/01/1990";
        userDetailsLogic.gender = "Male";
        userDetailsLogic.databaseSaveSuccessful = false;

        // Simulate button click
        userDetailsLogic.handleContinueButtonClick();

        // Verify
        assertEquals("Error creating profile", userDetailsLogic.toastMessage);
        assertNull("Should not navigate anywhere", userDetailsLogic.navigatedTo);
        assertFalse("Activity should not finish", userDetailsLogic.finishCalled);
    }

    /**
     * Test date selection
     */
    @Test
    public void testDateSelection() {
        // Simulate date selection
        userDetailsLogic.selectDate("15/06/2000");

        // Verify
        assertEquals("15/06/2000", userDetailsLogic.dateOfBirth);
    }

    /**
     * Test photo capture with permission
     */
    @Test
    public void testTakePhotoWithPermission() {
        // Grant permission
        userDetailsLogic.grantCameraPermission();

        // Simulate taking photo
        userDetailsLogic.takePhoto("content://media/photo.jpg", 1024);

        // Verify
        assertEquals("content://media/photo.jpg", userDetailsLogic.imageUri);
        assertEquals(1024, userDetailsLogic.fileSizeKB);
        assertFalse("Placeholder should be hidden", userDetailsLogic.profilePicturePlaceholderVisible);
        assertTrue("Selected image should be visible", userDetailsLogic.profilePictureSelectedVisible);
    }

    /**
     * Test photo capture without permission
     */
    @Test
    public void testTakePhotoWithoutPermission() {
        // Do not grant permission

        // Simulate taking photo
        userDetailsLogic.takePhoto("content://media/photo.jpg", 1024);

        // Verify
        assertNull("Image URI should not be set", userDetailsLogic.imageUri);
        assertEquals("Camera permission denied", userDetailsLogic.toastMessage);
        assertTrue("Placeholder should still be visible", userDetailsLogic.profilePicturePlaceholderVisible);
        assertFalse("Selected image should not be visible", userDetailsLogic.profilePictureSelectedVisible);
    }

    /**
     * Test saving data with profile picture
     */
    @Test
    public void testSaveWithProfilePicture() {
        // Setup with valid inputs and profile picture
        userDetailsLogic.fullName = "John Doe";
        userDetailsLogic.username = "johndoe123";
        userDetailsLogic.bio = "This is my bio";
        userDetailsLogic.dateOfBirth = "01/01/1990";
        userDetailsLogic.gender = "Male";
        userDetailsLogic.grantCameraPermission();
        userDetailsLogic.takePhoto("content://media/photo.jpg", 1024);

        // Simulate button click
        userDetailsLogic.handleContinueButtonClick();

        // Verify
        assertNotNull("User data should be saved", userDetailsLogic.savedUserData);
        assertEquals(true, userDetailsLogic.savedUserData.get("hasProfilePic"));
        assertEquals("content://media/photo.jpg", userDetailsLogic.savedUserData.get("profilePicUri"));
        assertEquals(1024L, userDetailsLogic.savedUserData.get("profilePicSizeKB"));
    }
}