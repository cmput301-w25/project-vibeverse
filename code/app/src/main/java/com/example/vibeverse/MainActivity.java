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

public class MainActivity extends AppCompatActivity {


    FirebaseAuth auth;
    Button button;
    TextView textView;
    FirebaseUser user;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
//        button = findViewById(R.id.logout_button);
//        textView = findViewById(R.id.userDetails);
        user = auth.getCurrentUser();

        if (user != null) {
            // Check if user exists in Firestore
            db.collection("users").document(user.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // User details exist, show main activity
                                Intent intent = new Intent(getApplicationContext(), ProfilePage.class);
                                startActivity(intent);
                                finish();
//                                String userDetails = "User ID: " + user.getUid() + "\nEmail: " + user.getEmail();
//                                textView.setText(userDetails);
                            } else {
                                // User details don't exist, redirect to user details activity
                                Intent intent = new Intent(getApplicationContext(), UserDetails.class);
                                startActivity(intent);
                                finish();
                            }
                        }
                    });
        } else {
            Intent intent = new Intent(getApplicationContext(), Login.class);
            startActivity(intent);
            finish();
        }

//        button.setOnClickListener(view -> {
//            auth.signOut();
//            Intent intent = new Intent(getApplicationContext(), Login.class);
//            startActivity(intent);
//            finish();
//
//        });
    }
}