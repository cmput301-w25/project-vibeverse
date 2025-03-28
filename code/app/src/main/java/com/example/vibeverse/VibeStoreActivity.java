package com.example.vibeverse;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VibeStoreActivity extends AppCompatActivity {

    private static final String TAG = "VibeStoreActivity";
    private RecyclerView themeRecyclerView;
    private ThemeAdapter themeAdapter;
    // List of all theme strings
    private List<String> themeList = new ArrayList<>();
    // Set to track which themes are locked
    private Set<String> lockedThemesSet = new HashSet<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedTheme = ""; // initially empty

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vibestore);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        themeRecyclerView = findViewById(R.id.themeRecyclerView);
        // Use a single column layout
        themeRecyclerView.setLayoutManager(new GridLayoutManager(this, 1));

        // First load the user's selected theme from the user document.
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Log.e(TAG, "User is not logged in");
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        selectedTheme = documentSnapshot.getString("selectedTheme");
                        if (selectedTheme == null) {
                            selectedTheme = "";
                        }
                    }
                    // Now create the adapter with the selected theme.
                    themeAdapter = new ThemeAdapter(themeList, lockedThemesSet, db, userId, selectedTheme);
                    themeRecyclerView.setAdapter(themeAdapter);

                    // Load the themes from the subcollections
                    loadThemes(userId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    private void loadThemes(String userId) {
        // Load unlocked themes from the user's subcollection
        db.collection("users")
                .document(userId)
                .collection("unlockedThemes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        // Retrieve the array field "themeNames"
                        List<String> themes = (List<String>) document.get("themeNames");
                        if (themes != null && !themes.isEmpty()) {
                            themeList.addAll(themes);
                        }
                    }
                    // Then load locked themes from the user's subcollection
                    db.collection("users")
                            .document(userId)
                            .collection("lockedThemes")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                for (DocumentSnapshot document : queryDocumentSnapshots2.getDocuments()) {
                                    List<String> themes = (List<String>) document.get("themeNames");
                                    if (themes != null && !themes.isEmpty()) {
                                        themeList.addAll(themes);
                                        lockedThemesSet.addAll(themes);
                                    }
                                }
                                themeAdapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Error loading locked themes", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading unlocked themes", e));
    }
}
