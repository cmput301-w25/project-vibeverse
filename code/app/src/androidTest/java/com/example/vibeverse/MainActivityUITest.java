package com.example.vibeverse;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.auth.FirebaseAuth;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;

/**
 * Tests redirection logic in MainActivity when no user is logged in.
 * Expected: Redirect to Login activity.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityUITest {

    private FirebaseAuth mAuth;

    @Before
    public void setUp() {
        Intents.init();
        mAuth = FirebaseAuth.getInstance();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testNoUserRedirectsToLogin() {
        mAuth.signOut();

        ActivityScenario.launch(MainActivity.class);

        intended(hasComponent(Login.class.getName()));
    }
}
