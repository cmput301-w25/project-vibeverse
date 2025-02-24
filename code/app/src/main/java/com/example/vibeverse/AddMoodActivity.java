package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class AddMoodActivity extends AppCompatActivity {

    private Spinner emotionSpinner;
    private EditText triggerInput, socialSituationInput;
    private Button saveMoodButton;

    public static List<MoodEvent> moodEventsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_mood);

        emotionSpinner = findViewById(R.id.emotionSpinner);
        triggerInput = findViewById(R.id.triggerInput);
        socialSituationInput = findViewById(R.id.socialSituationInput);
        saveMoodButton = findViewById(R.id.saveMoodButton);

        // Populate Spinner with Mood Choices
        String[] moods = {"😡 Anger", "🤔 Confusion", "🤢 Disgust", "😨 Fear", "😃 Happiness", "😢 Sadness", "😳 Shame", "😲 Surprise"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, moods);
        emotionSpinner.setAdapter(adapter);

        saveMoodButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveMoodEvent();
            }
        });
    }

    private void saveMoodEvent() {
        String selectedMood = emotionSpinner.getSelectedItem().toString();
        String trigger = triggerInput.getText().toString().trim();
        String socialSituation = socialSituationInput.getText().toString().trim();

        if (selectedMood.isEmpty()) {
            Toast.makeText(this, "Please select a mood!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and save Mood Event with timestamp
        MoodEvent newEvent = new MoodEvent(selectedMood, trigger, socialSituation);
        moodEventsList.add(newEvent);

        Toast.makeText(this, "Mood event added!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, MainActivity.class));
    }
}
