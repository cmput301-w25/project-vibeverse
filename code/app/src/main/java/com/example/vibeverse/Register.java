package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.Executors;

/**
 * Register activity handles user registration using Firebase Authentication.
 * <p>
 * The activity provides fields for the user to enter an email and password,
 * and a button to create a new account. It also includes a link to navigate
 * to the Login screen if the user already has an account.
 * </p>
 */

public class Register extends AppCompatActivity {

    private static final String TAG = "GoogleSignUp";
   /** Input field for the user's email address. */
    EditText editTextEmail;
   /** Input field for the user's password. */
    EditText editTextPassword;
   /** Button to register a new account. */
    Button buttonRegister;
    MaterialButton googleSignUpButton;
    /** FirebaseAuth instance for handling authentication. */
    FirebaseAuth mAuth;
    /** ProgressBar displayed during registration. */
    ProgressBar progressBar;
    /** TextView that provides a link to the login screen. */
    TextView textViewLogin;
    CredentialManager credentialManager;

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
        credentialManager = CredentialManager.create(this);
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonRegister = findViewById(R.id.register_button);
        progressBar = findViewById(R.id.progress_bar);
        textViewLogin = findViewById(R.id.loginNow);
        googleSignUpButton = findViewById(R.id.google_sign_up);

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
                                    String errorMessage = "Authentication failed";
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        errorMessage = exception.getMessage();
                                    }
                                    Toast.makeText(Register.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        googleSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signUpWithGoogle();
            }
        });
    }

    private void signUpWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);

        // Create Google sign-up request - set filterByAuthorizedAccounts to false for sign-up
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Show all accounts for sign-up
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Launch Google Sign-up
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignUp(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Failed to get credentials", e);
                            Toast.makeText(Register.this, "Google Sign-Up failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void handleSignUp(Credential credential) {
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            // Create Google ID Token
            android.os.Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign up to Firebase using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                String credentialType = credential != null ? credential.getType() : "null";
                Log.w(TAG, "Credential is not of type Google ID! Actual type: " + credentialType);
                Toast.makeText(Register.this, "Invalid credential type", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            // Check if user exists in Firestore
                            FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(user.getUid())
                                    .get()
                                    .addOnCompleteListener(firestoreTask -> {
                                        if (firestoreTask.isSuccessful()) {
                                            if (firestoreTask.getResult().exists()) {
                                                // User exists in Firestore, go to profile
                                                startActivity(new Intent(getApplicationContext(), ProfilePage.class));
                                            } else {
                                                // New user, go to user details
                                                startActivity(new Intent(getApplicationContext(), UserDetails.class));
                                            }
                                            finish();
                                        }
                                    });
                        } else {
                            // If sign in fails, display a message to the user
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            String errorMessage = "Authentication failed";
                            Exception exception = task.getException();
                            if (exception != null) {
                                errorMessage = exception.getMessage();
                            }
                            Toast.makeText(Register.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
                });
    }
}