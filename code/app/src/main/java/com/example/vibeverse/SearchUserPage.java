package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SearchUserPage extends AppCompatActivity implements UserAdapter.OnUserClickListener {

    /** EditText for entering search queries. */
    private EditText editSearch;
    /** RecyclerView for displaying search results. */
    private RecyclerView recyclerSearchResults;
    /** Adapter for managing user search results. */
    private UserAdapter userAdapter;
    /** List of User objects representing search results. */
    private List<User> userList;
    /** Firebase Firestore instance for database operations. */
    private FirebaseFirestore db;
    /** BottomNavigationView for navigating the app. */
    private BottomNavigationView bottomNavigationView;

    /**
     * Called when the activity is first created. Initializes views, adapter, and search listener.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_user);

        // Initialize views
        editSearch = findViewById(R.id.editSearch);
        recyclerSearchResults = findViewById(R.id.recyclerSearchResults);
        recyclerSearchResults.setLayoutManager(new LinearLayoutManager(this));

        // Initialize user list and adapter
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, this);
        recyclerSearchResults.setAdapter(userAdapter);

        // Set up bottom navigation.
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        // Get Firestore instance
        db = FirebaseFirestore.getInstance();

        // Add text change listener to search box
        editSearch.addTextChangedListener(new TextWatcher() {
            /**
             * This method is called before the text is changed.
             *
             * @param s The text before change.
             * @param start The start position.
             * @param count The count of characters before change.
             * @param after The count of characters that will be changed.
             */
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            /**
             * Called when the text is being changed. Updates the search results based on the query.
             *
             * @param s The text after change.
             * @param start The start position.
             * @param before The count of characters before change.
             * @param count The count of characters after change.
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    userList.clear();
                    userAdapter.notifyDataSetChanged();
                } else {
                    searchUsers(query);
                }
            }

            /**
             * This method is called after the text has been changed.
             *
             * @param s The final text.
             */
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * Searches for users in the Firestore database whose usernames start with the provided query.
     *
     * @param query The search query entered by the user.
     */
    private void searchUsers(String query) {
        // Search for usernames that start with the query (case-insensitive would be better, but Firestore has limitations)
        String lowerCaseQuery = query.toLowerCase();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
        db.collection("users")
                .orderBy("usernameLowercase") // Order by a lowercase version of the username
                .startAt(lowerCaseQuery)
                .endAt(lowerCaseQuery + "\uf8ff") // This is a high code point that comes after all regular characters
                .limit(20) // Limit results for better performance
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        userList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            User user = document.toObject(User.class);
                            user.setUserId(document.getId()); // Set the document ID as userId
                            // Exclude the logged-in user from search results
                            if (!user.getUserId().equals(currentUserId)) {
                                userList.add(user);
                            }
                        }
                        userAdapter.notifyDataSetChanged();
                    }
                });
    }

    /**
     * Called when a user in the search results is clicked. Navigates to the clicked user's profile.
     *
     * @param user The User object representing the clicked user.
     */
    @Override
    public void onUserClick(User user) {
        // Navigate to user profile when a user is clicked
        Intent intent = new Intent(SearchUserPage.this, UsersProfile.class);
        intent.putExtra("userId", user.getUserId());
        startActivity(intent);
    }
}