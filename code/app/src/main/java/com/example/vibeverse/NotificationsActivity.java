package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

/**
 * NotificationsActivity displays a list of notifications for the current user.
 * <p>
 * It retrieves notifications from Firestore, displays them in a RecyclerView using NotificationsAdapter,
 * marks notifications as read, and provides navigation back to HomePage via a back button.
 * </p>
 */
public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerNotifications;
    private NotificationsAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    private ImageButton backButton;

    private String currentUserId;

    /**
     * Called when the activity is created.
     * <p>
     * Initializes UI components, Firebase instances, loads notifications from Firestore,
     * marks them as read, and sets up bottom navigation.
     * </p>
     *
     * @param savedInstanceState the previously saved state of the activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_page);  // your notifications activity layout

        recyclerNotifications = findViewById(R.id.recyclerNotifications);
        recyclerNotifications.setLayoutManager(new LinearLayoutManager(this));

        notificationList = new ArrayList<>();
        adapter = new NotificationsAdapter(this, notificationList);
        recyclerNotifications.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadNotifications();
        markNotificationsAsRead();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        backButton = findViewById(R.id.buttonBack);

        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HomePage.class);
            startActivity(intent);
        });
    }

    /**
     * Loads notifications for the current user from Firestore and updates the RecyclerView.
     */
    private void loadNotifications() {
        // Query the notifications subcollection sorted by dateTime descending
        db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .orderBy("dateTime", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    notificationList.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // Assuming your Notification class has a default constructor and proper getters/setters.
                        Notification notification = doc.toObject(Notification.class);
                        notificationList.add(notification);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NotificationsActivity.this, "Error loading notifications", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Marks all notifications for the current user as read and resets the new notification count.
     */
    private void markNotificationsAsRead() {
        db.collection("users")
                .document(currentUserId)
                .collection("notifications")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.update(doc.getReference(), "isRead", true);
                    }
                    batch.update(db.collection("users").document(currentUserId), "newNotificationCount", 0);
                    batch.commit().addOnCompleteListener(task -> {
                    });
                });
    }
}
