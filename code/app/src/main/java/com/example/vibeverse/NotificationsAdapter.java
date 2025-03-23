package com.example.vibeverse;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;


public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder> {
    private List<Notification> notificationList;
    private Context context;
    private FirebaseFirestore db;

    public NotificationsAdapter(Context context, List<Notification> notifications) {
        this.context = context;
        this.notificationList = notifications;
        db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);
        holder.bind(notification);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {
        private de.hdodenhof.circleimageview.CircleImageView profileImage;
        private TextView contentText;
        private TextView dateTimeText;
        private Button acceptButton;
        private Button rejectButton;
        private LinearLayout buttonContainer;
        private View container; // This can be the CardView or main container for background changes

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.notificationContainer);
            profileImage = itemView.findViewById(R.id.profilePicture);
            contentText = itemView.findViewById(R.id.notificationContent);
            dateTimeText = itemView.findViewById(R.id.notificationDateTime);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            rejectButton = itemView.findViewById(R.id.rejectButton);
            buttonContainer = itemView.findViewById(R.id.buttonContainer);
        }

        public void bind(Notification notification) {
            // Set the notification text and date
            contentText.setText(notification.getContent());
            String formattedDateTime = getFormattedDateTime(notification.getDateTime());
            dateTimeText.setText(formattedDateTime);
            acceptButton.setBackgroundTintList(null);
            rejectButton.setBackgroundTintList(null);



            // Differentiate unread notifications visually
            if (!notification.isRead()) {
                // For example, set a special background for unread notifications
                container.setBackgroundResource(R.drawable.unread_notification_background);
            } else {
                container.setBackgroundResource(R.drawable.read_notification_background);
            }

            // For follow request notifications, check if an action has already been taken.
            if (notification.getNotifType() == Notification.NotifType.FOLLOW_REQUEST) {
                String requestStatus = notification.getRequestStatus(); // e.g., "accepted", "rejected", or null
                if ("pending".equals(requestStatus)) {
                    // No action taken yet: show the accept and reject buttons.
                    acceptButton.setVisibility(View.VISIBLE);
                    rejectButton.setVisibility(View.VISIBLE);
                }
                else if (requestStatus != null) {
                    // An action has been taken: display the corresponding icon.
                    buttonContainer.removeAllViews();
                    ImageView statusIcon = new ImageView(context);
                    if (requestStatus.equals("accepted")) {
                        statusIcon.setImageResource(R.drawable.ic_accepted);
                    } else if (requestStatus.equals("rejected")) {
                        statusIcon.setImageResource(R.drawable.ic_rejected);
                    }
                    statusIcon.setAlpha(0.5f);
                    buttonContainer.addView(statusIcon);
                }
            } else {
                // For other notification types, hide the buttons.
                acceptButton.setVisibility(View.GONE);
                rejectButton.setVisibility(View.GONE);
            }


            // Load the sender's profile picture using their user id.
            // This assumes that in the users collection, the profile picture URL is stored under "profilePicUri".
            db.collection("users")
                    .document(notification.getSenderUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String profilePicUrl = documentSnapshot.getString("profilePicUri");
                            if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                                // Using Glide to load the image:
                                Glide.with(context)
                                        .load(profilePicUrl)
                                        .placeholder(R.drawable.user_icon)
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.user_icon);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // In case of error, set a default icon
                        profileImage.setImageResource(R.drawable.user_icon);
                    });

            container.setOnClickListener(v -> {
                // Only handle clicks for FOLLOW_REQUEST notifications
                if (notification.getNotifType() == Notification.NotifType.FOLLOW_REQUEST) {
                    // Get the sender's user ID
                    String senderUserId = notification.getSenderUserId();

                    // Create an intent to open the UsersProfile activity
                    Intent intent = new Intent(context, UsersProfile.class);
                    intent.putExtra("userId", senderUserId);
                    context.startActivity(intent);
                }
                else if (notification.getNotifType() == Notification.NotifType.POST_COMMENTED_ON ||
                        notification.getNotifType() == Notification.NotifType.COMMENT_REPLIED_TO) {
                    // Get the mood event ID from the notification
                    String moodEventId = notification.getMoodEventId();
                    String moodOwnerId = notification.getMoodOwnerId();

                    // Create an intent to open the CommentSectionActivity
                    Intent intent = new Intent(context, CommentSectionActivity.class);
                    intent.putExtra("moodOwnerId", moodOwnerId);
                    intent.putExtra("moodDocId", moodEventId);
                    context.startActivity(intent);
                }
            });

            // Set click listeners for the follow request buttons
            acceptButton.setOnClickListener(v -> {
                buttonContainer.removeAllViews();
                ImageView statusIcon = new ImageView(context);
                statusIcon.setImageResource(R.drawable.ic_accepted);
                statusIcon.setAlpha(0.5f);
                buttonContainer.addView(statusIcon);

                // Get current active user's id.
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Remove sender id from current user's followRequests subcollection.
                db.collection("users")
                        .document(currentUserId)
                        .collection("followRequests")
                        .document("list")
                        .update("followReqs", FieldValue.arrayRemove(notification.getSenderUserId()));

                // Add sender id to current user's followers subcollection.
                db.collection("users")
                        .document(currentUserId)
                        .collection("followers")
                        .document("list")
                        .update("followerIds", FieldValue.arrayUnion(notification.getSenderUserId()));

                // Add current user's id to sender's following subcollection.
                db.collection("users")
                        .document(notification.getSenderUserId())
                        .collection("following")
                        .document("list")
                        .update("followingIds", FieldValue.arrayUnion(currentUserId));

                // Increment active user's follower count by 1.
                db.collection("users")
                        .document(currentUserId)
                        .update("followerCount", FieldValue.increment(1));

                // Increment notification sender's following count by 1.
                db.collection("users")
                        .document(notification.getSenderUserId())
                        .update("followingCount", FieldValue.increment(1));

                db.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .whereEqualTo("id", notification.getId())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Get the first matching document and update it.
                                querySnapshot.getDocuments().get(0)
                                        .getReference()
                                        .update("requestStatus", "accepted");
                            }
                        });

            });

            rejectButton.setOnClickListener(v -> {
                // Replace the buttons with a greyed-out rejected icon.
                buttonContainer.removeAllViews();
                ImageView statusIcon = new ImageView(context);
                statusIcon.setImageResource(R.drawable.ic_rejected);
                statusIcon.setAlpha(0.5f);
                buttonContainer.addView(statusIcon);

                // Get current active user's id.
                String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                // Remove sender id from current user's followRequests subcollection.
                db.collection("users")
                        .document(currentUserId)
                        .collection("followRequests")
                        .document("list")
                        .update("followReqs", FieldValue.arrayRemove(notification.getSenderUserId()));

                db.collection("users")
                        .document(currentUserId)
                        .collection("notifications")
                        .whereEqualTo("id", notification.getId())
                        .get()
                        .addOnSuccessListener(querySnapshot -> {
                            if (!querySnapshot.isEmpty()) {
                                // Get the first matching document and update it.
                                querySnapshot.getDocuments().get(0)
                                        .getReference()
                                        .update("requestStatus", "rejected");
                            }
                        });
            });
        }
    }

    private String getFormattedDateTime(String isoDateTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Parse the ISO string. Adjust the pattern if your input format changes.

            DateTimeFormatter inputFormatter = new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .optionalStart()
                    .appendFraction(java.time.temporal.ChronoField.NANO_OF_SECOND, 3, 6, true)
                    .optionalEnd()
                    .toFormatter();
          
            LocalDateTime dateTime = LocalDateTime.parse(isoDateTime, inputFormatter);

            // Get the day and its ordinal suffix
            int day = dateTime.getDayOfMonth();
            String daySuffix = getDayOfMonthSuffix(day);

            // Format the date part. We use "MMMM d, yyyy" then insert the suffix.
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            String formattedDate = dateTime.format(dateFormatter);
            // Replace the day number with the day plus its suffix
            formattedDate = formattedDate.replaceFirst("\\d+", day + daySuffix);

            // Format the time part as "hh:mm a" (e.g., 11:05 PM)
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
            String formattedTime = dateTime.format(timeFormatter);

            return formattedDate + " " + formattedTime;
        }
        return null;
    }

    private String getDayOfMonthSuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        switch (day % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }
}
