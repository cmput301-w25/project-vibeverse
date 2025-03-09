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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.view.View;

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

public class ProfilePage extends AppCompatActivity implements FilterDialog.FilterListener {

    public static final int EDIT_MOOD_REQUEST_CODE = 1001;
    private static final String TAG = "ProfilePage";

    private RecyclerView recyclerFeed;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> allMoodEvents;
    private EditText editSearch;
    private View emptyStateView;
    private Button logoutButton;
    private BottomNavigationView bottomNavigationView;
    private ImageButton buttonFilter;
    private ProgressBar progressLoading;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            // If not logged in, we use a device-based ID (as a fallback)
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }

        logoutButton = findViewById(R.id.buttonLogout);
        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfilePage.this, Login.class));
            finish();
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        recyclerFeed = findViewById(R.id.recyclerFeed);
        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);

        editSearch = findViewById(R.id.editSearch);
        buttonFilter = findViewById(R.id.buttonFilter);
        emptyStateView = findViewById(R.id.emptyStateView);
        progressLoading = findViewById(R.id.progressLoading);

        // In case progressLoading is null in layout, create it programmatically
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

        allMoodEvents = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(this, new ArrayList<>());
        recyclerFeed.setAdapter(moodEventAdapter);

        // Initial load from Firestore
        loadMoodsFromFirestore();

        // Open Filter dialog
        buttonFilter.setOnClickListener(v ->
                FilterDialog.show(ProfilePage.this, ProfilePage.this)
        );

        // Basic search filter (client-side)
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

    @Override
    public void onResume() {
        super.onResume();
        // Reload the entire list each time we resume
        loadMoodsFromFirestore();
    }

    // 1) This method is required by the new interface (even if you don't use it here)
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
        // Optionally do something here if you want local filtering logic
        // (If not needed, leave it empty)
    }

    // 2) This method receives the final filtered list from Firestore
    @Override
    public void onFilteredResults(List<MoodEvent> filteredMoods) {
        // Update your recycler with the final filtered data
        moodEventAdapter.updateMoodEvents(filteredMoods);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_MOOD_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            String updatedMood = data.getStringExtra("updatedMood");
            String updatedEmoji = data.getStringExtra("updatedEmoji");
            String updatedTrigger = data.getStringExtra("updatedTrigger");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);
            int moodPosition = data.getIntExtra("moodPosition", -1);
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");

            if (moodPosition >= 0 && moodPosition < allMoodEvents.size()) {
                MoodEvent moodEventToUpdate = allMoodEvents.get(moodPosition);
                updateMoodInFirestore(
                        moodEventToUpdate.getDocumentId(),
                        updatedEmoji,
                        updatedMood,
                        updatedTrigger,
                        updatedSocialSituation,
                        updatedIntensity,
                        updatedPhotoUri
                );
            }
        }
    }

    private void updateMoodInFirestore(
            String documentId,
            String emoji,
            String mood,
            String trigger,
            String socialSituation,
            int intensity,
            String photoUri
    ) {
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
