package com.example.vibeverse;

import android.annotation.SuppressLint;

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
import android.view.MenuItem;
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


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfilePage extends AppCompatActivity implements FilterDialog.FilterListener {

    private static final String TAG = "ProfilePage";
    private static final int EDIT_MOOD_REQUEST_CODE = 1001;


    private RecyclerView recyclerFeed;
    private Button buttonFilter;
    private PostAdapter postAdapter;
    private List<Post> allPosts;
    private EditText editSearch;

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

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                // Launch SelectMoodActivity
                Intent intent = new Intent(ProfilePage.this, SelectMoodActivity.class);
                startActivity(intent);
                return true;
            }
            // Handle other navigation items here
            return false;
        });

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
        allPosts = new ArrayList<>();
        postAdapter = new PostAdapter(new ArrayList<>());
        recyclerFeed.setAdapter(postAdapter);

        // Load data from Firestore instead of using dummy data
        loadMoodsFromFirestore();

        buttonFilter.setOnClickListener(v -> FilterDialog.show(ProfilePage.this, this));

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                postAdapter.filter(charSequence.toString());
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
                    allPosts.clear();
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

                            // Create a Post object
                            Post post = new Post(
                                    moodEvent.getMood(), // Title is the mood text
                                    moodEvent.getEmoji(), // Mood is the emoji
                                    R.drawable.demo_image, // Default image for now
                                    date
                            );

                            // Store the document ID for Firestore operations
                            post.documentId = document.getId();

                            // Add subtitle using trigger and social situation
                            StringBuilder subtitle = new StringBuilder();

                            if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
                                subtitle.append("Trigger: ").append(moodEvent.getTrigger());
                                post.trigger = moodEvent.getTrigger();
                            }

                            if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
                                if (subtitle.length() > 0) {
                                    subtitle.append(" | ");
                                }
                                subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                                post.socialSituation = moodEvent.getSocialSituation();
                            }

                            post.subtitle = subtitle.toString();
                            post.photoUri = moodEvent.getPhotoUri();
                            post.intensity = moodEvent.getIntensity();
                            post.timestamp = moodEvent.getTimestamp();

                            // Add to the list
                            allPosts.add(post);
                        } catch (Exception e) {
                            Log.e("ProfilePage", "Error parsing mood data: " + e.getMessage());
                        }
                    }

                    // Update the adapter with the new data
                    postAdapter.updatePosts(new ArrayList<>(allPosts));

                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }

                    // Show empty state if no posts
                    if (allPosts.isEmpty()) {
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
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);
            String timestamp = data.getStringExtra("timestamp");
            int moodPosition = data.getIntExtra("moodPosition", -1);
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");

            if (moodPosition >= 0 && moodPosition < allPosts.size()) {
                // Get the post to update
                Post postToUpdate = allPosts.get(moodPosition);

                // Update Firestore
                updateMoodInFirestore(postToUpdate.documentId, updatedEmoji, updatedMood,
                        updatedTrigger, updatedSocialSituation, updatedIntensity, updatedPhotoUri);
            }





        }
    }

    private void updateMoodInFirestore(String documentId, String emoji, String mood,
                                       String trigger, String socialSituation,
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

    private void deleteMoodFromFirestore(String documentId, int position) {
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
                                allPosts.remove(position);
                                postAdapter.updatePosts(new ArrayList<>(allPosts));

                                if (progressLoading != null) {
                                    progressLoading.setVisibility(View.GONE);
                                }

                                Toast.makeText(ProfilePage.this, "Mood deleted successfully", Toast.LENGTH_SHORT).show();

                                // Check if list is now empty
                                if (allPosts.isEmpty() && emptyStateView != null) {
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
        List<Post> filteredPosts = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Post post : allPosts) {
            long postTime = post.date.getTime();
            boolean isWithinTime = false;

            switch (timeFilter) {
                case "last_24_hours":
                    isWithinTime = (currentTime - postTime) <= 86400000;
                    break;
                case "3Days":
                    isWithinTime = (currentTime - postTime) <= 259200000;
                    break;
                case "last_week":
                    isWithinTime = (currentTime - postTime) <= 604800000;
                    break;
                case "last_month":
                    isWithinTime = (currentTime - postTime) <= 2592000000L;
                    break;
                case "all_time":
                    isWithinTime = true;
                    break;
            }

            if (isWithinTime) {
                if ((isHappy && post.mood.equals("HAPPY")) ||
                        (isSad && post.mood.equals("SAD")) ||
                        (isAfraid && post.mood.equals("AFRAID")) ||
                        (isConfused && post.mood.equals("CONFUSED")) ||
                        (!isHappy && !isSad && !isAfraid && !isConfused)) {
                    filteredPosts.add(post);
                }
            }
        }
        postAdapter.updatePosts(filteredPosts);
    }

    private static class Post {
        public String subtitle;
        String title;
        String mood;
        int imageResId;
        Date date;

        // Added fields for edit and delete functionality
        String documentId; // Firestore document ID
        String trigger;
        String socialSituation;
        int intensity = 5; // Default intensity
        String timestamp;
        String photoUri;
        String emoji;

        Post(String title, String mood, int imageResId, Date date) {
            this.title = title;
            this.mood = mood;
            this.imageResId = imageResId;
            this.date = date;
        }
    }

    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

        private final List<Post> postList;
        private List<Post> originalList;
        private List<Post> currentList;

        private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);

        PostAdapter(List<Post> postList) {
            this.postList = postList;
            this.originalList = new ArrayList<>(postList);
            this.currentList = new ArrayList<>(postList);
        }

        public void updatePosts(List<Post> newPosts) {
            postList.clear();
            postList.addAll(newPosts);
            originalList = new ArrayList<>(newPosts);
            currentList = new ArrayList<>(newPosts);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Post post = postList.get(position);
            holder.textTitle.setText(post.title);
            holder.textSubtitle.setText(formatter.format(post.date) + " • " + post.mood);
            holder.imagePost.setImageResource(post.imageResId);

            // Set click listener for the menu button
            holder.buttonPostMenu.setOnClickListener(v -> {
                showPostMenu(v, position, post);
            });
        }

        // Java
        private void showPostMenu(View view, int position, Post post) {
            // Create a popup menu without XML
            PopupMenu popup = new PopupMenu(ProfilePage.this, view);

            // Add menu items programmatically
            Menu menu = popup.getMenu();
            menu.add(0, 1, 0, "Edit");
            menu.add(0, 2, 0, "Delete");

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == 1) { // Edit
                    // Launch EditMoodActivity
                    Intent intent = new Intent(ProfilePage.this, EditMoodActivity.class);
                    intent.putExtra("selectedMood", post.title);
                    intent.putExtra("selectedEmoji", post.mood);
                    intent.putExtra("trigger", post.trigger);
                    intent.putExtra("socialSituation", post.socialSituation);
                    intent.putExtra("intensity", post.intensity);
                    intent.putExtra("timestamp", post.timestamp);
                    intent.putExtra("photoUri", post.photoUri);
                    intent.putExtra("moodPosition", position);
                    startActivityForResult(intent, EDIT_MOOD_REQUEST_CODE);
                    return true;
                } else if (id == 2) { // Delete
                    // Delete the mood
                    deleteMoodFromFirestore(post.documentId, position);
                    return true;
                }
                return false;
            });

            popup.show();
        }
        @Override
        public int getItemCount() {
            return postList.size();
        }

        // Custom filter method
        public void filter(String query) {
            query = query.toLowerCase().trim();
            currentList.clear();
            if (query.isEmpty()) {
                currentList.addAll(originalList);
            } else {
                for (Post post : originalList) {
                    boolean titleMatches = post.title != null && post.title.toLowerCase().contains(query);
                    boolean subtitleMatches = post.subtitle != null && post.subtitle.toLowerCase().contains(query);
                    if (titleMatches || subtitleMatches) {
                        currentList.add(post);
                    }
                }
            }
            // Update postList since it's used for display
            postList.clear();
            postList.addAll(currentList);
            notifyDataSetChanged();
        }

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView imagePost;
            TextView textTitle, textSubtitle;
            ImageButton buttonPostMenu;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imagePost = itemView.findViewById(R.id.imagePost);
                textTitle = itemView.findViewById(R.id.textTitle);
                textSubtitle = itemView.findViewById(R.id.textSubtitle);
                buttonPostMenu = itemView.findViewById(R.id.buttonPostMenu);
            }
        }
    }
}