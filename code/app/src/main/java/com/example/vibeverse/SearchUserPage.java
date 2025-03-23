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

    private EditText editSearch;
    private RecyclerView recyclerSearchResults;
    private UserAdapter userAdapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private BottomNavigationView bottomNavigationView;

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
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

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

    @Override
    public void onUserClick(User user) {
        // Navigate to user profile when a user is clicked
        Intent intent = new Intent(SearchUserPage.this, UsersProfile.class);
        intent.putExtra("userId", user.getUserId());
        startActivity(intent);
    }
}