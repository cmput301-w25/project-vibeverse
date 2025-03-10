package com.example.vibeverse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

/**
 * These tests verify the redirection logic based on the authentication state
 * and Firestore data:
 * - No user logged in -> redirect to Login.
 * - User logged in without a Firestore doc -> redirect to UserDetails.
 * - User logged in with a Firestore doc -> redirect to HomePage.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUITest {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Before
    public void setUp() {
        Intents.init();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    /**
     * Test: No user is logged in.
     * Expected: MainActivity should redirect to Login.
     */
    @Test
    public void testNoUserRedirectsToLogin() throws InterruptedException {

        mAuth.signOut();

        ActivityScenario.launch(MainActivity.class);

        intended(hasComponent(Login.class.getName()));
    }

    /**
     * Test: A user is logged in, but no Firestore document exists.
     * Expected: MainActivity should redirect to UserDetails.
     */
    @Test
    public void testLoggedInNoDocRedirectsToUserDetails() throws InterruptedException {

        mAuth.signOut();

        mAuth.signInWithEmailAndPassword("test_no_doc@example.com", "password")
                .addOnCompleteListener(task -> {

                });
        Thread.sleep(2000);

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).delete();
        }
        Thread.sleep(2000);


        ActivityScenario.launch(MainActivity.class);

        intended(hasComponent(UserDetails.class.getName()));
    }

    /**
     * Test: A user is logged in and a Firestore document exists.
     * Expected: MainActivity should redirect to HomePage.
     */
    @Test
    public void testLoggedInWithDocRedirectsToHomePage() throws InterruptedException {

        mAuth.signOut();

        mAuth.signInWithEmailAndPassword("test_with_doc@example.com", "password")
                .addOnCompleteListener(task -> {

                });
        Thread.sleep(2000);

        if (mAuth.getCurrentUser() != null) {
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid)
                    .set(new UserProfile("Test User", "testuser", "Test Bio"));
        }
        Thread.sleep(2000);

        ActivityScenario.launch(MainActivity.class);

        intended(hasComponent(HomePage.class.getName()));
    }

    public static class UserProfile {
        public String fullName;
        public String username;
        public String bio;

        public UserProfile() {
        }

        public UserProfile(String fullName, String username, String bio) {
            this.fullName = fullName;
            this.username = username;
            this.bio = bio;
        }
    }
}
