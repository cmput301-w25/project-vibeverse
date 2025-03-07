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

import com.example.vibeverse.FilterDialog;
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
        allPosts.add(new Post("First Post", "EXCITED", R.drawable.demo_image, new Date(System.currentTimeMillis() - 86400000)));
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

        // Set up filter button click listener
        buttonFilter.setOnClickListener(v -> FilterDialog.show(HomePage.this, this));

        // Set up search functionality
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                postAdapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                // Launch SelectMoodActivity
                Intent intent = new Intent(HomePage.this, SelectMoodActivity.class);
                startActivity(intent);
                return true;
            }
            // Handle other navigation items here
            return false;
        });
    }

    @Override
    public void onFilterApplied(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused) {
        applyFilters(timeFilter, isHappy, isSad, isAfraid, isConfused);
    }

    private void applyFilters(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused) {
        List<Post> filteredPosts = new ArrayList<>();
        long currentTime = System.currentTimeMillis();

        for (Post post : allPosts) {
            long postTime = post.date.getTime();
            boolean isWithinTime = false;

            switch (timeFilter) {
                case "last_24_hours":
                    isWithinTime = (currentTime - postTime) <= 86400000;
                    break;
                case "3Days":
                    isWithinTime = (currentTime - postTime) <= 259200000;
                    break;
                case "last_week":
                    isWithinTime = (currentTime - postTime) <= 604800000;
                    break;
                case "last_month":
                    isWithinTime = (currentTime - postTime) <= 2592000000L;
                    break;
                case "all_time":
                    isWithinTime = true;
                    break;
            }

            if (isWithinTime) {
                if ((isHappy && post.mood.equals("HAPPY")) ||
                        (isSad && post.mood.equals("SAD")) ||
                        (isAfraid && post.mood.equals("AFRAID")) ||
                        (isConfused && post.mood.equals("CONFUSED")) ||
                        (!isHappy && !isSad && !isAfraid && !isConfused)) {
                    filteredPosts.add(post);
                }
            }
        }
        postAdapter.updatePosts(filteredPosts);
    }

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

    private class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {
        private final List<Post> postList;
        private List<Post> originalList;
        private List<Post> currentList;
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
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
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
                currentList.addAll(originalList);
            } else {
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