package com.example.vibeverse;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.format.DateTimeFormatter;
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
            dateTimeText.setText(notification.getDateTime());
            acceptButton.setBackgroundTintList(null);
            rejectButton.setBackgroundTintList(null);



            // Differentiate unread notifications visually
            if (!notification.isRead()) {
                // For example, set a special background for unread notifications
                container.setBackgroundResource(R.drawable.unread_notification_background);
            } else {
                container.setBackgroundResource(R.drawable.read_notification_background);
            }

            // Show accept/reject buttons only if the notification type is FOLLOW_REQUEST
            if (notification.getNotifType() == Notification.NotifType.FOLLOW_REQUEST) {
                acceptButton.setVisibility(View.VISIBLE);
                rejectButton.setVisibility(View.VISIBLE);
            } else {
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

            // Set click listeners for the follow request buttons
            acceptButton.setOnClickListener(v -> {
                // Add logic for accepting the follow request
                // e.g., update Firestore, add to following/followers collections, etc.
            });

            rejectButton.setOnClickListener(v -> {
                // Add logic for rejecting the follow request
                // e.g., remove the notification or update its state.
            });
        }
    }
}
