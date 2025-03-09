package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Register activity handles user registration using Firebase Authentication.
 * <p>
 * The activity provides fields for the user to enter an email and password,
 * and a button to create a new account. It also includes a link to navigate
 * to the Login screen if the user already has an account.
 * </p>
 */
public class Register extends AppCompatActivity {

    /** Input field for the user's email address. */
    TextInputEditText editTextEmail;
    /** Input field for the user's password. */
    TextInputEditText editTextPassword;
    /** Button to register a new account. */
    Button buttonRegister;
    /** FirebaseAuth instance for handling authentication. */
    FirebaseAuth mAuth;
    /** ProgressBar displayed during registration. */
    ProgressBar progressBar;
    /** TextView that provides a link to the login screen. */
    TextView textViewLogin;

    /**
     * Called when the activity is starting.
     * <p>
     * If a user is already authenticated, this method checks Firestore to see if the user's details exist.
     * If details are found, the user is redirected to MainActivity; otherwise, to UserDetails.
     * </p>
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().exists()) {
                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            } else {
                                startActivity(new Intent(getApplicationContext(), UserDetails.class));
                            }
                            finish();
                        }
                    });
        }
    }

    /**
     * Called when the activity is created.
     * <p>
     * Initializes the UI components for registration, sets up click listeners
     * for registration and navigation to the login screen, and handles user input validation.
     * </p>
     *
     * @param savedInstanceState A Bundle containing the activity's previously frozen state, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        mAuth = FirebaseAuth.getInstance();
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonRegister = findViewById(R.id.register_button);
        progressBar = findViewById(R.id.progress_bar);
        textViewLogin = findViewById(R.id.loginNow);

        // Set click listener to navigate to the Login activity.
        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for the registration button.
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email = String.valueOf(editTextEmail.getText());
                String password = String.valueOf(editTextPassword.getText());

                // Validate email input.
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Register.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Validate password input.
                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Register.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(Register.this, "Account created successfully",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), UserDetails.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // If sign in fails, display a message to the user.
                                    Toast.makeText(Register.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
    }
}