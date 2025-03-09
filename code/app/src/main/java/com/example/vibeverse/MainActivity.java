package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * MainActivity serves as the entry point of the application.
 * <p>
 * It checks whether a user is authenticated via FirebaseAuth. If the user is already signed in,
 * it verifies whether the user details exist in Firestore. If the details exist, the user is redirected
 * to the HomePage; otherwise, the user is sent to the UserDetails activity. If no user is authenticated,
 * the activity redirects to the Login activity.
 * </p>
 * <p>
 * The activity also provides a logout button that signs out the current user.
 * </p>
 */
public class MainActivity extends AppCompatActivity {

    /** FirebaseAuth instance for user authentication. */
    FirebaseAuth auth;
    /** Button to sign out the current user. */
    Button button;
    /** TextView to display user details (for debugging or informational purposes). */
    TextView textView;
    /** The currently authenticated FirebaseUser. */
    FirebaseUser user;
    /** FirebaseFirestore instance for database operations. */
    FirebaseFirestore db;

    /**
     * Called when the activity is first created.
     * <p>
     * This method initializes FirebaseAuth and Firestore, retrieves the current user, and checks
     * if the user exists in the "users" collection in Firestore. Based on this check, the activity
     * redirects the user to HomePage or UserDetails. If no user is authenticated, it redirects to Login.
     * It also sets up a logout button which signs out the user when clicked.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down,
     *                           this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize FirebaseAuth and Firestore
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

//        button = findViewById(R.id.logout_button);
//        textView = findViewById(R.id.userDetails);


        user = auth.getCurrentUser();

        if (user != null) {
            // Check if user details exist in Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // User details exist, navigate to HomePage
                                Intent intent = new Intent(getApplicationContext(), HomePage.class);
                                startActivity(intent);
                                finish();
                            } else {
                                // User details do not exist, navigate to UserDetails activity
                                Intent intent = new Intent(getApplicationContext(), UserDetails.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        } else {
            // No user is authenticated, navigate to Login activity
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }


        // Set up logout button click listener
        button.setOnClickListener(view -> {
            auth.signOut();
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        });

    }
}