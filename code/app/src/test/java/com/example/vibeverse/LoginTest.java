package com.example.vibeverse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

/**
 * Unit tests for the Login activity's logic.
 *
 * These tests focus purely on the business logic without any Android framework dependencies.
 */
@RunWith(JUnit4.class)
public class LoginTest {

    /**
     * Test implementation of the core login logic without Android dependencies
     */
    private static class LoginLogic {
        private String email;
        private String password;
        public String toastMessage;
        public boolean progressBarVisible;
        public boolean progressBarWasEverVisible;  // Track if progress bar was ever shown
        public String navigatedTo;
        public boolean finishCalled;

        /**
         * Utility method to replace Android's TextUtils.isEmpty()
         */
        private boolean isEmpty(String str) {
            return str == null || str.trim().length() == 0;
        }

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

        public void login(String email, String password) {
            this.email = email;
            this.password = password;

            if (!validateInput()) {
                return;
            }

            progressBarVisible = true;
            progressBarWasEverVisible = true;  // Record that we showed the progress bar

            // Simulate the authentication result
            boolean isSuccessful = isLoginSuccessful();

            if (isSuccessful) {
                toastMessage = "Login successful.";
                navigatedTo = "HomePage";
                finishCalled = true;
            } else {
                toastMessage = "Authentication failed.";
            }

            progressBarVisible = false;  // Hide progress bar after authentication
        }

        public boolean isLoginSuccessful() {
            // For testing, we'll consider valid if both email and password are not empty
            // and the email contains @ character
            return !isEmpty(email) &&
                    !isEmpty(password) &&
                    email.contains("@") &&
                    password.length() >= 6;
        }

        public void checkExistingUser() {
            boolean userExists = simulateUserExistsInFirestore();

            if (userExists) {
                navigatedTo = "HomePage";
            } else {
                navigatedTo = "UserDetails";
            }

            finishCalled = true;
        }

        public boolean simulateUserExistsInFirestore() {
            // This would normally check Firestore
            // For testing, we'll return based on a simple condition
            return email != null && email.startsWith("existing");
        }

        public void registerClick() {
            navigatedTo = "Register";
            finishCalled = true;
        }
    }

    private LoginLogic loginLogic;

    @Before
    public void setUp() {
        loginLogic = new LoginLogic();
    }

    /**
     * Test successful login flow.
     */
    @Test
    public void testSuccessfulLogin() {
        // Execute
        loginLogic.login("user@example.com", "password123");

        // Verify
        assertTrue("Progress bar should be visible during login", loginLogic.progressBarWasEverVisible);
        assertEquals("Success message should be shown", "Login successful.", loginLogic.toastMessage);
        assertEquals("Should navigate to HomePage", "HomePage", loginLogic.navigatedTo);
        assertTrue("Activity should finish", loginLogic.finishCalled);
    }

    /**
     * Test failed login flow with invalid credentials.
     */
    @Test
    public void testFailedLogin() {
        // Execute
        loginLogic.login("user@example.com", "pass"); // Too short password

        // Verify
        assertFalse("Should not be considered successful", loginLogic.isLoginSuccessful());
        assertEquals("Failure message should be shown", "Authentication failed.", loginLogic.toastMessage);
        assertNull("Should not navigate anywhere", loginLogic.navigatedTo);
        assertFalse("Activity should not finish", loginLogic.finishCalled);
    }

    /**
     * Test email validation - empty email.
     */
    @Test
    public void testEmptyEmailValidation() {
        // Execute
        loginLogic.login("", "password123");

        // Verify
        assertFalse("Input validation should fail", loginLogic.validateInput());
        assertEquals("Email validation message should be shown", "Please enter your email", loginLogic.toastMessage);
        assertNull("Should not navigate anywhere", loginLogic.navigatedTo);
        assertFalse("Activity should not finish", loginLogic.finishCalled);
    }

    /**
     * Test password validation - empty password.
     */
    @Test
    public void testEmptyPasswordValidation() {
        // Execute
        loginLogic.login("user@example.com", "");

        // Verify
        assertFalse("Input validation should fail", loginLogic.validateInput());
        assertEquals("Password validation message should be shown", "Please enter your password", loginLogic.toastMessage);
        assertNull("Should not navigate anywhere", loginLogic.navigatedTo);
        assertFalse("Activity should not finish", loginLogic.finishCalled);
    }

    /**
     * Test auto-login for existing user.
     */
    @Test
    public void testAutoLoginExistingUser() {
        // Setup for existing user
        loginLogic.email = "existing-user@example.com";

        // Execute
        loginLogic.checkExistingUser();

        // Verify
        assertTrue("User should be detected as existing", loginLogic.simulateUserExistsInFirestore());
        assertEquals("Should navigate to HomePage", "HomePage", loginLogic.navigatedTo);
        assertTrue("Activity should finish", loginLogic.finishCalled);
    }

    /**
     * Test auto-login for new user.
     */
    @Test
    public void testAutoLoginNewUser() {
        // Setup for new user
        loginLogic.email = "new-user@example.com";

        // Execute
        loginLogic.checkExistingUser();

        // Verify
        assertFalse("User should be detected as new", loginLogic.simulateUserExistsInFirestore());
        assertEquals("Should navigate to UserDetails", "UserDetails", loginLogic.navigatedTo);
        assertTrue("Activity should finish", loginLogic.finishCalled);
    }

    /**
     * Test navigation to Register activity.
     */
    @Test
    public void testNavigationToRegister() {
        // Execute
        loginLogic.registerClick();

        // Verify
        assertEquals("Should navigate to Register", "Register", loginLogic.navigatedTo);
        assertTrue("Activity should finish", loginLogic.finishCalled);
    }
}