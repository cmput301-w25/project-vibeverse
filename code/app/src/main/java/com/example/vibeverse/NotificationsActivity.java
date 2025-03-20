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

public class NotificationsActivity extends AppCompatActivity {
    private RecyclerView recyclerNotifications;
    private NotificationsAdapter adapter;
    private List<Notification> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private BottomNavigationView bottomNavigationView;

    private ImageButton backButton;

    private String currentUserId;

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

    @Override
    protected void onPause() {
        super.onPause();
        // Optionally, refresh the UI when leaving the page if you want to show updated read states.
        // For example:
        // loadNotifications();
    }

}
