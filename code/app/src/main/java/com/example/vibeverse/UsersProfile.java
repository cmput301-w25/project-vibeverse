package com.example.vibeverse;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
    private ImageButton buttonBack;
    private RecyclerView recyclerUserPosts;
    private ProgressBar progressLoading;
    private View emptyStateView;

    String activeUserId;


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

        // Load user data
        loadUserData();

        // Load user posts (you can implement this based on your app's structure)
        loadUserPosts();

        // Set up back button
        buttonBack.setOnClickListener(v -> finish());

        // Set up follow button (placeholder for now)
        buttonFollowStateFollow.setOnClickListener(v -> {
// 1) Create the Notification object
            Notification followNotification = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                followNotification = new Notification(
                        "You have a new follow request!",      // content
                        LocalDateTime.now(),                   // dateTime
                        Notification.NotifType.FOLLOW_REQUEST, // notifType
                        activeUserId,                         // senderUserId (who is following)
                        pageUserId                             // receiverUserId (owner of this profile)
                );
            }

            // 2) Convert Notification object into a Map for Firestore
            //    (because LocalDateTime & Enum aren’t stored directly as-is)
            Map<String, Object> notifData = new HashMap<>();
            notifData.put("content", followNotification.getContent());
            notifData.put("dateTime", followNotification.getDateTime().toString());
            notifData.put("notifType", followNotification.getNotifType().name());
            notifData.put("senderUserId", followNotification.getSenderUserId());
            notifData.put("receiverUserId", followNotification.getReceiverUserId());
            notifData.put("isRead", followNotification.isRead());

            // 3) Get a reference to the recipient user’s doc & new_notifications subcollection
            DocumentReference recipientUserRef = db.collection("users").document(pageUserId);

            recipientUserRef.collection("notifications")
                    .add(notifData)
                    .addOnSuccessListener(docRef -> {
                        // 5) Increment the user's newNotificationCount
                        recipientUserRef.update("newNotificationCount",
                                com.google.firebase.firestore.FieldValue.increment(1));

                        // 6) Also add the sender's ID to the "followRequests" subcollection
                        //    (sibling of "notifications")
                        Map<String, Object> followRequestUpdate = new HashMap<>();
                        followRequestUpdate.put("followReqs",
                                com.google.firebase.firestore.FieldValue.arrayUnion(activeUserId));

                        // We'll store it in a doc named "list" in the followRequests subcollection
                        recipientUserRef.collection("followRequests")
                                .document("list")
                                // Use merge() so we don't overwrite existing IDs
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
            DocumentReference recipientUserRef = db.collection("users").document(pageUserId);

            // Remove from followers
            recipientUserRef.collection("followers")
                    .document("list")
                    .update("followerIds",
                            com.google.firebase.firestore.FieldValue.arrayRemove(activeUserId))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(UsersProfile.this, "Unfollowed.", Toast.LENGTH_SHORT).show();
                        showFollowState();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(UsersProfile.this, "Failed to unfollow: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        });


    }

    private void initViews() {
        profilePicture = findViewById(R.id.profilePicture);
        textName = findViewById(R.id.textName);
        textUsername = findViewById(R.id.textUsername);
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

        // Set up RecyclerView
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns for grid display
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
                    Toast.makeText(UsersProfile.this, "Error loading profile: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void updateUI(DocumentSnapshot document) {
        User user = document.toObject(User.class);
        if (user != null) {
            // Set name and username
            textName.setText(user.getFullName());
            textUsername.setText("@" + user.getUsername());

            // Set bio if available
            if (user.getBio() != null && !user.getBio().isEmpty()) {
                textBioContent.setText(user.getBio());
            } else {
                textBioContent.setText("");
            }

            // Load profile picture
            if (user.isHasProfilePic() && user.getProfilePicUri() != null && !user.getProfilePicUri().isEmpty()) {
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
        // This is a placeholder for loading user posts
        // You'll need to implement this based on your data structure
        showEmptyState(true);

        // Example of how you might load posts:
        /*
        db.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    showEmptyState(true);
                } else {
                    showEmptyState(false);
                    List<Post> posts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Post post = doc.toObject(Post.class);
                        posts.add(post);
                    }
                    // Set up your adapter and display posts
                    // PostAdapter adapter = new PostAdapter(posts);
                    // recyclerUserPosts.setAdapter(adapter);
                }
            });
        */
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