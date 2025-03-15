package com.example.vibeverse;

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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UsersProfile extends AppCompatActivity {

    private String userId;
    private FirebaseFirestore db;

    // UI Elements
    private CircleImageView profilePicture;
    private TextView textName, textUsername, textBioContent, textFollowers, textFollowing;
    private Button buttonFollow;
    private ImageButton buttonBack;
    private RecyclerView recyclerUserPosts;
    private ProgressBar progressLoading;
    private View emptyStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.users_profile);

        // Get user ID from intent
        userId = getIntent().getStringExtra("userId");
        if (userId == null) {
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
        buttonFollow.setOnClickListener(v -> {
            // You'll implement the follow logic later
            Toast.makeText(UsersProfile.this, "Follow feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void initViews() {
        profilePicture = findViewById(R.id.profilePicture);
        textName = findViewById(R.id.textName);
        textUsername = findViewById(R.id.textUsername);
        textBioContent = findViewById(R.id.textBioContent);
        textFollowers = findViewById(R.id.textFollowers);
        textFollowing = findViewById(R.id.textFollowing);
        buttonFollow = findViewById(R.id.buttonFollow);
        buttonBack = findViewById(R.id.buttonBack);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        progressLoading = findViewById(R.id.progressLoading);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Set up RecyclerView
        recyclerUserPosts.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns for grid display
    }

    private void loadUserData() {
        showLoading(true);

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    showLoading(false);
                    if (documentSnapshot.exists()) {
                        updateUI(documentSnapshot);
                    } else {
                        Toast.makeText(UsersProfile.this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
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
                textBioContent.setText("No bio available");
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

            // In a real app, you would load followers and following counts from the database
            // This is just placeholder code
            textFollowers.setText("0");
            textFollowing.setText("0");
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
}