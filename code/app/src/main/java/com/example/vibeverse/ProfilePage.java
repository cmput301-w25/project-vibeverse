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
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
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


    private TextView textName, textUsername, textBioContent, textFollowers, textFollowing, textPosts;
    private ImageView profilePicture;


    private ImageButton profileSettingsMenu;
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

    private DrawerLayout drawerLayout;
    private NavigationView rightNavView;

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
        textName = findViewById(R.id.fullName);
        textUsername = findViewById(R.id.textTopUsername);
        textBioContent = findViewById(R.id.textBioContent);
        profilePicture = findViewById(R.id.profilePicture);
        textFollowers = findViewById(R.id.textFollowers);
        textFollowing = findViewById(R.id.textFollowing);
        drawerLayout = findViewById(R.id.drawer_layout);
        rightNavView = findViewById(R.id.right_nav_view);
        textPosts = findViewById(R.id.textPosts);

        // Then call a helper method to load the profile
        loadUserProfile();
        // Logout button
        profileSettingsMenu = findViewById(R.id.buttonOverflowMenu);


        // Set up logout button to sign out the user.
        profileSettingsMenu = findViewById(R.id.buttonOverflowMenu);
        profileSettingsMenu.setOnClickListener(v -> {
            drawerLayout.openDrawer(GravityCompat.END);
        });

        rightNavView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();
            if (id == R.id.menu_vibestore) {
                // to be added
            } else if (id == R.id.menu_vibestatus) {
                // to be added
            } else if (id == R.id.menu_editprofile) {
                Intent intent = new Intent(ProfilePage.this, UserDetails.class);
                intent.putExtra("source", "edit_profile");
                startActivity(intent);
            } else if (id == R.id.menu_logout) {
                mAuth.signOut();
                startActivity(new Intent(ProfilePage.this, Login.class));
                finish();
            }
            // Close the drawer after a selection is made
            drawerLayout.closeDrawer(GravityCompat.END);
            return true;
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
                FilterDialog.show(ProfilePage.this, ProfilePage.this, allMoodEvents)
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

                    // Update the posts TextView
                    int postCount = snapshots.size();
                    textPosts.setText(String.valueOf(postCount));

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

                            StringBuilder subtitle = new StringBuilder();
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
                        String followerCount = String.valueOf(documentSnapshot.getLong("followerCount"));
                        String followingCount = String.valueOf(documentSnapshot.getLong("followingCount"));

                        // Populate the TextViews
                        if (fullName != null) textName.setText(fullName);
                        if (username != null) textUsername.setText("@"+username);
                        if (bio != null) textBioContent.setText(bio);
                        textFollowers.setText(followerCount);
                        textFollowing.setText(followingCount);

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
        loadUserProfile(); // Reload updated user details
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
        // First update the adapter with filtered moods
        moodEventAdapter.updateMoodEvents(filteredMoods);

        // Then apply any existing text search filter
        String currentSearchText = editSearch.getText().toString();
        if (!currentSearchText.isEmpty()) {
            moodEventAdapter.filter(currentSearchText);
        }
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
            String updatedReasonWhy = data.getStringExtra("updatedReasonWhy");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);
            int moodPosition = data.getIntExtra("moodPosition", -1);
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");
            boolean isPublic = data.getBooleanExtra("isPublic", false);

            // Add these lines to retrieve location data
            String updatedMoodLocation = data.getStringExtra("updatedMoodLocation");
            Double updatedMoodLatitude = null;
            Double updatedMoodLongitude = null;
            boolean locationRemoved = data.getBooleanExtra("locationRemoved", false);

            if (data.hasExtra("updatedMoodLatitude") && data.hasExtra("updatedMoodLongitude")) {
                updatedMoodLatitude = data.getDoubleExtra("updatedMoodLatitude", 0);
                updatedMoodLongitude = data.getDoubleExtra("updatedMoodLongitude", 0);
            }

            if (moodPosition >= 0 && moodPosition < allMoodEvents.size()) {
                MoodEvent moodEventToUpdate = allMoodEvents.get(moodPosition);
                // Update Firestore with location info
                updateMoodInFirestore(
                        moodEventToUpdate.getDocumentId(),
                        updatedEmoji,
                        updatedMood,
                        updatedReasonWhy,
                        updatedSocialSituation,
                        updatedIntensity,
                        updatedPhotoUri,
                        isPublic,
                        updatedMoodLocation,
                        updatedMoodLatitude,
                        updatedMoodLongitude,
                        locationRemoved
                );
            }
        }
    }

    /**
     * Updates a MoodEvent document in Firestore with the provided updated details.
     * <p>
     * Updates fields such as emoji, mood, social situation, intensity, and photo URI.
     * </p>
     *
     * @param documentId      The Firestore document ID of the MoodEvent.
     * @param emoji           The updated emoji.
     * @param mood            The updated mood title.
     * @param reasonWhy       The updated reason. 
     * @param socialSituation The updated social situation.
     * @param intensity       The updated intensity level.
     * @param photoUri        The updated photo URI.
     */

    private void updateMoodInFirestore(String documentId, String emoji, String mood,
                                       String reasonWhy, String socialSituation,
                                       int intensity, String photoUri, boolean isPublic,
                                       String moodLocation, Double latitude, Double longitude,
                                       boolean locationRemoved) {
        // Show loading indicator
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }

        Map<String, Object> updatedMood = new HashMap<>();
        updatedMood.put("emoji", emoji);
        updatedMood.put("mood", mood);
        updatedMood.put("emotionalState", emoji + " " + mood);
        updatedMood.put("socialSituation", socialSituation);
        updatedMood.put("intensity", intensity);
        updatedMood.put("reasonWhy", reasonWhy);
        updatedMood.put("isPublic", isPublic);

        if (photoUri != null && !photoUri.equals("N/A")) {
            updatedMood.put("hasPhoto", true);
            updatedMood.put("photoUri", photoUri);
        } else {
            updatedMood.put("hasPhoto", false);
        }

        // Handle location data
        if (locationRemoved) {
            // Remove location fields if location was explicitly removed
            updatedMood.put("moodLocation", null);
            updatedMood.put("moodLatitude", null);
            updatedMood.put("moodLongitude", null);
        } else if (moodLocation != null && latitude != null && longitude != null) {
            // Update with new location
            updatedMood.put("moodLocation", moodLocation);
            updatedMood.put("moodLatitude", latitude);
            updatedMood.put("moodLongitude", longitude);
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
                                textPosts.setText(String.valueOf(allMoodEvents.size()));
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
