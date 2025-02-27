package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MoodAdapter.OnMoodActionListener {

    private ListView moodListView;
    private Button addMoodButton;
    private Button profileButton; // New button for profile page
    private MoodAdapter adapter;
    private static final List<String> moodDisplayList = new ArrayList<>();
    private static final int REQUEST_EDIT_MOOD = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        moodListView = findViewById(R.id.moodListView);
        addMoodButton = findViewById(R.id.addMoodButton);
        profileButton = findViewById(R.id.profileButton); // Initialize profile button

        // Open SelectMoodActivity when clicking "Add Mood"
        addMoodButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SelectMoodActivity.class);
            startActivityForResult(intent, 1);
        });

        // Open ProfileActivity when clicking "Go to Profile"
        profileButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfilePage.class);
            startActivity(intent);
        });

        // Use the custom adapter
        adapter = new MoodAdapter(this, moodDisplayList, this);
        moodListView.setAdapter(adapter);

        // Check if a mood was passed from SelectMoodActivity
        Intent intent = getIntent();
        if (intent.hasExtra("selectedMood")) {
            addMoodToList(intent);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                addMoodToList(data);
            } else if (requestCode == REQUEST_EDIT_MOOD) {
                updateMoodInList(data);
            }
        }
    }

    private void addMoodToList(Intent data) {
        String selectedMood = data.getStringExtra("selectedMood");
        String selectedEmoji = data.getStringExtra("selectedEmoji");
        String timestamp = data.getStringExtra("timestamp");
        String trigger = data.getStringExtra("trigger");
        String socialSituation = data.getStringExtra("socialSituation");

        String moodEntry = selectedEmoji + " " + selectedMood + "\n"
                + timestamp + "\n"
                + "Trigger: " + (trigger.isEmpty() ? "N/A" : trigger) + "\n"
                + "Social Situation: " + (socialSituation.isEmpty() ? "N/A" : socialSituation);

        moodDisplayList.add(moodEntry);
        adapter.notifyDataSetChanged();
    }

    private void updateMoodInList(Intent data) {
        int moodPosition = data.getIntExtra("moodPosition", -1);
        if (moodPosition != -1) {
            String updatedMood = data.getStringExtra("updatedMood");
            String updatedEmoji = data.getStringExtra("updatedEmoji");
            String updatedTrigger = data.getStringExtra("updatedTrigger");
            String updatedSocialSituation = data.getStringExtra("updatedSocialSituation");
            String timestamp = data.getStringExtra("timestamp");

            String updatedMoodEntry = updatedEmoji + " " + updatedMood + "\n"
                    + timestamp + "\n"
                    + "Trigger: " + (updatedTrigger.isEmpty() ? "N/A" : updatedTrigger) + "\n"
                    + "Social Situation: " + (updatedSocialSituation.isEmpty() ? "N/A" : updatedSocialSituation);

            moodDisplayList.set(moodPosition, updatedMoodEntry);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onMoodDelete(int position) {
        moodDisplayList.remove(position);
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Mood deleted", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onMoodEdit(int position) {
        // Extract details to send to EditMoodActivity
        String moodEntry = moodDisplayList.get(position);
        String[] parts = moodEntry.split("\n");

        if (parts.length < 2) return;

        String[] moodDetails = parts[0].split(" ", 2);
        String emoji = moodDetails[0];
        String mood = moodDetails[1];

        String timestamp = parts[1].trim();
        String trigger = parts.length > 2 ? parts[2].replace("Trigger: ", "").trim() : "N/A";
        String socialSituation = parts.length > 3 ? parts[3].replace("Social Situation: ", "").trim() : "N/A";

        // Open EditMoodActivity
        Intent editIntent = new Intent(MainActivity.this, EditMoodActivity.class);
        editIntent.putExtra("selectedMood", mood);
        editIntent.putExtra("selectedEmoji", emoji);
        editIntent.putExtra("timestamp", timestamp);
        editIntent.putExtra("trigger", trigger);
        editIntent.putExtra("socialSituation", socialSituation);
        editIntent.putExtra("moodPosition", position);
        startActivityForResult(editIntent, REQUEST_EDIT_MOOD);
    }
}
