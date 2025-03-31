package com.example.vibeverse;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


import de.hdodenhof.circleimageview.CircleImageView;

public class UsersProfile extends AppCompatActivity implements FilterDialog.FilterListener {

    /** The ID of the user whose profile is being viewed. */
    private String pageUserId;
    /** Firestore instance for database operations. */
    private FirebaseFirestore db;

    // UI Elements

    /** The profile picture view. */
    private CircleImageView profilePicture;

    /** TextView displaying the user's full name. */
    private TextView textName, textBioContent, textFollowers, textFollowing, textPosts;

    /** Button shown when the active user can follow the profile user. */
    private Button buttonFollowStateFollow;
    /** Button shown when a follow request has been sent. */
    private Button buttonFollowStateRequested;
    /** Button shown when the active user is already following the profile user. */
    private Button buttonFollowStateFollowing;

    /** TextView showing the username at the top of the profile. */
    private TextView  textTopUsername;
    /** Button to initiate a follow action. */
    private Button buttonFollow;

    /** Button to go back from the profile view. */
    private ImageButton buttonBack;
    /** RecyclerView to display the profile user's posts. */
    private RecyclerView recyclerUserPosts;
    /** ProgressBar displayed during loading operations. */
    private ProgressBar progressLoading;
    /** View displayed when no posts are available or follow is required. */
    private View emptyStateView;

    /** ID of the currently active user. */
    String activeUserId;
    /** EditText for searching/filtering mood events. */
    private EditText editSearch;
    /** Button to open the filter dialog. */
    private ImageButton buttonFilter;
    /** List holding all mood events for the profile user. */
    private List<MoodEvent> allMoodEvents = new ArrayList<>();

    /** Adapter for displaying mood events in the RecyclerView. */
    private MoodEventAdapter moodEventAdapter;
    /** Formatter for parsing source timestamps. */
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    /**
     * Called when the activity is created. Initializes UI components, loads user data and posts,
     * and sets up event listeners for follow actions, back navigation, and filtering.
     *
     * @param savedInstanceState Bundle containing saved state data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.users_profile);
        activeUserId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Get user ID from intent
        pageUserId = getIntent().getStringExtra("userId");
        if (pageUserId == null) {
            Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        initViews();

        // Initialize adapter
        moodEventAdapter = new MoodEventAdapter(this, new ArrayList<>());
        recyclerUserPosts.setAdapter(moodEventAdapter);

        // Hide menu button for other users' profiles
        moodEventAdapter.setMenuButtonVisibility(false);

        // Load user data
        loadUserData();

        // Load user posts
        loadUserPosts();

        // Set up back button
        buttonBack.setOnClickListener(v -> finish());

        // Set up follow button (placeholder for now)
        buttonFollowStateFollow.setOnClickListener(v -> {
            // First, retrieve the active user's username from Firestore.
            db.collection("users").document(activeUserId)
                    .get()
                    .addOnSuccessListener(docSnapshot -> {
                        if (docSnapshot.exists()) {
                            String activeUsername = docSnapshot.getString("username");
                            if (activeUsername == null || activeUsername.isEmpty()) {
                                Toast.makeText(UsersProfile.this,
                                        "Active user username not found.",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Build the notification content using the active user's username.
                            String notificationContent = activeUsername + " has requested to follow you!";
                            DocumentReference recipientUserRef = db.collection("users").document(pageUserId);
                            DocumentReference newNotifDocRef = recipientUserRef.collection("notifications").document();
                            String notifId = newNotifDocRef.getId();

                            // Create the Notification object.
                            Notification followNotification = null;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                followNotification = new Notification(
                                        notifId,
                                        notificationContent,                     // content
                                        LocalDateTime.now().toString(),                     // dateTime
                                        Notification.NotifType.FOLLOW_REQUEST,   // notifType
                                        activeUserId,                            // senderUserId
                                        pageUserId                             // receiverUserId
                                );
                            }

                            // Convert Notification object into a Map for Firestore.
                            Map<String, Object> notifData = new HashMap<>();
                            notifData.put("content", followNotification.getContent());
                            notifData.put("dateTime", followNotification.getDateTime().toString());
                            notifData.put("notifType", followNotification.getNotifType().name());
                            notifData.put("senderUserId", followNotification.getSenderUserId());
                            notifData.put("receiverUserId", followNotification.getReceiverUserId());
                            notifData.put("isRead", followNotification.isRead());
                            notifData.put("requestStatus", followNotification.getRequestStatus());
                            notifData.put("id", notifId);

                            recipientUserRef.collection("notifications")
                                    .add(notifData)
                                    .addOnSuccessListener(docRef -> {
                                        // Increment the user's newNotificationCount.
                                        recipientUserRef.update("newNotificationCount",
                                                com.google.firebase.firestore.FieldValue.increment(1));

                                        // Also add the sender's ID to the "followRequests" subcollection.
                                        Map<String, Object> followRequestUpdate = new HashMap<>();
                                        followRequestUpdate.put("followReqs",
                                                com.google.firebase.firestore.FieldValue.arrayUnion(activeUserId));

                                        recipientUserRef.collection("followRequests")
                                                .document("list")
                                                .set(followRequestUpdate, com.google.firebase.firestore.SetOptions.merge())
                                                .addOnSuccessListener(aVoid2 -> {
                                                    Toast.makeText(UsersProfile.this,
                                                            "Follow request sent!",
                                                            Toast.LENGTH_SHORT).show();
                                                    showRequestedState();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(UsersProfile.this,
                                                            "Failed to add follow request: " + e.getMessage(),
                                                            Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(UsersProfile.this,
                                                "Failed to send notification: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(UsersProfile.this,
                                    "Active user data not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UsersProfile.this,
                                "Error retrieving active user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        buttonFollowStateRequested.setOnClickListener(v -> {
            DocumentReference recipientUserRef = db.collection("users").document(pageUserId);

            // 1) Remove from followRequests
            recipientUserRef.collection("followRequests")
                    .document("list")
                    .update("followReqs",
                            com.google.firebase.firestore.FieldValue.arrayRemove(activeUserId))
                    .addOnSuccessListener(aVoid -> {
                        // 2) Find & delete the matching notification (senderUserId=activeUserId & notifType=FOLLOW_REQUEST)
                        recipientUserRef.collection("notifications")
                                .whereEqualTo("senderUserId", activeUserId)
                                .whereEqualTo("notifType", Notification.NotifType.FOLLOW_REQUEST.name())
                                .limit(1)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        // There's at most one doc that matches
                                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                                        doc.getReference().delete(); // remove the notification doc

                                        // 3) Decrement newNotificationCount
                                        recipientUserRef.update("newNotificationCount",
                                                com.google.firebase.firestore.FieldValue.increment(-1));
                                    }
                                });

                        // 4) Switch UI to "Follow"
                        showFollowState();
                        Toast.makeText(UsersProfile.this, "Follow request canceled.", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UsersProfile.this, "Failed to cancel request: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });
        buttonFollowStateFollowing.setOnClickListener(v -> {
            String profileUsername = textTopUsername.getText().toString();

            // Inflate the custom layout
            LayoutInflater inflater = LayoutInflater.from(UsersProfile.this);
            View dialogView = inflater.inflate(R.layout.dialog_unfollow_confirmation, null);

            // Get references to the dialog views
            TextView titleTextView = dialogView.findViewById(R.id.dialogTitle);
            TextView messageTextView = dialogView.findViewById(R.id.dialogMessage);
            Button yesButton = dialogView.findViewById(R.id.buttonYes);
            Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

            // Set the message text dynamically if needed
            messageTextView.setText("Are you sure you want to unfollow " + profileUsername + "? You will have to request to follow again.");

            // Create the AlertDialog with the custom view
            AlertDialog alertDialog = new AlertDialog.Builder(UsersProfile.this)
                    .setView(dialogView)
                    .create();

            // Set the Yes button action
            yesButton.setOnClickListener(v1 -> {
                DocumentReference pageUserRef = db.collection("users").document(pageUserId);
                DocumentReference activeUserRef = db.collection("users").document(activeUserId);

                // Remove activeUserId from the page user's followers subcollection.
                pageUserRef.collection("followers")
                        .document("list")
                        .update("followerIds", FieldValue.arrayRemove(activeUserId))
                        .addOnSuccessListener(aVoid -> {
                            // Remove pageUserId from the active user's following subcollection.
                            activeUserRef.collection("following")
                                    .document("list")
                                    .update("followingIds", FieldValue.arrayRemove(pageUserId))
                                    .addOnSuccessListener(aVoid2 -> {
                                        // Decrement counts: page user's followerCount and active user's followingCount.
                                        pageUserRef.update("followerCount", FieldValue.increment(-1));
                                        activeUserRef.update("followingCount", FieldValue.increment(-1))
                                                .addOnSuccessListener(aVoid3 -> {
                                                    // Check the unfollow achievement ("I don't like you anymore" - ach12)
                                                    AchievementChecker achievementChecker = new AchievementChecker(activeUserId);
                                                    achievementChecker.checkAch12();

                                                    showFollowState();
                                                    loadUserPosts(); // reload the posts
                                                    Toast.makeText(UsersProfile.this, "Unfollowed.", Toast.LENGTH_SHORT).show();
                                                    alertDialog.dismiss();
                                                })
                                                .addOnFailureListener(e -> {
                                                    Toast.makeText(UsersProfile.this, "Failed to update following count: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(UsersProfile.this, "Failed to update following list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(UsersProfile.this, "Failed to update followers list: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            });

            // Set the Cancel button action
            cancelButton.setOnClickListener(v12 -> alertDialog.dismiss());

            // Show the custom dialog
            alertDialog.show();
        });

        // Set up search functionality for client-side filtering
        editSearch.addTextChangedListener(new TextWatcher() {
            /**
             * Called before text is changed.
             *
             * @param s The text before change.
             * @param start The start position.
             * @param count The number of characters before change.
             * @param after The number of characters after change.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * Called when text is changed. Filters mood events based on the search string.
             *
             * @param s The new text.
             * @param start The start position.
             * @param before The number of characters before change.
             * @param count The number of characters after change.
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                moodEventAdapter.filter(s.toString());
            }

            /**
             * Called after text is changed.
             *
             * @param s The final text.
             */
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Open the FilterDialog when the filter button is clicked
        buttonFilter.setOnClickListener(v ->
                FilterDialog.show(UsersProfile.this, UsersProfile.this, allMoodEvents)
        );
    }

    /**
     * Initializes the UI views.
     */
    private void initViews() {
        profilePicture = findViewById(R.id.profilePicture);
        textName = findViewById(R.id.textName);
        textTopUsername = findViewById(R.id.textTopUsername);
        textBioContent = findViewById(R.id.textBioContent);
        textFollowers = findViewById(R.id.textFollowers);
        textFollowing = findViewById(R.id.textFollowing);
        buttonFollowStateFollow = findViewById(R.id.buttonFollow);
        buttonFollowStateRequested = findViewById(R.id.buttonFollowStateRequested);
        buttonFollowStateFollowing = findViewById(R.id.buttonFollowStateFollowing);
        buttonFollowStateFollow.setVisibility(View.GONE);
        buttonFollowStateRequested.setVisibility(View.GONE);
        buttonFollowStateFollowing.setVisibility(View.GONE);
        buttonBack = findViewById(R.id.buttonBack);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        progressLoading = findViewById(R.id.progressLoading);
        emptyStateView = findViewById(R.id.emptyStateView);
        editSearch = findViewById(R.id.editSearch);
        buttonFilter = findViewById(R.id.buttonFilter);
        textPosts = findViewById(R.id.textPosts);

        recyclerUserPosts.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * Loads user data from Firestore and updates the UI accordingly.
     */
    private void loadUserData() {
        showLoading(true);

        db.collection("users").document(pageUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);

                    if (!documentSnapshot.exists()) {
                        Toast.makeText(UsersProfile.this, "User not found", Toast.LENGTH_SHORT).show();

                        finish();
                        return;
                    }

                    updateUI(documentSnapshot);

                    // After we have the basic user info, check follow status:
                    checkFollowStatus();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    /**
     * Updates the UI elements based on the retrieved user data.
     *
     * @param document The Firestore DocumentSnapshot containing user data.
     */
    private void updateUI(DocumentSnapshot document) {
        User user = document.toObject(User.class);
        if (user != null) {
            textName.setText(user.getFullName());
            textTopUsername.setText("@" + user.getUsername());

            // Set follower and following counts
            textFollowers.setText(String.valueOf(user.getFollowerCount()));
            textFollowing.setText(String.valueOf(user.getFollowingCount()));

            // Set bio if available
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                textBioContent.setText(user.getBio());
            } else {
                textBioContent.setText("");
            }

            if (user.isHasProfilePic() && user.getProfilePicUri() != null) {
                Glide.with(this)
                        .load(user.getProfilePicUri())
                        .placeholder(R.drawable.user_icon)
                        .error(R.drawable.user_icon)
                        .into(profilePicture);
            } else {
                profilePicture.setImageResource(R.drawable.user_icon);
            }
        }
    }

    /**
     * Loads the posts of the user. Checks follow status before fetching posts.
     */
    private void loadUserPosts() {
        showLoading(true);

        // First, check if the active user is following the page user
        DocumentReference followersDocRef = db.collection("users")
                .document(pageUserId)
                .collection("followers")
                .document("list");

        followersDocRef.get().addOnSuccessListener(followersDocSnap -> {
            @SuppressWarnings("unchecked")
            java.util.List<String> followerIds =
                    (java.util.List<String>) followersDocSnap.get("followerIds");

            // Check if active user is in followers list
            if (followerIds != null && followerIds.contains(activeUserId)) {
                // User is following, load posts normally
                fetchUserPosts();
            } else {
                // Not following, show follow to view posts state
                showLoading(false);
                showFollowToViewPostsState(true);
            }
        }).addOnFailureListener(e -> {
            showLoading(false);
            showFollowToViewPostsState(true);
            Toast.makeText(this, "Error checking follow status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Fetches the user's posts from Firestore and updates the mood events adapter.
     */
    private void fetchUserPosts() {
        db.collection("Usermoods")
                .document(pageUserId)
                .collection("moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    int postCount = queryDocumentSnapshots.size();
                    // Update the posts TextView
                    textPosts.setText(String.valueOf(postCount));

                    showLoading(false);
                    allMoodEvents.clear(); // Clear previous events

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            MoodEvent moodEvent = MoodEvent.fromMap(doc.getData());
                            moodEvent.setDocumentId(doc.getId());

                            // Check if the post is public
                            Boolean isPublic = (Boolean) doc.getData().get("isPublic");
                            if (isPublic == null || !isPublic) {
                                continue; // Skip this post if it's not public
                            }

                            if (moodEvent.getTimestamp() != null) {
                                Date date = sourceFormat.parse(moodEvent.getTimestamp());
                                moodEvent.setDate(date);
                            }

                            // Build subtitle
                            StringBuilder subtitle = new StringBuilder();
                            if (moodEvent.getSocialSituation() != null &&
                                    !moodEvent.getSocialSituation().isEmpty()) {
                                if (subtitle.length() > 0) subtitle.append(" | ");
                                subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                            }
                            moodEvent.setSubtitle(subtitle.toString());

                            allMoodEvents.add(moodEvent);
                        } catch (ParseException e) {
                            Log.e("UsersProfile", "Error parsing timestamp", e);
                        }
                    }

                    if (allMoodEvents.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        moodEventAdapter.updateMoodEvents(allMoodEvents);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    /**
     * Displays or hides the "Follow to view posts" state.
     *
     * @param show true to show the empty state prompting follow; false to hide it.
     */
    private void showFollowToViewPostsState(boolean show) {
        recyclerUserPosts.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyStateView.setVisibility(show ? View.VISIBLE : View.GONE);

        if (show) {
            // Customize the empty state view for follow to view posts
            TextView emptyTitle = emptyStateView.findViewById(R.id.emptyStateTitle);
            TextView emptySubtitle = emptyStateView.findViewById(R.id.emptyStateSubtitle);

            emptyTitle.setText("Follow to see their posts");
            emptySubtitle.setText("Once you follow, their posts will appear here.");
        }
    }

    /**
     * Shows or hides the loading indicator.
     *
     * @param isLoading true to show the loading indicator; false to hide it.
     */
    private void showLoading(boolean isLoading) {
        progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerUserPosts.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    /**
     * Displays or hides the empty state view.
     *
     * @param isEmpty true to show the empty state view; false to show the posts.
     */
    private void showEmptyState(boolean isEmpty) {
        recyclerUserPosts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    /**
     * Checks the follow status between the active user and the profile user.
     * Updates the UI based on whether the active user is a follower, has requested to follow, or neither.
     */
    private void checkFollowStatus() {
        // 1) Check if activeUserId is in the pageUserId's followers
        DocumentReference followersDocRef = db.collection("users")
                .document(pageUserId)
                .collection("followers")
                .document("list");

        followersDocRef.get().addOnSuccessListener(followersDocSnap -> {
            if (followersDocSnap.exists()) {
                // The array might be named "followerIds" or something else in your code
                @SuppressWarnings("unchecked")
                java.util.List<String> followerIds =
                        (java.util.List<String>) followersDocSnap.get("followerIds");

                if (followerIds != null && followerIds.contains(activeUserId)) {
                    // The user is already in the followers array
                    showFollowingState();
                    loadUserPosts();
                    return; // no need to check requests
                }
            }

            // 2) If not a follower, check if they're in followRequests
            DocumentReference requestsDocRef = db.collection("users")
                    .document(pageUserId)
                    .collection("followRequests")
                    .document("list");

            requestsDocRef.get().addOnSuccessListener(requestsDocSnap -> {
                if (requestsDocSnap.exists()) {
                    @SuppressWarnings("unchecked")
                    java.util.List<String> followReqs =
                            (java.util.List<String>) requestsDocSnap.get("followReqs");

                    if (followReqs != null && followReqs.contains(activeUserId)) {
                        // The user has requested to follow
                        showRequestedState();
                        showFollowToViewPostsState(true);
                    } else {
                        // Not a follower and not in requests
                        showFollowState();
                        showFollowToViewPostsState(true);
                    }
                } else {
                    // No doc => definitely not in requests
                    showFollowState();
                    showFollowToViewPostsState(true);
                }
            });
        });
    }

    /**
     * Updates the UI to show the follow button state.
     */
    private void showFollowState() {
        buttonFollowStateFollow.setVisibility(View.VISIBLE);
        buttonFollowStateRequested.setVisibility(View.GONE);
        buttonFollowStateFollowing.setVisibility(View.GONE);
    }

    /**
     * Updates the UI to show that a follow request has been sent.
     */
    private void showRequestedState() {
        buttonFollowStateFollow.setVisibility(View.GONE);
        buttonFollowStateRequested.setVisibility(View.VISIBLE);
        buttonFollowStateFollowing.setVisibility(View.GONE);
    }

    /**
     * Updates the UI to show that the active user is following the profile user.
     */
    private void showFollowingState() {
        buttonFollowStateFollow.setVisibility(View.GONE);
        buttonFollowStateRequested.setVisibility(View.GONE);
        buttonFollowStateFollowing.setVisibility(View.VISIBLE);
    }

    /**
     * Callback when a filter is applied from the FilterDialog.
     *
     * @param timeFilter The selected time filter.
     * @param isHappy    Whether the happy filter is applied.
     * @param isSad      Whether the sad filter is applied.
     * @param isAngry    Whether the angry filter is applied.
     * @param isSurprised Whether the surprised filter is applied.
     * @param isAfraid   Whether the afraid filter is applied.
     * @param isDisgusted Whether the disgusted filter is applied.
     * @param isConfused Whether the confused filter is applied.
     * @param isShameful Whether the shameful filter is applied.
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
        // Intentionally left empty if not needed
    }

    /**
     * Callback when filtered results are available from the FilterDialog.
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
}