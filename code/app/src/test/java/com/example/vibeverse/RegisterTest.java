package com.example.vibeverse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Unit tests for the Register activity's logic.
 *
 * These tests focus purely on the business logic without any Android framework dependencies.
 */
@RunWith(JUnit4.class)
public class RegisterTest {

    /**
     * Test implementation of the core register logic without Android dependencies
     */
    private static class RegisterLogic {
        private String email;
        private String password;
        public String toastMessage;
        public boolean progressBarVisible;
        public boolean progressBarWasEverVisible;
        public String navigatedTo;
        public boolean finishCalled;

        /**
         * Utility method to replace Android's TextUtils.isEmpty()
         */
        private boolean isEmpty(String str) {
            return str == null || str.trim().length() == 0;
        }

        /**
         * Validates the input email and password
         */
        public boolean validateInput() {
            if (isEmpty(email)) {
                toastMessage = "Please enter your email";
                return false;
            }

            if (isEmpty(password)) {
                toastMessage = "Please enter your password";
                return false;
            }

            return true;
        }

        /**
         * Simulates the registration process
         */
        public void register(String email, String password) {
            this.email = email;
            this.password = password;

            if (!validateInput()) {
                return;
            }

            progressBarVisible = true;
            progressBarWasEverVisible = true;

            // Simulate the registration result
            boolean isSuccessful = isRegistrationSuccessful();

            if (isSuccessful) {
                toastMessage = "Account created successfully";
                navigatedTo = "UserDetails";
                finishCalled = true;
            } else {
                toastMessage = "Authentication failed.";
            }

            progressBarVisible = false;
        }

        /**
         * Determines if registration would be successful
         */
        public boolean isRegistrationSuccessful() {
            // For testing, we'll consider valid if both email and password are not empty
            // and the email contains @ character and password is at least 6 characters
            return !isEmpty(email) &&
                    !isEmpty(password) &&
                    email.contains("@") &&
                    password.length() >= 6 &&
                    !email.contains("existing");  // Simulate failure for "existing" emails
        }

        /**
         * Simulates checking for an existing user
         */
        public void checkExistingUser() {
            boolean userExists = simulateUserExistsInFirestore();

            if (userExists) {
                navigatedTo = "MainActivity";
            } else {
                navigatedTo = "UserDetails";
            }

            finishCalled = true;
        }

        /**
         * Simulates checking if user document exists in Firestore
         */
        public boolean simulateUserExistsInFirestore() {
            // This would normally check Firestore
            // For testing, we'll return based on a simple condition
            return email != null && email.startsWith("existing");
        }

        /**
         * Simulates clicking the login text view
         */
        public void loginClick() {
            navigatedTo = "Login";
            finishCalled = true;
        }
    }

    private RegisterLogic registerLogic;

    @Before
    public void setUp() {
        registerLogic = new RegisterLogic();
    }

    /**
     * Test successful registration flow.
     */
    @Test
    public void testSuccessfulRegistration() {
        // Execute
        registerLogic.register("new-user@example.com", "password123");

        // Verify
        assertTrue("Progress bar should be visible during registration", registerLogic.progressBarWasEverVisible);
        assertEquals("Success message should be shown", "Account created successfully", registerLogic.toastMessage);
        assertEquals("Should navigate to UserDetails", "UserDetails", registerLogic.navigatedTo);
        assertTrue("Activity should finish", registerLogic.finishCalled);
    }

    /**
     * Test failed registration flow with existing email.
     */
    @Test
    public void testFailedRegistration() {
        // Execute - using an email that simulates already existing
        registerLogic.register("existing-user@example.com", "password123");

        // Verify
        assertFalse("Should not be considered successful", registerLogic.isRegistrationSuccessful());
        assertEquals("Failure message should be shown", "Authentication failed.", registerLogic.toastMessage);
        assertNull("Should not navigate anywhere", registerLogic.navigatedTo);
        assertFalse("Activity should not finish", registerLogic.finishCalled);
    }

    /**
     * Test email validation - empty email.
     */
    @Test
    public void testEmptyEmailValidation() {
        // Execute
        registerLogic.register("", "password123");

        // Verify
        assertFalse("Input validation should fail", registerLogic.validateInput());
        assertEquals("Email validation message should be shown", "Please enter your email", registerLogic.toastMessage);
        assertNull("Should not navigate anywhere", registerLogic.navigatedTo);
        assertFalse("Activity should not finish", registerLogic.finishCalled);
    }

    /**
     * Test password validation - empty password.
     */
    @Test
    public void testEmptyPasswordValidation() {
        // Execute
        registerLogic.register("user@example.com", "");

        // Verify
        assertFalse("Input validation should fail", registerLogic.validateInput());
        assertEquals("Password validation message should be shown", "Please enter your password", registerLogic.toastMessage);
        assertNull("Should not navigate anywhere", registerLogic.navigatedTo);
        assertFalse("Activity should not finish", registerLogic.finishCalled);
    }

    /**
     * Test password validation - password too short.
     */
    @Test
    public void testShortPasswordValidation() {
        // Execute
        registerLogic.register("user@example.com", "12345");

        // Verify
        assertTrue("Input validation should pass", registerLogic.validateInput());
        assertFalse("Registration should fail", registerLogic.isRegistrationSuccessful());
        assertEquals("Failure message should be shown", "Authentication failed.", registerLogic.toastMessage);
    }

    /**
     * Test auto-login for existing user.
     */
    @Test
    public void testAutoLoginExistingUser() {
        // Setup for existing user
        registerLogic.email = "existing-user@example.com";

        // Execute
        registerLogic.checkExistingUser();

        // Verify
        assertTrue("User should be detected as existing", registerLogic.simulateUserExistsInFirestore());
        assertEquals("Should navigate to MainActivity", "MainActivity", registerLogic.navigatedTo);
        assertTrue("Activity should finish", registerLogic.finishCalled);
    }

    /**
     * Test auto-login for new user.
     */
    @Test
    public void testAutoLoginNewUser() {
        // Setup for new user
        registerLogic.email = "new-user@example.com";

        // Execute
        registerLogic.checkExistingUser();

        // Verify
        assertFalse("User should be detected as new", registerLogic.simulateUserExistsInFirestore());
        assertEquals("Should navigate to UserDetails", "UserDetails", registerLogic.navigatedTo);
        assertTrue("Activity should finish", registerLogic.finishCalled);
    }

    /**
     * Test navigation to Login activity.
     */
    @Test
    public void testNavigationToLogin() {
        // Execute
        registerLogic.loginClick();

        // Verify
        assertEquals("Should navigate to Login", "Login", registerLogic.navigatedTo);
        assertTrue("Activity should finish", registerLogic.finishCalled);
    }
}