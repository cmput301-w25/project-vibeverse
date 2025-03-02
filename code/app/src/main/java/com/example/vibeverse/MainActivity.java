package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * MainActivity is the primary activity that displays the history of mood events.
 * <p>
 * It uses a custom adapter (MoodAdapter) to display mood entries in a ListView.
 * Users can add new moods via the Add Mood button, edit existing moods by tapping on them,
 * and delete moods by long-pressing on an entry.
 * </p>
 */
public class MainActivity extends AppCompatActivity implements MoodAdapter.OnMoodActionListener {

    private ListView moodListView;
    private Button addMoodButton;
    private MoodAdapter adapter;
    private static final List<String> moodDisplayList = new ArrayList<>();
    // Add a list to store MoodEvent objects
    private static final List<MoodEvent> moodEventList = new ArrayList<>();

    private static final int REQUEST_ADD_MOOD = 1;
    private static final int REQUEST_EDIT_MOOD = 2;

    /**
     * Called when the activity is first created.
     * <p>
     * Initializes the ListView and Add Mood button, sets up the custom adapter,
     * and checks for any mood event passed via the Intent. If a MoodEvent is found,
     * it converts it into a display string and adds it to the list.
     * </p>
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moodListView = findViewById(R.id.moodListView);
        addMoodButton = findViewById(R.id.addMoodButton);

        addMoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectMoodActivity.class);
            startActivityForResult(intent, REQUEST_ADD_MOOD);
        });

        // Use the custom adapter
        adapter = new MoodAdapter(this, moodDisplayList, this);
        moodListView.setAdapter(adapter);

        // Check if a MoodEvent was passed from SelectMoodActivity
        if (getIntent().hasExtra("moodEvent")) {
            MoodEvent moodEvent = (MoodEvent) getIntent().getSerializableExtra("moodEvent");
            if (moodEvent != null) {
                // Add the mood event to our list
                moodEventList.add(moodEvent);

                // Create display string
                String moodDisplay = createMoodDisplayString(moodEvent);
                moodDisplayList.add(moodDisplay);
                adapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * Creates a display string for a mood event
     *
     * @param moodEvent The mood event to generate a display string for
     * @return A formatted string representing the mood event
     */
    private String createMoodDisplayString(MoodEvent moodEvent) {
        // Retrieve the photograph URI if available
        String photoUri = moodEvent.getPhotoUri();

        // Get intensity label based on value
        int intensity = moodEvent.getIntensity();
        String intensityLabel = "";
        if (intensity <= 3) {
            intensityLabel = "Slightly ";
        } else if (intensity >= 8) {
            intensityLabel = "Very ";
        }

        return moodEvent.getEmoji() + " " + intensityLabel + moodEvent.getMood() + "\n" +
                moodEvent.getTimestamp() + "\n" +
                "Trigger: " + (moodEvent.getTrigger().isEmpty() ? "N/A" : moodEvent.getTrigger()) + "\n" +
                "Social Situation: " + (moodEvent.getSocialSituation().isEmpty() ? "N/A" : moodEvent.getSocialSituation()) + "\n" +
                "Photo: " + photoUri + "\n" +
                "Intensity: " + intensity;
    }

    /**
     * Handles results from activities started with startActivityForResult.
     * <p>
     * If a new mood is added or an existing mood is edited, the corresponding method
     * (addMoodToList or updateMoodInList) is called to update the ListView.
     * </p>
     *
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode  The integer result code returned by the child activity.
     * @param data        An Intent that carries the result data.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_ADD_MOOD) {
                if (data.hasExtra("moodEvent")) {
                    // Handle the MoodEvent object directly
                    MoodEvent moodEvent = (MoodEvent) data.getSerializableExtra("moodEvent");
                    if (moodEvent != null) {
                        moodEventList.add(moodEvent);
                        moodDisplayList.add(createMoodDisplayString(moodEvent));
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    // Fall back to the old way for backward compatibility
                    addMoodToList(data);
                }
            } else if (requestCode == REQUEST_EDIT_MOOD) {
                updateMoodInList(data);
            }
        }
    }

    /**
     * Adds a new mood entry to the moodDisplayList.
     *
     * @param data The Intent containing mood details from SelectMoodActivity.
     */
    private void addMoodToList(Intent data) {
        String selectedMood = data.getStringExtra("selectedMood");
        String selectedEmoji = data.getStringExtra("selectedEmoji");
        String timestamp = data.getStringExtra("timestamp");
        String trigger = data.getStringExtra("trigger");
        String socialSituation = data.getStringExtra("socialSituation");
        String photoUri = data.getStringExtra("photoUri");  // Photo URI
        int intensity = data.getIntExtra("intensity", 5);   // Intensity with default 5

        // Create a MoodEvent object to store
        MoodEvent moodEvent = new MoodEvent(selectedEmoji + " " + selectedMood, trigger, socialSituation);
        moodEvent.setTimestamp(timestamp);
        moodEvent.setIntensity(intensity);
        if (photoUri != null && !photoUri.equals("N/A")) {
            // Create photograph from URI if available
            Photograph photo = new Photograph(android.net.Uri.parse(photoUri), 0, new java.util.Date(), "");
            moodEvent.setPhotograph(photo);
        }

        // Add to both lists
        moodEventList.add(moodEvent);

        // Get intensity label
        String intensityLabel = "";
        if (intensity <= 3) {
            intensityLabel = "Slightly ";
        } else if (intensity >= 8) {
            intensityLabel = "Very ";
        }

        String moodEntry = selectedEmoji + " " + intensityLabel + selectedMood + "\n"
                + timestamp + "\n"
                + "Trigger: " + (trigger.isEmpty() ? "N/A" : trigger) + "\n"
                + "Social Situation: " + (socialSituation.isEmpty() ? "N/A" : socialSituation) + "\n"
                + "Photo: " + (photoUri == null || photoUri.isEmpty() ? "N/A" : photoUri) + "\n"
                + "Intensity: " + intensity;

        moodDisplayList.add(moodEntry);
        adapter.notifyDataSetChanged();
    }

    /**
     * Updates an existing mood entry in the moodDisplayList.
     *
     * @param data The Intent containing updated mood details from EditMoodActivity.
     */
    private void updateMoodInList(Intent data) {
        int moodPosition = data.getIntExtra("moodPosition", -1);
        if (moodPosition != -1) {
            String updatedMood = data.getStringExtra("updatedMood");
            String updatedEmoji = data.getStringExtra("updatedEmoji");
            String updatedTrigger = data.getStringExtra("updatedTrigger");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            String timestamp = data.getStringExtra("timestamp");
            String updatedPhotoUri = data.getStringExtra("updatedPhotoUri");  // Updated photo URI
            int updatedIntensity = data.getIntExtra("updatedIntensity", 5);   // Updated intensity

            // Update the MoodEvent object if available
            if (moodPosition < moodEventList.size()) {
                MoodEvent moodEvent = moodEventList.get(moodPosition);
                moodEvent.setEmotionalState(updatedEmoji + " " + updatedMood);
                moodEvent.setTrigger(updatedTrigger);
                moodEvent.setSocialSituation(updatedSocialSituation);
                moodEvent.setTimestamp(timestamp);
                moodEvent.setIntensity(updatedIntensity);

                if (updatedPhotoUri != null && !updatedPhotoUri.equals("N/A")) {
                    // Update photograph if available
                    Photograph photo = new Photograph(android.net.Uri.parse(updatedPhotoUri), 0, new java.util.Date(), "");
                    moodEvent.setPhotograph(photo);
                } else {
                    moodEvent.setPhotograph(null);
                }
            }

            // Get intensity label
            String intensityLabel = "";
            if (updatedIntensity <= 3) {
                intensityLabel = "Slightly ";
            } else if (updatedIntensity >= 8) {
                intensityLabel = "Very ";
            }

            String updatedMoodEntry = updatedEmoji + " " + intensityLabel + updatedMood + "\n"
                    + timestamp + "\n"
                    + "Trigger: " + (updatedTrigger.isEmpty() ? "N/A" : updatedTrigger) + "\n"
                    + "Social Situation: " + (updatedSocialSituation.isEmpty() ? "N/A" : updatedSocialSituation) + "\n"
                    + "Photo: " + (updatedPhotoUri == null || updatedPhotoUri.isEmpty() ? "N/A" : updatedPhotoUri) + "\n"
                    + "Intensity: " + updatedIntensity;

            moodDisplayList.set(moodPosition, updatedMoodEntry);
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Callback for the delete action from the MoodAdapter.
     * Removes the mood entry at the given position from the list and updates the ListView.
     *
     * @param position The index of the mood entry to be deleted.
     */
    @Override
    public void onMoodDelete(int position) {
        moodDisplayList.remove(position);
        if (position < moodEventList.size()) {
            moodEventList.remove(position);
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Mood deleted", Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback for the edit action from the MoodAdapter.
     * Extracts mood details from the selected entry and starts EditMoodActivity for result.
     *
     * @param position The index of the mood entry to be edited.
     */
    @Override
    public void onMoodEdit(int position) {
        // Check if we have a MoodEvent object for this position
        if (position < moodEventList.size()) {
            MoodEvent moodEvent = moodEventList.get(position);

            // Open EditMoodActivity and pass the complete mood details
            Intent editIntent = new Intent(MainActivity.this, EditMoodActivity.class);
            editIntent.putExtra("selectedMood", moodEvent.getMood());
            editIntent.putExtra("selectedEmoji", moodEvent.getEmoji());
            editIntent.putExtra("timestamp", moodEvent.getTimestamp());
            editIntent.putExtra("trigger", moodEvent.getTrigger());
            editIntent.putExtra("socialSituation", moodEvent.getSocialSituation());
            editIntent.putExtra("photoUri", moodEvent.getPhotoUri());
            editIntent.putExtra("moodPosition", position);
            editIntent.putExtra("intensity", moodEvent.getIntensity());
            startActivityForResult(editIntent, REQUEST_EDIT_MOOD);
            return;
        }

        // Fallback method parsing from the display string
        String moodEntry = moodDisplayList.get(position);
        String[] parts = moodEntry.split("\n");

        if (parts.length < 2) return;

        String[] moodDetails = parts[0].split(" ", 2); // Split emoji and the rest
        Log.d("EditMoodActivity", "Mood details length: " + moodDetails.length + ", contents: " + java.util.Arrays.toString(moodDetails));

        String emoji = moodDetails[0];
        String mood = moodDetails[1];

        // Check if mood starts with "Slightly " or "Very "
        if (mood.startsWith("Slightly ")) {
            mood = mood.substring("Slightly ".length());
        } else if (mood.startsWith("Very ")) {
            mood = mood.substring("Very ".length());
        }

        String timestamp = parts[1].trim();
        String trigger = parts.length > 2 ? parts[2].replace("Trigger: ", "").trim() : "N/A";
        String socialSituation = parts.length > 3 ? parts[3].replace("Social Situation: ", "").trim() : "N/A";
        String photoUri = parts.length > 4 ? parts[4].replace("Photo: ", "").trim() : "N/A";

        // Extract intensity if available
        int intensity = 5; // Default
        if (parts.length > 5 && parts[5].startsWith("Intensity: ")) {
            try {
                intensity = Integer.parseInt(parts[5].replace("Intensity: ", "").trim());
            } catch (NumberFormatException e) {
                Log.e("MainActivity", "Error parsing intensity: " + e.getMessage());
            }
        }

        // Open EditMoodActivity and pass all mood details
        Intent editIntent = new Intent(MainActivity.this, EditMoodActivity.class);
        editIntent.putExtra("selectedMood", mood);
        editIntent.putExtra("selectedEmoji", emoji);
        editIntent.putExtra("timestamp", timestamp);
        editIntent.putExtra("trigger", trigger);
        editIntent.putExtra("socialSituation", socialSituation);
        editIntent.putExtra("moodPosition", position);
        editIntent.putExtra("photoUri", photoUri);
        editIntent.putExtra("intensity", intensity);
        startActivityForResult(editIntent, REQUEST_EDIT_MOOD);
    }
}