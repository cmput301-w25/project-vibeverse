package com.example.vibeverse;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
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

    private TextView textTitle, textSubtitle, socialSituationView;
    private RecyclerView recyclerComments;
    private EditText editComment;
    private Button buttonSendComment;
    private ImageView imagePost, imageEmoji; // Added for loading the image

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

    private String selectedTheme;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_section);
        findViewById(android.R.id.content).setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));


        // Retrieve only moodOwnerId and moodDocId from the intent.
        moodUserId = getIntent().getStringExtra("moodOwnerId");
        moodDocId = getIntent().getStringExtra("moodDocId");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Find views inside the included post layout
        View postView = findViewById(R.id.includedPost);
        textTitle = postView.findViewById(R.id.textTitle);
        textSubtitle = postView.findViewById(R.id.textSubtitle);
        imageEmoji = postView.findViewById(R.id.imageEmoji);
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

        postMenuButton.setVisibility(View.GONE);

        fetchUserTheme(() -> loadMoodDetails());


        // Setup the comment section
        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        commentList = new ArrayList<>();
        commentAdapter = new CommentAdapter(this, commentList, moodUserId, moodDocId);
        recyclerComments.setAdapter(commentAdapter);

        commentAdapter.setOnReplyClickListener(comment -> {
            replyingToComment = comment;
            String authorUserId = comment.getAuthorUserId();
            db.collection("users").document(authorUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            replyBannerText.setText("Replying to " + username);
                        } else {
                            replyBannerText.setText("Replying to Unknown");
                        }
                    })
                    .addOnFailureListener(e -> replyBannerText.setText("Replying to Unknown"));
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
                        if (!commentList.isEmpty()) {
                            recyclerComments.smoothScrollToPosition(commentList.size() - 1);
                        }

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


    private void fetchUserTheme(final Runnable onComplete) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("selectedTheme")) {
                        selectedTheme = documentSnapshot.getString("selectedTheme");
                    } else {
                        selectedTheme = "default";
                    }
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    selectedTheme = "default";
                    onComplete.run();
                });
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

        AchievementChecker achievementChecker = new AchievementChecker(currentUserId);

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

                        achievementChecker.checkAch14();

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
                                                                    moodDocId,
                                                                    moodUserId
                                                            );
                                                        } else {
                                                            replyNotification = new Notification(
                                                                    notifId,
                                                                    notificationContent,
                                                                    new Date().toString(),
                                                                    Notification.NotifType.COMMENT_REPLIED_TO,
                                                                    currentUserId,
                                                                    parentAuthorId,
                                                                    moodDocId,
                                                                    moodUserId
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
                                                        notifData.put("moodOwnerId", replyNotification.getMoodOwnerId());
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

                        achievementChecker.checkAch14();

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
                                                        moodDocId,
                                                        moodUserId
                                                );
                                            } else {
                                                commentNotification = new Notification(
                                                        notifId,
                                                        notificationContent,
                                                        new Date().toString(),
                                                        Notification.NotifType.POST_COMMENTED_ON,
                                                        currentUserId,
                                                        moodUserId,
                                                        moodDocId,
                                                        moodUserId
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
                                            notifData.put("moodOwnerId", commentNotification.getMoodOwnerId());
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

    /**
     * Lightens a given color by blending it with white.
     *
     * @param color  The original color.
     * @param factor The factor by which to lighten (0.0 to 1.0).
     * @return The lightened color.
     */
    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }


    private int getEmojiResourceId(String moodId, String theme) {
        String resourceName = "emoji_" + moodId.toLowerCase() + "_" + theme.toLowerCase();
        return getResources().getIdentifier(resourceName, "drawable", getPackageName());
    }


    private void loadMoodDetails() {
        db.collection("Usermoods")
                .document(moodUserId)
                .collection("moods")
                .document(moodDocId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String reasonWhy = documentSnapshot.getString("reasonWhy");
                        String moodTitle = documentSnapshot.getString("mood"); // Mood title from "mood" field
                        // We no longer fetch emoji as text since it's computed locally.
                        Boolean hasPhoto = documentSnapshot.getBoolean("hasPhoto");
                        Long intensity = documentSnapshot.getLong("intensity");
                        String socialSituation = documentSnapshot.getString("socialSituation");
                        String timestampStr = documentSnapshot.getString("timestamp");

                        // Update UI elements.
                        textTitle.setText(reasonWhy);
                        textSubtitle.setText(timestampStr + " • " + moodTitle);
                        socialSituationView.setText(socialSituation);

                        // Compute mood color.
                        String colorName = moodTitle.toLowerCase() + "_color";
                        int colorResId = getResources().getIdentifier(colorName, "color", getPackageName());
                        int moodColor = ContextCompat.getColor(this, colorResId);
                        if (moodColorStrip != null) {
                            moodColorStrip.setBackgroundColor(moodColor);
                        }
                        if (emojiContainer != null) {
                            emojiContainer.setCardBackgroundColor(lightenColor(moodColor, 0.7f));
                        }
                        if (intensityProgressBar != null && intensity != null) {
                            intensityProgressBar.setProgress(intensity.intValue());
                            intensityProgressBar.setProgressTintList(ColorStateList.valueOf(moodColor));
                        }

                        // Set the PNG emoji using the helper method.
                        int emojiResId = getEmojiResourceId(moodTitle, selectedTheme);
                        imageEmoji.setImageResource(emojiResId);

                        // Load the image if available.
                        String photoUri = documentSnapshot.getString("photoUri");
                        if (hasPhoto != null && hasPhoto && photoUri != null && !photoUri.isEmpty()) {
                            imageContainer.setVisibility(View.VISIBLE);
                            Glide.with(this)
                                    .load(Uri.parse(photoUri))
                                    .into(imagePost);
                        } else {
                            imageContainer.setVisibility(View.GONE);
                        }
                    } else {
                        Toast.makeText(CommentSectionActivity.this, "Mood data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CommentSectionActivity.this, "Error retrieving mood: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


}
