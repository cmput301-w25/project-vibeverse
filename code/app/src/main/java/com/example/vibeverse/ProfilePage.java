package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfilePage extends AppCompatActivity implements FilterDialog.FilterListener {

    private static final String TAG = "ProfilePage";
    public static final int EDIT_MOOD_REQUEST_CODE = 1001;


    private RecyclerView recyclerFeed;
    private Button buttonFilter;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> allMoodEvents;
    private EditText editSearch;

    private TextView textName, textUsername, textBioContent;
    private ImageView profilePicture;

    private Button logoutButton;
    private BottomNavigationView bottomNavigationView;
    private View emptyStateView;

    // Add these as class fields
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;
    private ProgressBar progressLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID or use a device ID if not logged in
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);

            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }


        }

        // **Find your TextViews & ImageView from XML**
        textName = findViewById(R.id.textName);
        textUsername = findViewById(R.id.textUsername);
        textBioContent = findViewById(R.id.textBioContent);
        profilePicture = findViewById(R.id.profilePicture);

        // Then call a helper method to load the profile
        loadUserProfile();
        // Logout button
        logoutButton = findViewById(R.id.buttonLogout);

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Intent intent = new Intent(ProfilePage.this, Login.class);
                startActivity(intent);
                finish();
            }
        });

        // Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        recyclerFeed = findViewById(R.id.recyclerFeed);
        ImageButton buttonFilter = findViewById(R.id.buttonFilter);
        emptyStateView = findViewById(R.id.emptyStateView);

        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);
        editSearch = findViewById(R.id.editSearch);

        progressLoading = findViewById(R.id.progressLoading);
        if (progressLoading == null) {
            // If progress bar doesn't exist in the layout yet, create one programmatically
            progressLoading = new ProgressBar(this);
            progressLoading.setId(View.generateViewId());
            progressLoading.setVisibility(View.GONE);

            // Add to layout - adapt this according to your actual layout structure
            ViewGroup parent = (ViewGroup) recyclerFeed.getParent();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            parent.addView(progressLoading, params);
        }

        // Initialize empty lists
        allMoodEvents = new ArrayList<MoodEvent>();
        moodEventAdapter = new MoodEventAdapter(ProfilePage.this, new ArrayList<MoodEvent>());
        recyclerFeed.setAdapter(moodEventAdapter);

        // Load data from Firestore instead of using dummy data
        loadMoodsFromFirestore();

        buttonFilter.setOnClickListener(v -> FilterDialog.show(ProfilePage.this, this));

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                moodEventAdapter.filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void loadMoodsFromFirestore() {
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        if (recyclerFeed != null) {
            recyclerFeed.setVisibility(View.VISIBLE);
        }

        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }

        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allMoodEvents.clear();
                    SimpleDateFormat sourceFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            // Create a MoodEvent from the Firestore data

                            MoodEvent moodEvent = MoodEvent.fromMap(document.getData());

                            // Parse the timestamp to a Date object
                            Date date = new Date();
                            try {
                                if (moodEvent.getTimestamp() != null) {
                                    date = sourceFormat.parse(moodEvent.getTimestamp());
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }

                            moodEvent.setDate(date);

                            // Store the document ID for Firestore operations
                            moodEvent.setDocumentId(document.getId());

                            // Add subtitle using trigger and social situation
                            StringBuilder subtitle = new StringBuilder();

                            if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
                                subtitle.append("Trigger: ").append(moodEvent.getTrigger());
                            }

                            if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
                                if (subtitle.length() > 0) {
                                    subtitle.append(" | ");
                                }
                                subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                            }

                            moodEvent.setSubtitle(subtitle.toString());

                            // Add to the list
                            allMoodEvents.add(moodEvent);
                        } catch (Exception e) {
                            Log.e("ProfilePage", "Error parsing mood data: " + e.getMessage());
                        }
                    }

                    // Update the adapter with the new data
                    moodEventAdapter.updateMoodEvents(new ArrayList<>(allMoodEvents));

                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }

                    // Show empty state if no posts
                    if (allMoodEvents.isEmpty()) {
                        if (emptyStateView != null) {
                            recyclerFeed.setVisibility(View.GONE);
                            emptyStateView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(ProfilePage.this, "No mood entries found. Add one!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this, "Error loading moods: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void loadUserProfile() {
        // Make sure you have the correct path: "users" -> document(userId)
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Read the fields from the Firestore document
                        String fullName = documentSnapshot.getString("fullName");
                        String username = documentSnapshot.getString("username");
                        String bio = documentSnapshot.getString("bio");
                        String profilePicUri = documentSnapshot.getString("profilePicUri");

                        // Populate the TextViews
                        if (fullName != null) textName.setText(fullName);
                        if (username != null) textUsername.setText(username);
                        if (bio != null) textBioContent.setText(bio);

                        // If you have a profile picture URL, load it using Glide (or Picasso).
                        if (profilePicUri != null && !profilePicUri.isEmpty()) {
                            // Make sure you have Glide in your Gradle dependencies
                            // implementation 'com.github.bumptech.glide:glide:4.14.2'
                            Glide.with(ProfilePage.this)
                                    .load(profilePicUri)
                                    .placeholder(R.drawable.user_icon) // fallback placeholder
                                    .error(R.drawable.user_icon)       // error placeholder
                                    .into(profilePicture);
                        }
                    } else {
                        Toast.makeText(ProfilePage.this, "User profile does not exist.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfilePage.this, "Failed to load user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload data when returning to this screen
        loadMoodsFromFirestore();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EDIT_MOOD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Get updated values
            String updatedMood = data.getStringExtra("updatedMood");
            String updatedEmoji = data.getStringExtra("updatedEmoji");
            String updatedTrigger = data.getStringExtra("updatedTrigger");
            String updatedReasonWhy = data.getStringExtra("updatedReasonWhy");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);
            String timestamp = data.getStringExtra("timestamp");
            int moodPosition = data.getIntExtra("moodPosition", -1);
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");

            if (moodPosition >= 0 && moodPosition < allMoodEvents.size()) {
                // Get the post to update
                MoodEvent moodEventToUpdate = allMoodEvents.get(moodPosition);

                // Update Firestore
                updateMoodInFirestore(moodEventToUpdate.getDocumentId(), updatedEmoji, updatedMood, updatedTrigger,
                        updatedReasonWhy, updatedSocialSituation, updatedIntensity, updatedPhotoUri);;
            }





        }
    }

    private void updateMoodInFirestore(String documentId, String emoji, String mood,
                                       String trigger, String reasonWhy, String socialSituation,
                                       int intensity, String photoUri) {
        // Show loading indicator
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        // Create a map with updated values
        Map<String, Object> updatedMood = new HashMap<>();
        updatedMood.put("emoji", emoji);
        updatedMood.put("mood", mood);
        updatedMood.put("emotionalState", emoji + " " + mood);
        updatedMood.put("trigger", trigger);
        updatedMood.put("socialSituation", socialSituation);
        updatedMood.put("intensity", intensity);
        updatedMood.put("reasonWhy", reasonWhy);

        // Handle photo if it exists
        if (photoUri != null && !photoUri.equals("N/A")) {
            updatedMood.put("hasPhoto", true);
            updatedMood.put("photoUri", photoUri);
        } else {
            updatedMood.put("hasPhoto", false);
        }

        // Update the document in Firestore
        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .document(documentId)
                .update(updatedMood)
                .addOnSuccessListener(aVoid -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this, "Mood updated successfully", Toast.LENGTH_SHORT).show();
                    loadMoodsFromFirestore(); // Reload data to reflect changes
                })
                .addOnFailureListener(e -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this, "Error updating mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    public void deleteMoodFromFirestore(String documentId, int position) {
        // Show a confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Delete Mood")
                .setMessage("Are you sure you want to delete this mood entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Show loading
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.VISIBLE);
                    }

                    // Delete from Firestore
                    db.collection("Usermoods")
                            .document(userId)
                            .collection("moods")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Remove from local list
                                allMoodEvents.remove(position);
                                moodEventAdapter.updateMoodEvents(new ArrayList<>(allMoodEvents));

                                if (progressLoading != null) {
                                    progressLoading.setVisibility(View.GONE);
                                }

                                Toast.makeText(ProfilePage.this, "Mood deleted successfully", Toast.LENGTH_SHORT).show();

                                // Check if list is now empty
                                if (allMoodEvents.isEmpty() && emptyStateView != null) {
                                    recyclerFeed.setVisibility(View.GONE);
                                    emptyStateView.setVisibility(View.VISIBLE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (progressLoading != null) {
                                    progressLoading.setVisibility(View.GONE);
                                }
                                Toast.makeText(ProfilePage.this, "Error deleting mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

    @Override
    public void onFilterApplied(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused) {
        applyFilters(timeFilter, isHappy, isSad, isAfraid, isConfused);
    }

    private void applyFilters(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused) {
        List<MoodEvent> filteredMoodEvents = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (MoodEvent moodEvent: allMoodEvents) {
            long moodEventTime = moodEvent.getDate().getTime();
            boolean isWithinTime = false;

            switch (timeFilter) {
                case "last_24_hours":
                    isWithinTime = (currentTime - moodEventTime) <= 86400000;
                    break;
                case "3Days":
                    isWithinTime = (currentTime - moodEventTime) <= 259200000;
                    break;
                case "last_week":
                    isWithinTime = (currentTime - moodEventTime) <= 604800000;
                    break;
                case "last_month":
                    isWithinTime = (currentTime - moodEventTime) <= 2592000000L;
                    break;
                case "all_time":
                    isWithinTime = true;
                    break;
            }

            if (isWithinTime) {
                if ((isHappy && moodEvent.getMoodTitle().equals("HAPPY")) ||
                        (isSad && moodEvent.getMoodTitle().equals("SAD")) ||
                        (isAfraid && moodEvent.getMoodTitle().equals("AFRAID")) ||
                        (isConfused && moodEvent.getMoodTitle().equals("CONFUSED")) ||
                        (!isHappy && !isSad && !isAfraid && !isConfused)) {
                    filteredMoodEvents.add(moodEvent);
                }
            }
        }
        moodEventAdapter.updateMoodEvents(filteredMoodEvents);
    }


}