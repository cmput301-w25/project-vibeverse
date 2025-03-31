package com.example.vibeverse;

import static android.content.ContentValues.TAG;

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
 * Login activity handles user authentication using Firebase.
 * <p>
 * This activity provides input fields for the user to enter an email and password.
 * If the user is already authenticated, it redirects them either to HomePage
 * or UserDetails (if additional user information is needed).
 * On login attempt, it validates inputs and uses FirebaseAuth to sign in.
 * </p>
 */
public class Login extends AppCompatActivity {

    /** Input field for the user's email address and password. */
    EditText editTextEmail, editTextPassword;
    /** Button that triggers the login process. */
    Button buttonLogin;
    /** Firebase authentication instance. */
    MaterialButton googleSignInButton;

    FirebaseAuth mAuth;
    /** Progress bar shown during the login process. */
    ProgressBar progressBar;
    /** TextView that directs the user to registration if needed. */
    TextView textViewLogin;
    CredentialManager credentialManager;

    /**
     * Called when the activity is starting.
     * <p>
     * If the user is already authenticated, this method checks if user details
     * exist in Firestore. If so, it redirects to HomePage; otherwise, it opens
     * the UserDetails activity. Finally, it finishes this activity.
     * </p>
     */
    @Override
    public void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (task.getResult().exists()) {
                                startActivity(new Intent(getApplicationContext(), HomePage.class));
                            } else {
                                Intent intent = new Intent(getApplicationContext(), UserDetails.class);
                                intent.putExtra("source", "login");
                                startActivity(intent); startActivity(new Intent(getApplicationContext(), UserDetails.class));
                            }
                            finish();
                        }
                    });
        }
    }

    /**
     * Called when the activity is first created.
     * <p>
     * Sets up the UI components, including email and password input fields,
     * login button, progress bar, and a link to the registration screen.
     * Also sets up the click listeners for logging in and navigating to registration.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        credentialManager = CredentialManager.create(this);


        // Initialize UI components
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.login_button);
        progressBar = findViewById(R.id.progress_bar);
        textViewLogin = findViewById(R.id.registerNow);
        googleSignInButton = findViewById(R.id.google_sign_in);



        // Set click listener to navigate to the registration activity
        textViewLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), Register.class);
                startActivity(intent);
                finish();
            }
        });

        // Set click listener for login button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                String email = String.valueOf(editTextEmail.getText());
                String password = String.valueOf(editTextPassword.getText());

                // Validate email input
                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(Login.this, "Please enter your email", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.GONE);
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this, "Please enter your password", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Attempt to sign in using FirebaseAuth
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                progressBar.setVisibility(View.GONE);
                                if (task.isSuccessful()) {
                                    Toast.makeText(Login.this, "Login successful.",
                                            Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), HomePage.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    String errorMessage = "Authentication failed";
                                    Exception exception = task.getException();
                                    if (exception != null) {
                                        errorMessage = exception.getMessage();
                                    }
                                    Toast.makeText(Login.this, errorMessage, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });

        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });
    }

    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);

        // Create Google sign-in request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(getString(R.string.default_web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Launch Google Sign-in
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleSignIn(result.getCredential());
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Failed to get credentials", e);
                            Toast.makeText(Login.this, "Google Sign-In failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void handleSignIn(Credential credential) {
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            // Create Google ID Token
            android.os.Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign in to Firebase using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            progressBar.setVisibility(View.GONE);
            Log.w(TAG, "Credential is not of type Google ID!");
            Toast.makeText(Login.this, "Invalid credential type", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
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
                                            // User exists in Firestore, go to home
                                            startActivity(new Intent(getApplicationContext(), HomePage.class));
                                        } else {
                                            // New user, go to user details
                                            Intent intent = new Intent(getApplicationContext(), UserDetails.class);
                                            intent.putExtra("source", "login");
                                            startActivity(intent);

                                        }
                                        finish();
                                    }
                                });
                    } else {
                        // If sign in fails, display a message to the user
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(Login.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}