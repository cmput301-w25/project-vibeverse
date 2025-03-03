package com.example.vibeverse;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProfilePage extends AppCompatActivity implements FilterDialog.FilterListener {

    private RecyclerView recyclerFeed;
    private Button buttonFilter;
    private PostAdapter postAdapter;
    private List<Post> allPosts;
    private EditText editSearch;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_page);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_add) {
                // Launch SelectMoodActivity
                Intent intent = new Intent(ProfilePage.this, SelectMoodActivity.class);
                startActivity(intent);
                return true;
            }
            // Handle other navigation items here
            return false;
        });

        recyclerFeed = findViewById(R.id.recyclerFeed);
        ImageButton buttonFilter = findViewById(R.id.buttonFilter);

        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setHasFixedSize(true);
        editSearch = findViewById(R.id.editSearch);

        // Create some dummy data to display in the feed
        allPosts = new ArrayList<>();
        allPosts.add(new Post("I Bought A New Puppy Yesterday!", "HAPPY", R.drawable.demo_image, new Date(System.currentTimeMillis() - 21600000)));
        allPosts.add(new Post("My Puppy Spoke Today!", "AFRAID", R.drawable.demo_image, new Date(System.currentTimeMillis() - 252000000)));
        allPosts.add(new Post("My Puppy Barked At A Cat Girl Today!", "SAD", R.drawable.demo_image, new Date(System.currentTimeMillis() - 28800000)));
        allPosts.add(new Post("My Puppy Died Today!", "CONFUSED", R.drawable.demo_image, new Date(System.currentTimeMillis() - 500000000)));

        // Sort posts by date (newest first)
        Collections.sort(allPosts, (post1, post2) -> post2.date.compareTo(post1.date));

        postAdapter = new PostAdapter(new ArrayList<>(allPosts));
        recyclerFeed.setAdapter(postAdapter);

        buttonFilter.setOnClickListener(v -> FilterDialog.show(ProfilePage.this, this));

        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                postAdapter.filter(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
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
        public String subtitle;
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

        // Custom filter method
        public void filter(String query) {
            query = query.toLowerCase().trim();
            currentList.clear();
            if (query.isEmpty()) {
                currentList.addAll(originalList);
            } else {
                for (Post post : originalList) {
                    boolean titleMatches = post.title != null && post.title.toLowerCase().contains(query);
                    boolean subtitleMatches = post.subtitle != null && post.subtitle.toLowerCase().contains(query);
                    if (titleMatches || subtitleMatches) {
                        currentList.add(post);
                    }
                }
            }
            // Update postList since it's used for display
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
