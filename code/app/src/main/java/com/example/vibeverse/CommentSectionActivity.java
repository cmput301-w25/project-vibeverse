package com.example.vibeverse;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;


import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentSectionActivity extends AppCompatActivity {

    private TextView textTitle, textSubtitle, textEmoji, socialSituationView;
    private RecyclerView recyclerComments;
    private EditText editComment;
    private Button buttonSendComment;
    private ImageView imagePost; // Added for loading the image

    private View imageContainer, moodColorStrip;

    private CardView emojiContainer;
    private ProgressBar intensityProgressBar;
    private ImageButton postMenuButton;

    private View replyBanner;

    private TextView replyBannerText;

    private ImageButton replyBannerClose;
    private FirebaseAuth mAuth;

    private FirebaseFirestore db;

    private String moodDocId;       // The mood document ID
    private String moodUserId;   // The user ID owning the mood

    private CommentAdapter commentAdapter;
    private List<Comment> commentList;

    private ListenerRegistration commentListener;

    private Comment replyingToComment = null; // null means not in reply mode



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_section);
        findViewById(android.R.id.content).setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));

        // Retrieve post data from the intent
        Intent intent = getIntent();
        String reasonWhy = intent.getStringExtra("reasonWhy");
        String moodTitle = intent.getStringExtra("moodTitle");
        String emoji = intent.getStringExtra("emoji");
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        String photoUri = intent.getStringExtra("photoUri"); // Get the image URI if available
        Boolean hasPhoto = intent.getBooleanExtra("hasPhoto", false);
        String socialSituation = intent.getStringExtra("socialSituation");
        moodUserId = intent.getStringExtra("moodOwnerId");
        moodDocId = intent.getStringExtra("moodDocId");
        int moodColor = intent.getIntExtra("moodColor", 0);
        int intensity = intent.getIntExtra("intensity", 0);
        int lighterMoodColor = intent.getIntExtra("lighterMoodColor", 0);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // Find views inside the included post layout
        View postView = findViewById(R.id.includedPost);
        textTitle = postView.findViewById(R.id.textTitle);
        textSubtitle = postView.findViewById(R.id.textSubtitle);
        textEmoji = postView.findViewById(R.id.textEmoji);
        imagePost = postView.findViewById(R.id.imagePost); // Image view for loading photo
        imageContainer = postView.findViewById(R.id.imageContainer);
        socialSituationView = findViewById(R.id.socialText);
        postMenuButton = findViewById(R.id.buttonPostMenu);
        moodColorStrip = findViewById(R.id.moodColorStrip);
        emojiContainer = findViewById(R.id.emojiContainer);
        intensityProgressBar = findViewById(R.id.intensityProgressBar);
        replyBanner = findViewById(R.id.replyBanner);
        replyBannerText = findViewById(R.id.replyBannerText);
        replyBannerClose = findViewById(R.id.replyBannerClose);


        // Populate the post view with passed data
        textTitle.setText(reasonWhy);

        // Format the timestamp into a readable date string
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
        String dateString = formatter.format(new Date(timestamp));
        textSubtitle.setText(dateString + " • " + moodTitle);
        textEmoji.setText(emoji);
        socialSituationView.setText(socialSituation);

        // Load the image if a valid URI exists
        if (hasPhoto && photoUri != null && !photoUri.isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(Uri.parse(photoUri))
                    .into(imagePost);
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        postMenuButton.setVisibility(View.GONE);


        if (moodColorStrip != null) {
            moodColorStrip.setBackgroundColor(moodColor);
        }

        // Tint the emoji container with a lighter version of the mood color.
        if (emojiContainer != null) {
            emojiContainer.setCardBackgroundColor(lighterMoodColor);
        }

        // Set the intensity progress bar.
        if (intensityProgressBar != null) {
            intensityProgressBar.setProgress(intensity);
            intensityProgressBar.setProgressTintList(ColorStateList.valueOf(moodColor));
        }

        // Setup the comment section
        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList, moodUserId, moodDocId);
        recyclerComments.setAdapter(commentAdapter);



        commentAdapter.setOnReplyClickListener(comment -> {
            replyingToComment = comment;

            String authorUserId = comment.getAuthorUserId();
            db.collection("users").document(authorUserId).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String username = documentSnapshot.getString("username");
                    replyBannerText.setText("Replying to " + username);
                } else {
                    replyBannerText.setText("Replying to Unknown");
                }
            }).addOnFailureListener(e -> {
                replyBannerText.setText("Replying to Unknown");
            });


            replyBanner.setVisibility(View.VISIBLE);
        });

        replyBannerClose.setOnClickListener(v -> {
            replyingToComment = null;
            replyBanner.setVisibility(View.GONE);
        });



        // Setup the send comment button
        editComment = findViewById(R.id.editComment);
        buttonSendComment = findViewById(R.id.buttonSendComment);
        buttonSendComment.setBackgroundTintList(null);
        buttonSendComment.setOnClickListener(v -> postComment());

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Attach a snapshot listener to the comments collection for real-time updates.
        commentListener = db.collection("Usermoods")
                .document(moodUserId)
                .collection("moods")
                .document(moodDocId)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(CommentSectionActivity.this, "Failed to load comments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (querySnapshot != null) {
                        commentList.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Comment comment = doc.toObject(Comment.class);
                            if (comment != null) {
                                commentList.add(comment);
                            }
                        }
                        commentAdapter.notifyDataSetChanged();
                        // Optionally scroll to the bottom.
                        recyclerComments.smoothScrollToPosition(commentList.size() - 1);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Remove the listener when the activity is no longer visible.
        if (commentListener != null) {
            commentListener.remove();
        }
    }


    /**
     * Posts a comment to Firestore under the appropriate mood event.
     */
    private void postComment() {
        String commentText = editComment.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(CommentSectionActivity.this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous";
        String commentId = db.collection("Usermoods")
                .document(moodUserId)
                .collection("moods")
                .document(moodDocId)
                .collection("comments")
                .document().getId();

        // If replying, set parentCommentId to the comment being replied to; else "N/A"
        String parentCommentId = (replyingToComment != null) ? replyingToComment.getCommentId() : "N/A";

        Comment newComment = new Comment(
                commentId,
                commentText,
                currentUserId,
                new Date(),
                moodDocId,
                parentCommentId
        );

        if (replyingToComment != null) {
            // Save as a reply in the parent's "replies" subcollection.
            db.collection("Usermoods")
                    .document(moodUserId)
                    .collection("moods")
                    .document(moodDocId)
                    .collection("comments")
                    .document(replyingToComment.getCommentId())
                    .collection("replies")
                    .document(commentId)
                    .set(newComment)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(CommentSectionActivity.this, "Reply added", Toast.LENGTH_SHORT).show();
                        editComment.setText("");

                        // Store the parent comment's author before resetting reply mode.
                        String parentAuthorId = replyingToComment.getAuthorUserId();

                        // Reset reply mode.
                        replyingToComment = null;
                        replyBanner.setVisibility(View.GONE);

                        // Only send a notification if the reply is not to your own comment.
                        if (!parentAuthorId.equals(currentUserId)) {
                            // Retrieve active user's username.
                            db.collection("users").document(currentUserId)
                                    .get()
                                    .addOnSuccessListener(docSnapshot -> {
                                        if (docSnapshot.exists()) {
                                            String activeUsername = docSnapshot.getString("username");
                                            if (activeUsername == null || activeUsername.isEmpty()) {
                                                Toast.makeText(CommentSectionActivity.this,
                                                        "Active user username not found.",
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Retrieve the post owner's username.
                                            db.collection("users").document(moodUserId)
                                                    .get()
                                                    .addOnSuccessListener(postOwnerSnapshot -> {
                                                        String postOwnerUsername = "someone";
                                                        if (postOwnerSnapshot.exists()) {
                                                            postOwnerUsername = postOwnerSnapshot.getString("username");
                                                            if (postOwnerUsername == null || postOwnerUsername.isEmpty()) {
                                                                postOwnerUsername = "someone";
                                                            }
                                                        }

                                                        // Truncate the reply text for the notification preview.
                                                        String truncatedReply = commentText.length() > 50
                                                                ? commentText.substring(0, 50) + "..."
                                                                : commentText;

                                                        // Build the notification message.
                                                        String notificationContent = activeUsername +
                                                                " replied to your comment on " + postOwnerUsername +
                                                                "'s post: \"" + truncatedReply + "\"";

                                                        // Reference the parent comment's author user document.
                                                        DocumentReference recipientUserRef = db.collection("users").document(parentAuthorId);
                                                        // Create a new notification document reference with an auto-generated ID.
                                                        DocumentReference newNotifDocRef = recipientUserRef.collection("notifications").document();
                                                        String notifId = newNotifDocRef.getId();

                                                        // Create the Notification object with type COMMENT_REPLIED_TO.
                                                        Notification replyNotification;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            replyNotification = new Notification(
                                                                    notifId,
                                                                    notificationContent,
                                                                    LocalDateTime.now().toString(),
                                                                    Notification.NotifType.COMMENT_REPLIED_TO,
                                                                    currentUserId,
                                                                    parentAuthorId,
                                                                    moodDocId
                                                            );
                                                        } else {
                                                            replyNotification = new Notification(
                                                                    notifId,
                                                                    notificationContent,
                                                                    new Date().toString(),
                                                                    Notification.NotifType.COMMENT_REPLIED_TO,
                                                                    currentUserId,
                                                                    parentAuthorId,
                                                                    moodDocId
                                                            );
                                                        }

                                                        // Convert the Notification object into a Map.
                                                        Map<String, Object> notifData = new HashMap<>();
                                                        notifData.put("content", replyNotification.getContent());
                                                        notifData.put("dateTime", replyNotification.getDateTime());
                                                        notifData.put("notifType", replyNotification.getNotifType().name());
                                                        notifData.put("senderUserId", replyNotification.getSenderUserId());
                                                        notifData.put("receiverUserId", replyNotification.getReceiverUserId());
                                                        notifData.put("isRead", replyNotification.isRead());
                                                        notifData.put("requestStatus", replyNotification.getRequestStatus());
                                                        notifData.put("moodEventId", replyNotification.getMoodEventId());
                                                        notifData.put("id", notifId);

                                                        // Save the notification to the parent comment's author's notifications subcollection.
                                                        recipientUserRef.collection("notifications")
                                                                .document(notifId)
                                                                .set(notifData)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    // Optionally, increment the newNotificationCount field.
                                                                    recipientUserRef.update("newNotificationCount",
                                                                            com.google.firebase.firestore.FieldValue.increment(1));
                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Toast.makeText(CommentSectionActivity.this,
                                                                            "Failed to send reply notification: " + e.getMessage(),
                                                                            Toast.LENGTH_SHORT).show();
                                                                });
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(CommentSectionActivity.this,
                                                                "Error retrieving post owner's data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            Toast.makeText(CommentSectionActivity.this,
                                                    "Active user data not found.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CommentSectionActivity.this,
                                                "Error retrieving active user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CommentSectionActivity.this, "Error adding reply: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Save as a top-level comment
            db.collection("Usermoods")
                    .document(moodUserId)
                    .collection("moods")
                    .document(moodDocId)
                    .collection("comments")
                    .document(commentId)
                    .set(newComment)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(CommentSectionActivity.this, "Comment added", Toast.LENGTH_SHORT).show();
                        editComment.setText("");
                        // Optionally initialize a "replies" subcollection (if needed)
                        db.collection("Usermoods")
                                .document(moodUserId)
                                .collection("moods")
                                .document(moodDocId)
                                .collection("comments")
                                .document(commentId)
                                .collection("replies")
                                .document("init")
                                .set(Collections.singletonMap("init", true));

                        // Only generate notification if the commenter is not commenting on their own post.
                        if (!moodUserId.equals(currentUserId)) {
                            // Retrieve the active user's username.
                            db.collection("users").document(currentUserId)
                                    .get()
                                    .addOnSuccessListener(docSnapshot -> {
                                        if (docSnapshot.exists()) {
                                            String activeUsername = docSnapshot.getString("username");
                                            if (activeUsername == null || activeUsername.isEmpty()) {
                                                Toast.makeText(CommentSectionActivity.this,
                                                        "Active user username not found.",
                                                        Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            // Truncate comment for notification preview (e.g., 50 characters)
                                            String truncatedComment = commentText.length() > 50
                                                    ? commentText.substring(0, 50) + "..."
                                                    : commentText;

                                            String notificationContent = activeUsername + " commented on your post: \"" + truncatedComment + "\"";

                                            // Reference the post author's user document.
                                            DocumentReference recipientUserRef = db.collection("users").document(moodUserId);
                                            // Create a new notification document reference with an auto-generated ID.
                                            DocumentReference newNotifDocRef = recipientUserRef.collection("notifications").document();
                                            String notifId = newNotifDocRef.getId();

                                            // Create the Notification object.
                                            Notification commentNotification;
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                commentNotification = new Notification(
                                                        notifId,
                                                        notificationContent,
                                                        LocalDateTime.now().toString(),
                                                        Notification.NotifType.POST_COMMENTED_ON,
                                                        currentUserId,
                                                        moodUserId,
                                                        moodDocId
                                                );
                                            } else {
                                                commentNotification = new Notification(
                                                        notifId,
                                                        notificationContent,
                                                        new Date().toString(),
                                                        Notification.NotifType.POST_COMMENTED_ON,
                                                        currentUserId,
                                                        moodUserId,
                                                        moodDocId
                                                );
                                            }

                                            // Convert the Notification object into a Map.
                                            Map<String, Object> notifData = new HashMap<>();
                                            notifData.put("content", commentNotification.getContent());
                                            notifData.put("dateTime", commentNotification.getDateTime());
                                            notifData.put("notifType", commentNotification.getNotifType().name());
                                            notifData.put("senderUserId", commentNotification.getSenderUserId());
                                            notifData.put("receiverUserId", commentNotification.getReceiverUserId());
                                            notifData.put("isRead", commentNotification.isRead());
                                            notifData.put("requestStatus", commentNotification.getRequestStatus());
                                            notifData.put("moodEventId", commentNotification.getMoodEventId());
                                            notifData.put("id", notifId);

                                            // Save the notification to the post author's notifications subcollection.
                                            recipientUserRef.collection("notifications")
                                                    .document(notifId)
                                                    .set(notifData)
                                                    .addOnSuccessListener(aVoid -> {
                                                        // Optionally, increment the newNotificationCount field for the post author.
                                                        recipientUserRef.update("newNotificationCount",
                                                                com.google.firebase.firestore.FieldValue.increment(1));
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        Toast.makeText(CommentSectionActivity.this,
                                                                "Failed to send notification: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    });
                                        } else {
                                            Toast.makeText(CommentSectionActivity.this,
                                                    "Active user data not found.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(CommentSectionActivity.this,
                                                "Error retrieving active user: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(CommentSectionActivity.this, "Error adding comment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });



        }
    }


}
