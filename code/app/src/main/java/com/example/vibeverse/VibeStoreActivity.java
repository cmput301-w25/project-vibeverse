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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * VibeStoreActivity displays the available themes in the Vibe Store.
 * <p>
 * It loads the themes from assets, retrieves the user's selected theme and locked themes from Firestore,
 * and sets up the ThemeAdapter to display the themes.
 * </p>
 */
public class VibeStoreActivity extends AppCompatActivity {

    private static final String TAG = "VibeStoreActivity";
    private RecyclerView themeRecyclerView;
    private ThemeAdapter themeAdapter;
    // List of all theme strings
    private List<ThemeData> themeList;
    private Set<String> lockedThemesSet = new HashSet<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String selectedTheme = ""; // initially empty

    /**
     * Called when the activity is starting.
     * <p>
     * Initializes Firestore, FirebaseAuth, the RecyclerView, loads the user's selected theme,
     * themes from assets, and locked themes from Firestore.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
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
                    themeList = loadThemesFromAssets();
                    // Now create the adapter with the selected theme.
                    themeAdapter = new ThemeAdapter(themeList, lockedThemesSet, db, userId, selectedTheme);
                    themeRecyclerView.setAdapter(themeAdapter);

                    loadLockedThemes(userId);
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading user data", e));
    }

    /**
     * Loads the list of themes from the assets folder by parsing the themes.json file.
     *
     * @return A list of ThemeData objects.
     */
    private List<ThemeData> loadThemesFromAssets() {
        try {
            InputStream inputStream = getAssets().open("themes.json");
            InputStreamReader reader = new InputStreamReader(inputStream);
            Type listType = new TypeToken<List<ThemeData>>() {}.getType();
            List<ThemeData> themes = new Gson().fromJson(reader, listType);
            reader.close();
            return themes;
        } catch (Exception e) {
            Log.e(TAG, "Error loading themes from assets", e);
            return new ArrayList<>();
        }
    }

    /**
     * Loads the locked themes from Firestore for the given user ID.
     * <p>
     * It retrieves the locked themes from the "lockedThemes" subcollection and updates the adapter.
     * </p>
     *
     * @param userId The ID of the current user.
     */
    private void loadLockedThemes(String userId) {
        // For example, load the names/IDs of locked themes and add them to lockedThemesSet.
        db.collection("users")
                .document(userId)
                .collection("lockedThemes")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        List<String> themes = (List<String>) document.get("themeNames");
                        if (themes != null && !themes.isEmpty()) {
                            lockedThemesSet.addAll(themes);
                        }
                    }
                    themeAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading locked themes", e));
    }
}
