package com.example.vibeverse;

import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
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

public class UsersProfile extends AppCompatActivity {

    private String pageUserId;
    private FirebaseFirestore db;

    // UI Elements
    private CircleImageView profilePicture;

    private TextView textName, textUsername, textBioContent, textFollowers, textFollowing;

    private Button buttonFollowStateFollow;
    private Button buttonFollowStateRequested;
    private Button buttonFollowStateFollowing;

    private TextView  textTopUsername;
    private Button buttonFollow;

    private ImageButton buttonBack;
    private RecyclerView recyclerUserPosts;
    private ProgressBar progressLoading;
    private View emptyStateView;


    String activeUserId;


    private MoodEventAdapter moodEventAdapter;
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());


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
            String profileUsername = textUsername.getText().toString();

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
                                        activeUserRef.update("followingCount", FieldValue.increment(-1));
                                        Toast.makeText(UsersProfile.this, "Unfollowed.", Toast.LENGTH_SHORT).show();
                                        showFollowState();
                                        alertDialog.dismiss();
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



    }

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

        recyclerUserPosts.setLayoutManager(new LinearLayoutManager(this));
    }

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

    private void loadUserPosts() {
        showLoading(true);
        db.collection("Usermoods")
                .document(pageUserId)
                .collection("moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    showLoading(false);
                    List<MoodEvent> moodEvents = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            MoodEvent moodEvent = MoodEvent.fromMap(doc.getData());
                            moodEvent.setDocumentId(doc.getId());

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

                            moodEvents.add(moodEvent);
                        } catch (ParseException e) {
                            Log.e("UsersProfile", "Error parsing timestamp", e);
                        }
                    }

                    if (moodEvents.isEmpty()) {
                        showEmptyState(true);
                    } else {
                        showEmptyState(false);
                        moodEventAdapter.updateMoodEvents(moodEvents);
                    }
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Error loading posts: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    showEmptyState(true);
                });
    }

    private void showLoading(boolean isLoading) {
        progressLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (isLoading) {
            recyclerUserPosts.setVisibility(View.GONE);
            emptyStateView.setVisibility(View.GONE);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        recyclerUserPosts.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

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
                    } else {
                        // Not a follower and not in requests
                        showFollowState();
                    }
                } else {
                    // No doc => definitely not in requests
                    showFollowState();
                }
            });
        });
    }

    private void showFollowState() {
        buttonFollowStateFollow.setVisibility(View.VISIBLE);
        buttonFollowStateRequested.setVisibility(View.GONE);
        buttonFollowStateFollowing.setVisibility(View.GONE);
    }

    private void showRequestedState() {
        buttonFollowStateFollow.setVisibility(View.GONE);
        buttonFollowStateRequested.setVisibility(View.VISIBLE);
        buttonFollowStateFollowing.setVisibility(View.GONE);
    }

    private void showFollowingState() {
        buttonFollowStateFollow.setVisibility(View.GONE);
        buttonFollowStateRequested.setVisibility(View.GONE);
        buttonFollowStateFollowing.setVisibility(View.VISIBLE);
    }


}