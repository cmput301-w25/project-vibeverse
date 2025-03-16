package com.example.vibeverse;

import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private String userId;
    private FirebaseFirestore db;

    // UI Elements
    private CircleImageView profilePicture;
    private TextView textName, textUsername, textBioContent, textFollowers, textFollowing, textTopUsername;
    private Button buttonFollow;
    private ImageButton buttonBack;
    private RecyclerView recyclerUserPosts;
    private ProgressBar progressLoading;
    private View emptyStateView;

    private MoodEventAdapter moodEventAdapter;
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

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

        // Initialize adapter
        moodEventAdapter = new MoodEventAdapter(this, new ArrayList<>());
        recyclerUserPosts.setAdapter(moodEventAdapter);

        // Load user data
        loadUserData();

        // Load user posts
        loadUserPosts();

        // Set up back button
        buttonBack.setOnClickListener(v -> finish());

        // Follow button placeholder
        buttonFollow.setOnClickListener(v ->
                Toast.makeText(this, "Follow feature coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void initViews() {
        profilePicture = findViewById(R.id.profilePicture);
        textName = findViewById(R.id.textName);
        textUsername = findViewById(R.id.textUsername);
        textTopUsername = findViewById(R.id.textTopUsername);
        textBioContent = findViewById(R.id.textBioContent);
        textFollowers = findViewById(R.id.textFollowers);
        textFollowing = findViewById(R.id.textFollowing);
        buttonFollow = findViewById(R.id.buttonFollow);
        buttonBack = findViewById(R.id.buttonBack);
        recyclerUserPosts = findViewById(R.id.recyclerUserPosts);
        progressLoading = findViewById(R.id.progressLoading);
        emptyStateView = findViewById(R.id.emptyStateView);

        recyclerUserPosts.setLayoutManager(new LinearLayoutManager(this));
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
                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
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
            textUsername.setText("@" + user.getUsername());
            textTopUsername.setText(user.getUsername());

            if (user.getBio() != null && !user.getBio().isEmpty()) {
                textBioContent.setText(user.getBio());
            } else {
                textBioContent.setText("No bio available");
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

            // Placeholder counts
            textFollowers.setText("0");
            textFollowing.setText("0");
        }
    }

    private void loadUserPosts() {
        showLoading(true);
        db.collection("Usermoods")
                .document(userId)
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
                            if (moodEvent.getTrigger() != null && !moodEvent.getTrigger().isEmpty()) {
                                subtitle.append("Trigger: ").append(moodEvent.getTrigger());
                            }
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
}