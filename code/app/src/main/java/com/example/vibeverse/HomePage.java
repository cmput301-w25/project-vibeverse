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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomePage extends AppCompatActivity implements FilterDialog.FilterListener {

    private RecyclerView recyclerFeed;
    private ImageButton buttonNotification;
    private BottomNavigationView bottomNavigationView;
    private ImageButton buttonFilter;
    private PostAdapter postAdapter;
    private List<Post> allPosts;
    private EditText editSearch;
    private ImageView logoImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize views
        recyclerFeed = findViewById(R.id.recyclerFeed);
        buttonNotification = findViewById(R.id.buttonNotification);
        buttonFilter = findViewById(R.id.buttonFilter);
        editSearch = findViewById(R.id.editSearch);
        logoImage = findViewById(R.id.logoImage);

        // Set up RecyclerView
        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);

        // Initialize sample data
        allPosts = new ArrayList<>();
        allPosts.add(new Post("Welcome to VibeVerse!", "HAPPY", R.drawable.demo_image, new Date()));
        allPosts.add(new Post("First Post", "EXCITED", R.drawable.demo_image,
                new Date(System.currentTimeMillis() - 86400000))); // 1 day old post
        // Add more sample posts as needed

        // Sort posts by date (newest first)
        Collections.sort(allPosts, (post1, post2) -> post2.date.compareTo(post1.date));

        // Set up adapter
        postAdapter = new PostAdapter(new ArrayList<>(allPosts));
        recyclerFeed.setAdapter(postAdapter);

        // Set up notification button click listener
        buttonNotification.setOnClickListener(v -> {
            // Handle notification button click
            // Add your notification logic here
        });

        // Set up filter button click listener (opens FilterDialog)
        buttonFilter.setOnClickListener(v -> FilterDialog.show(HomePage.this, HomePage.this));

        // Set up search functionality
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

        // Bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);
    }

    /**
     * THIS is the updated FilterDialog.FilterListener method
     * with all new mood booleans.
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
     * Apply local (client-side) filtering to the sample posts.
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

            // Time filter
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

            // Mood filter
            // Post.mood is uppercase in your sample data: "HAPPY", "EXCITED", etc.
            // Check if it matches the selected moods.
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

            // If NO moods are selected, show all.
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
     * You must still implement the second method of the FilterDialog.FilterListener
     * interface, even if it's empty.
     */
    @Override
    public void onFilteredResults(List<MoodEvent> results) {
        // Not used in HomePage; can remain empty
    }

    /**
     * Simple Post class to hold sample data.
     */
    private static class Post {
        String title;
        String mood;
        int imageResId;
        Date date;

        Post(String title, String mood, int imageResId, Date date) {
            this.title = title;
            this.mood = mood;
            this.imageResId = imageResId;
            this.date = date;
        }
    }

    /**
     * RecyclerView Adapter to display your sample posts.
     */
    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private final List<Post> postList;
        private final List<Post> originalList;
        private final List<Post> currentList;
        private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);

        PostAdapter(List<Post> postList) {
            this.postList = postList;
            this.originalList = new ArrayList<>(postList);
            this.currentList = new ArrayList<>(postList);
        }

        public void updatePosts(List<Post> newPosts) {
            postList.clear();
            postList.addAll(newPosts);
            notifyDataSetChanged();

            // Update "originalList" and "currentList" if you want
            // further search or filter changes to reflect these updates.
            // (Optional, depends on your logic.)
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

        public void filter(String query) {
            query = query.toLowerCase().trim();
            currentList.clear();

            if (query.isEmpty()) {
                // If empty, restore full list
                currentList.addAll(originalList);
            } else {
                // Otherwise, match the query with the post title
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

        class PostViewHolder extends RecyclerView.ViewHolder {
            ImageView imagePost;
            TextView textTitle, textSubtitle;

            PostViewHolder(@NonNull View itemView) {
                super(itemView);
                imagePost = itemView.findViewById(R.id.imagePost);
                textTitle = itemView.findViewById(R.id.textTitle);
                textSubtitle = itemView.findViewById(R.id.textSubtitle);
            }
        }
    }
}