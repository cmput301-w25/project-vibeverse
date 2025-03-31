package com.example.vibeverse;

import android.content.Intent;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HomePage is the main activity that displays a feed of mood events from followed users.
 * <p>
 * It loads mood events from Firestore, displays them in a RecyclerView using MoodEventAdapter,
 * supports search filtering and applying additional filters via FilterDialog, and handles notifications.
 * </p>
 */
public class HomePage extends AppCompatActivity implements FilterDialog.FilterListener {
    private static final String TAG = "HomePage";

    private RecyclerView recyclerFeed;
    private MoodEventAdapter moodEventAdapter;
    private List<MoodEvent> allMoodEvents;
    private EditText editSearch;
    private View emptyStateView;
    private ImageButton buttonNotification, buttonFilter;
    private BottomNavigationView bottomNavigationView;
    private ProgressBar progressLoading;
    private TextView notificationBadge;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;

    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    /**
     * Called when the activity is starting.
     * <p>
     * Initializes Firebase, UI components, sets up the RecyclerView, and loads initial mood events.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Initialize UI components
        initializeViews();

        // Set up RecyclerView
        setupRecyclerView();

        // Initial data load
        fetchFollowedUsersPosts();
    }

    /**
     * Initializes the views and sets up UI components.
     */
    private void initializeViews() {
        recyclerFeed = findViewById(R.id.recyclerFeed);
        editSearch = findViewById(R.id.editSearch);
        emptyStateView = findViewById(R.id.emptyStateView);
        progressLoading = findViewById(R.id.progressLoading);
        buttonNotification = findViewById(R.id.buttonNotification);
        buttonFilter = findViewById(R.id.buttonFilter);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        notificationBadge = findViewById(R.id.notificationBadge);

        // Setup bottom navigation
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        // Notification badge logic (kept as in original implementation)
        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long newNotificationCount = documentSnapshot.getLong("newNotificationCount");
                        if (newNotificationCount != null && newNotificationCount > 0) {
                            notificationBadge.setText(String.valueOf(newNotificationCount));
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    notificationBadge.setVisibility(View.GONE);
                });

        // Notification button
        buttonNotification.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Filter button
        buttonFilter.setOnClickListener(v ->
                FilterDialog.show(HomePage.this, HomePage.this, allMoodEvents)
        );

        // Search functionality
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
     * Sets up the RecyclerView with a LinearLayoutManager and initializes the MoodEventAdapter.
     */
    private void setupRecyclerView() {
        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);

        allMoodEvents = new ArrayList<>();
        moodEventAdapter = new MoodEventAdapter(this, new ArrayList<>());
        recyclerFeed.setAdapter(moodEventAdapter);
        // Hide menu button for other users' profiles
        moodEventAdapter.setMenuButtonVisibility(false);
        moodEventAdapter.setProfileVisibility(true);
    }

    /**
     * Fetches posts from followed users and loads them into the RecyclerView.
     */
    private void fetchFollowedUsersPosts() {
        // Show loading state
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }
        if (recyclerFeed != null) {
            recyclerFeed.setVisibility(View.GONE);
        }

        // First, get the list of following IDs
        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .document("list")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");

                    if (followingIds == null || followingIds.isEmpty()) {
                        showEmptyState(true);
                        return;
                    }

                    // Create a combined list for all mood events
                    List<MoodEvent> combinedMoodEvents = new ArrayList<>();

                    // Track completion with AtomicInteger to handle async calls
                    AtomicInteger pendingTasks = new AtomicInteger(followingIds.size());

                    // Process each followed user
                    for (String userId : followingIds) {
                        processUserMoodEvents(userId, combinedMoodEvents, pendingTasks, followingIds.size());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching following list", e);
                    showEmptyState(true);
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                });
    }

    /**
     * Processes mood events for a specific user.
     *
     * @param userId             The user ID whose mood events are to be processed.
     * @param combinedMoodEvents A list to accumulate all mood events.
     * @param pendingTasks       An AtomicInteger to track pending async tasks.
     * @param totalUsers         The total number of users being processed.
     */
    private void processUserMoodEvents(String userId, List<MoodEvent> combinedMoodEvents,
                                       AtomicInteger pendingTasks, int totalUsers) {
        // First fetch user profile information
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(userDoc -> {
                    String username = userDoc.getString("username");
                    String profilePicUri = userDoc.getString("profilePicUri");

                    // Track mood events for this user
                    AtomicInteger userMoodEventCount = new AtomicInteger(0);

                    // Then fetch their mood events
                    db.collection("Usermoods")
                            .document(userId)
                            .collection("moods")
                            .orderBy("timestamp", Query.Direction.DESCENDING)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                                    if (userMoodEventCount.get() >= 3) {
                                        break;
                                    }
                                    try {
                                        MoodEvent moodEvent = MoodEvent.fromMap(doc.getData());

                                        // Check if the post is public
                                        Boolean isPublic = (Boolean) doc.getData().get("isPublic");
                                        if (isPublic == null || !isPublic) {
                                            continue;
                                        }

                                        moodEvent.setDocumentId(doc.getId());

                                        // Set the user profile information
                                        moodEvent.setUsername(username);
                                        moodEvent.setProfilePictureUrl(profilePicUri);
                                        moodEvent.setOwnerUserId(userId);

                                        if (moodEvent.getTimestamp() != null) {
                                            Date date = sourceFormat.parse(moodEvent.getTimestamp());
                                            moodEvent.setDate(date);
                                        }

                                        // Build subtitle
                                        StringBuilder subtitle = new StringBuilder();
                                        if (moodEvent.getSocialSituation() != null &&
                                                !moodEvent.getSocialSituation().isEmpty()) {
                                            subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                                        }
                                        moodEvent.setSubtitle(subtitle.toString());

                                        synchronized (combinedMoodEvents) {
                                            combinedMoodEvents.add(moodEvent);
                                        }
                                        userMoodEventCount.incrementAndGet();
                                    } catch (ParseException e) {
                                        Log.e(TAG, "Error parsing timestamp", e);
                                    }
                                }

                                // Check if all users have been processed
                                if (pendingTasks.decrementAndGet() == 0) {
                                    finalizeMoodEvents(combinedMoodEvents);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error fetching posts for user: " + userId, e);
                                // Still decrement counter even if there's an error
                                if (pendingTasks.decrementAndGet() == 0) {
                                    finalizeMoodEvents(combinedMoodEvents);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching user profile for: " + userId, e);
                    // Decrement counter on failure
                    if (pendingTasks.decrementAndGet() == 0) {
                        finalizeMoodEvents(combinedMoodEvents);
                    }
                });
    }

    /**
     * Finalizes the list of mood events after all asynchronous tasks are complete,
     * sorts them by timestamp, and updates the UI.
     *
     * @param combinedMoodEvents The combined list of mood events.
     */
    private void finalizeMoodEvents(List<MoodEvent> combinedMoodEvents) {
        // Sort by timestamp and update UI on main thread
        runOnUiThread(() -> {
            // Sort by timestamp
            combinedMoodEvents.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            allMoodEvents = new ArrayList<>(combinedMoodEvents);

            if (allMoodEvents.isEmpty()) {
                showEmptyState(true);
            } else {
                showEmptyState(false);
                moodEventAdapter.updateMoodEvents(allMoodEvents);
            }

            if (progressLoading != null) {
                progressLoading.setVisibility(View.GONE);
            }

            // Apply any existing text search filter
            String currentSearchText = editSearch.getText().toString();
            if (!currentSearchText.isEmpty()) {
                moodEventAdapter.filter(currentSearchText);
            }
        });
    }

    /**
     * Shows or hides the empty state view based on whether there are mood events.
     *
     * @param show True to show the empty state, false to hide it.
     */
    private void showEmptyState(boolean show) {
        if (emptyStateView != null) {
            emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerFeed.setVisibility(show ? View.GONE : View.VISIBLE);
            progressLoading.setVisibility(View.GONE);  // Always hide loading when showing results
        }
    }

    /**
     * Called when the activity resumes.
     * <p>
     * Reloads followed users' posts and checks the user's mood streak.
     * </p>
     */
    @Override
    protected void onResume() {
        super.onResume();
        fetchFollowedUsersPosts();
        checkAndResetStreak();
    }

    /**
     * Callback method for FilterDialog.FilterListener.
     * <p>
     * This method is required but not used directly.
     * </p>
     *
     * @param timeFilter  The selected time filter.
     * @param isHappy     True if "Happy" is selected.
     * @param isSad       True if "Sad" is selected.
     * @param isAngry     True if "Angry" is selected.
     * @param isSurprised True if "Surprised" is selected.
     * @param isAfraid    True if "Afraid" is selected.
     * @param isDisgusted True if "Disgusted" is selected.
     * @param isConfused  True if "Confused" is selected.
     * @param isShameful  True if "Shameful" is selected.
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
        // This method is required but not used directly
    }

    /**
     * Callback method for FilterDialog.FilterListener.
     * <p>
     * Updates the adapter with the filtered list of mood events.
     * </p>
     *
     * @param filteredMoods The list of filtered MoodEvent objects.
     */
    @Override
    public void onFilteredResults(List<MoodEvent> filteredMoods) {
        // Update the adapter with filtered moods
        moodEventAdapter.updateMoodEvents(filteredMoods);

        // Apply any existing text search filter
        String currentSearchText = editSearch.getText().toString();
        if (!currentSearchText.isEmpty()) {
            moodEventAdapter.filter(currentSearchText);
        }
    }

    /**
     * Checks and resets the user's mood streak if they haven't posted a mood event today.
     */
    private void checkAndResetStreak() {
        // Use a simple date format to compare dates (e.g., "yyyy-MM-dd")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDateStr = sdf.format(new Date());

        DocumentReference userDocRef = db.collection("users").document(currentUserId);
        userDocRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the stored last mood date and mood streak
                String lastMoodDate = documentSnapshot.contains("last_mood_date") ?
                        documentSnapshot.getString("last_mood_date") : null;
                Long streakLong = documentSnapshot.contains("mood_streak") ?
                        documentSnapshot.getLong("mood_streak") : 0L;

                // If lastMoodDate exists and is not today's date, reset the streak.
                if (lastMoodDate != null && !lastMoodDate.equals(currentDateStr)) {
                    // Reset the mood streak to 0 since the user has not posted a mood today
                    userDocRef.update("mood_streak", 0)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Mood streak reset to 0");
                                // Also update the achievement checker
                                AchievementChecker achievementChecker = new AchievementChecker(currentUserId);
                                achievementChecker.checkAch23(0);
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error resetting mood streak", e));
                }
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error fetching user data for streak check", e);
        });
    }
}