package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * ProfilePage displays the user's mood feed and profile-related functionalities.
 * <p>
 * This activity retrieves and displays a list of MoodEvent objects from Firestore.
 * It allows the user to filter the feed using a FilterDialog and perform a simple
 * client-side search via an EditText. It also includes a logout button and bottom
 * navigation for app-wide navigation.
 * </p>
 */
public class ProfilePage extends AppCompatActivity implements FilterDialog.FilterListener {

    /** Request code used when editing a mood event. */
    public static final int EDIT_MOOD_REQUEST_CODE = 1001;
    private static final String TAG = "ProfilePage";

    /** RecyclerView to display the mood feed. */
    private RecyclerView recyclerFeed;
    /** Adapter for the RecyclerView displaying MoodEvent objects. */
    private MoodEventAdapter moodEventAdapter;
    /** List of all MoodEvent objects. */
    private List<MoodEvent> allMoodEvents;
    /** EditText for performing a search within the mood feed. */
    private EditText editSearch;
    /** View displayed when there are no mood entries. */
    private View emptyStateView;
    /** Button to logout the user. */


    private TextView textName, textUsername, textBioContent;
    private ImageView profilePicture;


    private Button logoutButton;
    /** BottomNavigationView for navigating between app sections. */
    private BottomNavigationView bottomNavigationView;
    /** Button to open the FilterDialog. */
    private ImageButton buttonFilter;
    /** ProgressBar indicating loading state. */
    private ProgressBar progressLoading;
    /** Firestore database instance. */
    private FirebaseFirestore db;
    /** Firebase Authentication instance. */
    private FirebaseAuth mAuth;
    /** ID of the current user. */
    private String userId;

    /** Formatter for parsing and formatting timestamps. */
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    /**
     * Called when the activity is created.
     * <p>
     * Initializes UI components, configures RecyclerView and bottom navigation,
     * loads the current mood feed from Firestore, and sets up search and filter functionality.
     * </p>
     *
     * @param savedInstanceState Bundle containing saved state data, if any.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        // Initialize Firebase Auth and Firestore instances.
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            // If not logged in, use a device-based ID as fallback.
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


        // Set up logout button to sign out the user.
        logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfilePage.this, Login.class));
            finish();
        });

        // Set up bottom navigation.
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        // Set up RecyclerView.
        recyclerFeed = findViewById(R.id.recyclerFeed);
        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);

        // Initialize search and filter UI elements.
        editSearch = findViewById(R.id.editSearch);
        buttonFilter = findViewById(R.id.buttonFilter);
        emptyStateView = findViewById(R.id.emptyStateView);
        progressLoading = findViewById(R.id.progressLoading);

        // If progressLoading is not defined in the layout, create it programmatically.
        if (progressLoading == null) {
            progressLoading = new ProgressBar(this);
            progressLoading.setId(View.generateViewId());
            progressLoading.setVisibility(View.GONE);
            ViewGroup parent = (ViewGroup) recyclerFeed.getParent();
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.CENTER;
            parent.addView(progressLoading, params);
        }

        // Initialize the list and adapter for mood events.
        allMoodEvents = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(this, new ArrayList<>());
        recyclerFeed.setAdapter(moodEventAdapter);

        // Load mood events from Firestore.
        loadMoodsFromFirestore();

        // Open the FilterDialog when the filter button is clicked.
        buttonFilter.setOnClickListener(v ->
                FilterDialog.show(ProfilePage.this, ProfilePage.this)
        );

        // Set up search functionality for client-side filtering.
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                moodEventAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Loads mood events from Firestore.
     * <p>
     * Retrieves the "moods" subcollection for the current user, orders by timestamp (descending),
     * and converts each document into a MoodEvent. Updates the adapter and handles empty state UI.
     * </p>
     */
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
                .addOnSuccessListener(snapshots -> {
                    allMoodEvents.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            MoodEvent moodEvent = MoodEvent.fromMap(doc.getData());
                            if (moodEvent.getTimestamp() != null) {
                                Date date = sourceFormat.parse(moodEvent.getTimestamp());
                                if (date != null) {
                                    moodEvent.setDate(date);
                                }
                            }
                            moodEvent.setDocumentId(doc.getId());

                            // Build a small "subtitle" (Trigger, Social, etc.)
                            StringBuilder subtitle = new StringBuilder();
                            if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
                                subtitle.append("Trigger: ").append(moodEvent.getTrigger());
                            }
                            if (moodEvent.getSocialSituation() != null &&
                                    !moodEvent.getSocialSituation().isEmpty()) {
                                if (subtitle.length() > 0) {
                                    subtitle.append(" | ");
                                }
                                subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                            }
                            moodEvent.setSubtitle(subtitle.toString());

                            allMoodEvents.add(moodEvent);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing timestamp", e);
                        }
                    }
                    moodEventAdapter.updateMoodEvents(new ArrayList<>(allMoodEvents));

                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    if (allMoodEvents.isEmpty()) {
                        if (emptyStateView != null) {
                            recyclerFeed.setVisibility(View.GONE);
                            emptyStateView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(ProfilePage.this,
                                    "No mood entries found. Add one!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this,
                            "Error loading moods: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
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


  
    /**
   * Called when the activity resumes.
   * <p>
   * Reloads the mood events from Firestore to update the feed.
   * </p>
   */
    @Override
    public void onResume() {
        super.onResume();
        loadMoodsFromFirestore();
    }

    /**
     * Callback method from FilterDialog.FilterListener.
     * <p>
     * This method is required by the interface but is not used in this implementation.
     * </p>
     *
     * @param timeFilter   The time filter string.
     * @param isHappy      True if "Happy" is selected.
     * @param isSad        True if "Sad" is selected.
     * @param isAngry      True if "Angry" is selected.
     * @param isSurprised  True if "Surprised" is selected.
     * @param isAfraid     True if "Afraid" is selected.
     * @param isDisgusted  True if "Disgusted" is selected.
     * @param isConfused   True if "Confused" is selected.
     * @param isShameful   True if "Shameful" is selected.
     */
    @Override
    public void onFilterApplied(String timeFilter,
                                boolean isHappy,
                                boolean isSad,
                                boolean isAngry,
                                boolean isSurprised,
                                boolean isAfraid,
                                boolean isDisgusted,
                                boolean isConfused,
                                boolean isShameful) {
    }

    /**
     * Callback method that receives the final filtered list of MoodEvent objects from Firestore.
     * <p>
     * Updates the RecyclerView adapter with the filtered data.
     * </p>
     *
     * @param filteredMoods The list of filtered MoodEvent objects.
     */
    @Override
    public void onFilteredResults(List<MoodEvent> filteredMoods) {
        moodEventAdapter.updateMoodEvents(filteredMoods);
    }

    /**
     * Handles the result from EditMoodActivity.
     * <p>
     * If the mood was updated, retrieves the updated details and updates the corresponding
     * MoodEvent in Firestore.
     * </p>
     *
     * @param requestCode The request code for the activity result.
     * @param resultCode  The result code returned by the activity.
     * @param data        The Intent containing updated mood details.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_MOOD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String updatedMood = data.getStringExtra("updatedMood");
            String updatedEmoji = data.getStringExtra("updatedEmoji");
            String updatedTrigger = data.getStringExtra("updatedTrigger");
            String updatedReasonWhy = data.getStringExtra("updatedReasonWhy");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);
            int moodPosition = data.getIntExtra("moodPosition", -1);
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");

            if (moodPosition >= 0 && moodPosition < allMoodEvents.size()) {
                MoodEvent moodEventToUpdate = allMoodEvents.get(moodPosition);
                // Update Firestore
                updateMoodInFirestore(moodEventToUpdate.getDocumentId(), updatedEmoji, updatedMood, updatedTrigger,
                        updatedReasonWhy, updatedSocialSituation, updatedIntensity, updatedPhotoUri);;
            }
        }
    }

    /**
     * Updates a MoodEvent document in Firestore with the provided updated details.
     * <p>
     * Updates fields such as emoji, mood, trigger, social situation, intensity, and photo URI.
     * </p>
     *
     * @param documentId      The Firestore document ID of the MoodEvent.
     * @param emoji           The updated emoji.
     * @param mood            The updated mood title.
     * @param trigger         The updated trigger.
     * @param reasonWhy       The updated reason. 
     * @param socialSituation The updated social situation.
     * @param intensity       The updated intensity level.
     * @param photoUri        The updated photo URI.
     */

    private void updateMoodInFirestore(String documentId, String emoji, String mood,
                                       String trigger, String reasonWhy, String socialSituation,
                                       int intensity, String photoUri) {
        // Show loading indicator
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        Map<String, Object> updatedMood = new HashMap<>();
        updatedMood.put("emoji", emoji);
        updatedMood.put("mood", mood);
        updatedMood.put("emotionalState", emoji + " " + mood);
        updatedMood.put("trigger", trigger);
        updatedMood.put("socialSituation", socialSituation);
        updatedMood.put("intensity", intensity);
        updatedMood.put("reasonWhy", reasonWhy);

        if (photoUri != null && !photoUri.equals("N/A")) {
            updatedMood.put("hasPhoto", true);
            updatedMood.put("photoUri", photoUri);
        } else {
            updatedMood.put("hasPhoto", false);
        }

        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .document(documentId)
                .update(updatedMood)
                .addOnSuccessListener(aVoid -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this,
                            "Mood updated successfully",
                            Toast.LENGTH_SHORT).show();
                    loadMoodsFromFirestore();
                })
                .addOnFailureListener(e -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(ProfilePage.this,
                            "Error updating mood: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Deletes a MoodEvent document from Firestore.
     * <p>
     * Displays a confirmation dialog before deletion. If confirmed, deletes the MoodEvent and updates the adapter.
     * </p>
     *
     * @param documentId The Firestore document ID of the MoodEvent to delete.
     * @param position   The position of the MoodEvent in the list.
     */
    public void deleteMoodFromFirestore(String documentId, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Mood")
                .setMessage("Are you sure you want to delete this mood entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.VISIBLE);
                    }
                    db.collection("Usermoods")
                            .document(userId)
                            .collection("moods")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                allMoodEvents.remove(position);
                                moodEventAdapter.updateMoodEvents(new ArrayList<>(allMoodEvents));
                                if (progressLoading != null) {
                                    progressLoading.setVisibility(View.GONE);
                                }
                                Toast.makeText(ProfilePage.this,
                                        "Mood deleted successfully",
                                        Toast.LENGTH_SHORT).show();
                                if (allMoodEvents.isEmpty() && emptyStateView != null) {
                                    recyclerFeed.setVisibility(View.GONE);
                                    emptyStateView.setVisibility(View.VISIBLE);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (progressLoading != null) {
                                    progressLoading.setVisibility(View.GONE);
                                }
                                Toast.makeText(ProfilePage.this,
                                        "Error deleting mood: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
