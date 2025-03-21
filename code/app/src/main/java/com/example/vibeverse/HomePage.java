package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * HomePage is an Activity that displays a simple feed of posts.
 * <p>
 * It shows sample posts in a RecyclerView and provides client-side filtering
 * based on time range and mood selection through the {@link FilterDialog}.
 * The activity also includes a search box and bottom navigation.
 * </p>
 */
public class HomePage extends AppCompatActivity implements FilterDialog.FilterListener {

    /** RecyclerView displaying the feed of posts. */
    private RecyclerView recyclerFeed;
    /** Button to access notifications (logic to be implemented). */
    private ImageButton buttonNotification;
    /** BottomNavigationView for app-wide navigation. */
    private BottomNavigationView bottomNavigationView;
    /** Button to open the FilterDialog for filtering posts. */
    private ImageButton buttonFilter;
    /** Adapter for displaying posts in the RecyclerView. */
    private PostAdapter postAdapter;
    /** List of all posts (sample data). */
    private List<Post> allPosts;
    /** EditText for searching posts by title. */
    private EditText editSearch;
    /** Logo image (optional decoration). */
    private ImageView logoImage;

    private TextView notificationBadge;


    /**
     * Called when the Activity is created.
     * <p>
     * Initializes UI components, loads sample post data, sets up RecyclerView
     * and bottom navigation, and configures search and filter functionality.
     * </p>
     *
     * @param savedInstanceState Bundle containing saved state data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize views from layout
        recyclerFeed = findViewById(R.id.recyclerFeed);
        buttonNotification = findViewById(R.id.buttonNotification);
        buttonFilter = findViewById(R.id.buttonFilter);
        editSearch = findViewById(R.id.editSearch);
        logoImage = findViewById(R.id.logoImage);

        // Set up RecyclerView with a linear layout manager
        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);

        // Initialize sample post data
        allPosts = new ArrayList<>();
        allPosts.add(new Post("Welcome to VibeVerse!", "HAPPY", R.drawable.demo_image, new Date()));
        allPosts.add(new Post("First Post", "EXCITED", R.drawable.demo_image,
                new Date(System.currentTimeMillis() - 86400000))); // 1 day old post
        // Add more sample posts as needed

        // Sort posts by date (newest first)
        Collections.sort(allPosts, (post1, post2) -> post2.date.compareTo(post1.date));

        // Initialize adapter with a copy of the sample data
        postAdapter = new PostAdapter(new ArrayList<>(allPosts));
        recyclerFeed.setAdapter(postAdapter);

        // Set up notification button click listener (stub)
        buttonNotification.setOnClickListener(v -> {
            Intent intent = new Intent(HomePage.this, NotificationsActivity.class);
            startActivity(intent);
        });

        // Set up filter button to open the FilterDialog
//        buttonFilter.setOnClickListener(v -> FilterDialog.show(HomePage.this, HomePage.this));

        // Set up search functionality to filter posts as user types
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                postAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Set up bottom navigation using the NavigationHelper utility class
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        // Get the badge TextView
        notificationBadge = findViewById(R.id.notificationBadge);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long newNotificationCount = documentSnapshot.getLong("newNotificationCount");
                        if (newNotificationCount != null && newNotificationCount > 0) {
                            notificationBadge.setText(String.valueOf(newNotificationCount));
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // Optionally handle the error (e.g., log or show a Toast)
                    notificationBadge.setVisibility(View.GONE);
                });
    }

    /**
     * Callback method from FilterDialog.FilterListener.
     * <p>
     * This method is called when the user applies filters in the FilterDialog.
     * It invokes a client-side filter to update the displayed posts.
     * </p>
     *
     * @param timeFilter   The time filter string (e.g., "last_24_hours").
     * @param isHappy      True if "Happy" is selected.
     * @param isSad        True if "Sad" is selected.
     * @param isAngry      True if "Angry" is selected.
     * @param isSurprised  True if "Surprised" is selected.
     * @param isAfraid     True if "Afraid" is selected.
     * @param isDisgusted  True if "Disgusted" is selected.
     * @param isConfused   True if "Confused" is selected.
     * @param isShameful   True if "Shameful" is selected.
     */
    @Override
    public void onFilterApplied(String timeFilter,
                                boolean isHappy,
                                boolean isSad,
                                boolean isAngry,
                                boolean isSurprised,
                                boolean isAfraid,
                                boolean isDisgusted,
                                boolean isConfused,
                                boolean isShameful) {
        applyFilters(timeFilter,
                isHappy, isSad, isAngry, isSurprised,
                isAfraid, isDisgusted, isConfused, isShameful);
    }

    /**
     * Applies local (client-side) filtering to the sample posts.
     * <p>
     * Filters posts based on the provided time filter and mood selections.
     * Posts that do not fall within the specified time range or do not match the
     * selected moods are excluded from the displayed list.
     * </p>
     *
     * @param timeFilter   The time filter string.
     * @param isHappy      True if "Happy" is selected.
     * @param isSad        True if "Sad" is selected.
     * @param isAngry      True if "Angry" is selected.
     * @param isSurprised  True if "Surprised" is selected.
     * @param isAfraid     True if "Afraid" is selected.
     * @param isDisgusted  True if "Disgusted" is selected.
     * @param isConfused   True if "Confused" is selected.
     * @param isShameful   True if "Shameful" is selected.
     */
    private void applyFilters(String timeFilter,
                              boolean isHappy,
                              boolean isSad,
                              boolean isAngry,
                              boolean isSurprised,
                              boolean isAfraid,
                              boolean isDisgusted,
                              boolean isConfused,
                              boolean isShameful) {

        List<Post> filteredPosts = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Post post : allPosts) {
            long postTime = post.date.getTime();
            boolean isWithinTime = false;

            // Apply time filter based on the provided filter string
            switch (timeFilter) {
                case "last_24_hours":
                    isWithinTime = (currentTime - postTime) <= 86400000L;
                    break;
                case "3Days":
                    isWithinTime = (currentTime - postTime) <= 259200000L;
                    break;
                case "last_week":
                    isWithinTime = (currentTime - postTime) <= 604800000L;
                    break;
                case "last_month":
                    isWithinTime = (currentTime - postTime) <= 2592000000L;
                    break;
                case "all_time":
                default:
                    isWithinTime = true;
            }

            if (!isWithinTime) {
                continue;
            }

            // Apply mood filter: check if the post's mood (in uppercase) matches any selected mood
            boolean moodMatch = false;
            String postMood = post.mood.toUpperCase(Locale.ROOT);

            if (isHappy     && "HAPPY".equals(postMood))      moodMatch = true;
            if (isSad       && "SAD".equals(postMood))        moodMatch = true;
            if (isAngry     && "ANGRY".equals(postMood))      moodMatch = true;
            if (isSurprised && "SURPRISED".equals(postMood))  moodMatch = true;
            if (isAfraid    && "AFRAID".equals(postMood))     moodMatch = true;
            if (isDisgusted && "DISGUSTED".equals(postMood))  moodMatch = true;
            if (isConfused  && "CONFUSED".equals(postMood))   moodMatch = true;
            if (isShameful  && "SHAMEFUL".equals(postMood))   moodMatch = true;

            // If no mood checkboxes are selected, then show all posts
            if (!isHappy && !isSad && !isAngry && !isSurprised &&
                    !isAfraid && !isDisgusted && !isConfused && !isShameful) {
                moodMatch = true;
            }

            if (moodMatch) {
                filteredPosts.add(post);
            }
        }

        postAdapter.updatePosts(filteredPosts);
    }

    /**
     * Callback method from FilterDialog.FilterListener.
     * <p>
     * This method is required by the FilterListener interface but is not used in HomePage.
     * </p>
     *
     * @param results The list of filtered MoodEvent objects.
     */
    @Override
    public void onFilteredResults(List<MoodEvent> results) {
        // Not used in HomePage; can remain empty
    }

    /**
     * Simple Post class representing a sample post.
     */
    private static class Post {
        /** Title of the post. */
        String title;
        /** Mood associated with the post (e.g., "HAPPY", "EXCITED"). */
        String mood;
        /** Resource ID for an image associated with the post. */
        int imageResId;
        /** Date the post was created. */
        Date date;

        /**
         * Constructs a new Post.
         *
         * @param title     The post title.
         * @param mood      The mood associated with the post.
         * @param imageResId The image resource ID.
         * @param date      The creation date.
         */
        Post(String title, String mood, int imageResId, Date date) {
            this.title = title;
            this.mood = mood;
            this.imageResId = imageResId;
            this.date = date;
        }
    }

    /**
     * RecyclerView Adapter for displaying Post objects.
     */
    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        /** List of posts currently displayed. */
        private final List<Post> postList;
        /** Original full list of posts. */
        private final List<Post> originalList;
        /** List used for filtering results. */
        private final List<Post> currentList;
        /** Formatter for displaying the post date and time. */
        private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);

        /**
         * Constructs a new PostAdapter.
         *
         * @param postList The initial list of posts.
         */
        PostAdapter(List<Post> postList) {
            this.postList = postList;
            this.originalList = new ArrayList<>(postList);
            this.currentList = new ArrayList<>(postList);
        }

        /**
         * Updates the adapter with a new list of posts.
         *
         * @param newPosts The new list of posts.
         */
        public void updatePosts(List<Post> newPosts) {
            postList.clear();
            postList.addAll(newPosts);
            notifyDataSetChanged();

            // Optionally update the original and current lists as well.
            originalList.clear();
            originalList.addAll(newPosts);
            currentList.clear();
            currentList.addAll(newPosts);
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            Post post = postList.get(position);
            holder.textTitle.setText(post.title);
            holder.textSubtitle.setText(formatter.format(post.date) + " • " + post.mood);
            holder.imagePost.setImageResource(post.imageResId);
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }

        /**
         * Filters the list of posts based on a query string.
         * <p>
         * The query is compared against the post titles.
         * </p>
         *
         * @param query The search query.
         */
        public void filter(String query) {
            query = query.toLowerCase().trim();
            currentList.clear();

            if (query.isEmpty()) {
                // If query is empty, restore the full list.
                currentList.addAll(originalList);
            } else {
                // Otherwise, filter posts by matching the query with the title.
                for (Post post : originalList) {
                    if (post.title.toLowerCase().contains(query)) {
                        currentList.add(post);
                    }
                }
            }

            postList.clear();
            postList.addAll(currentList);
            notifyDataSetChanged();
        }

        /**
         * ViewHolder class for a Post item.
         */
        class PostViewHolder extends RecyclerView.ViewHolder {
            /** ImageView for the post image. */
            ImageView imagePost;
            /** TextView for the post title. */
            TextView textTitle;
            /** TextView for the post subtitle. */
            TextView textSubtitle;

            /**
             * Constructs a new PostViewHolder.
             *
             * @param itemView The view representing a single post.
             */
            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imagePost = itemView.findViewById(R.id.imagePost);
                textTitle = itemView.findViewById(R.id.textTitle);
                textSubtitle = itemView.findViewById(R.id.textSubtitle);
            }
        }
    }
}