package com.example.vibeverse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MoodInsightsActivity extends AppCompatActivity {

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // UI Components from mood_insight_main.xml
    private PieChart pieChart;
    private LineChart lineChart;
    private TextView summaryText;

    // Data variables
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    // Assume Firestore timestamps are in ISO format, e.g., "2025-03-23T12:34:56Z"
    private SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    private static final String TAG = "MoodInsightsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Use the layout that displays your insights (charts, toggle buttons, emoji summary, etc.)
        setContentView(R.layout.mood_insight_main);

        // Initialize UI components.
        pieChart = findViewById(R.id.pie_chart);
        lineChart = findViewById(R.id.line_chart);
        summaryText = findViewById(R.id.mood_summary_text);

        // Initialize Firebase Auth and Firestore.
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Determine user ID.
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);
            if (userId == null) {
                userId = UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }

        // Load mood data from Firestore and then update the graphs and summary.
        loadMoodsFromFirestore();
    }

    private void loadMoodsFromFirestore() {
        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    allMoodEvents.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        try {
                            MoodEvent moodEvent = MoodEvent.fromMap(doc.getData());
                            if (moodEvent.getTimestamp() != null) {
                                Date date = sourceFormat.parse(moodEvent.getTimestamp());
                                if (date != null) {
                                    moodEvent.setDate(date);
                                }
                            }
                            moodEvent.setDocumentId(doc.getId());
                            allMoodEvents.add(moodEvent);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                    // Update insights with a default filter of past 7 days.
                    updateInsights(7);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MoodInsightsActivity.this, "Error loading moods: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private ArrayList<MoodEvent> filterMoods(int days) {
        ArrayList<MoodEvent> filtered = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long cutoff = currentTime - days * 24 * 60 * 60 * 1000L;
        for (MoodEvent event : allMoodEvents) {
            if (event.getDate() != null && event.getDate().getTime() >= cutoff) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private void updateInsights(int days) {
        ArrayList<MoodEvent> filteredMoods = filterMoods(days);
        drawPieChart(filteredMoods);
        drawLineChart(filteredMoods);
        updateEmojiSummary(filteredMoods);
    }

    private void drawPieChart(ArrayList<MoodEvent> moods) {
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }

        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mood Breakdown");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void drawLineChart(ArrayList<MoodEvent> moods) {
        HashMap<String, Integer> moodScores = new HashMap<>();
        moodScores.put("Anger", 1);
        moodScores.put("Sadness", 2);
        moodScores.put("Confusion", 3);
        moodScores.put("Fear", 2);
        moodScores.put("Disgust", 2);
        moodScores.put("Shame", 1);
        moodScores.put("Surprise", 4);
        moodScores.put("Happiness", 5);

        HashMap<String, ArrayList<Integer>> scoresByDate = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (MoodEvent event : moods) {
            if (event.getDate() != null) {
                String day = dayFormat.format(event.getDate());
                int score = moodScores.getOrDefault(event.getMoodTitle(), 3);
                scoresByDate.computeIfAbsent(day, k -> new ArrayList<>()).add(score);
            }
        }

        ArrayList<Entry> lineEntries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, ArrayList<Integer>> entry : scoresByDate.entrySet()) {
            ArrayList<Integer> scores = entry.getValue();
            float average = 0;
            for (int s : scores) {
                average += s;
            }
            average /= scores.size();
            lineEntries.add(new Entry(index++, average));
        }

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Mood Trend");
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setValueTextSize(10f);
        LineData lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);
        lineChart.invalidate();
    }

    private void updateEmojiSummary(ArrayList<MoodEvent> moods) {
        if (moods.isEmpty()) {
            summaryText.setText("No mood data 😶");
            return;
        }
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }
        String topMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topMood = entry.getKey();
            }
        }
        HashMap<String, String> moodEmojis = new HashMap<>();
        moodEmojis.put("Anger", "😠");
        moodEmojis.put("Sadness", "😢");
        moodEmojis.put("Confusion", "😕");
        moodEmojis.put("Fear", "😨");
        moodEmojis.put("Disgust", "🤢");
        moodEmojis.put("Shame", "😳");
        moodEmojis.put("Surprise", "😮");
        moodEmojis.put("Happiness", "😄");
        String emoji = moodEmojis.getOrDefault(topMood, "🙂");
        summaryText.setText("You mostly felt " + emoji + " (" + topMood + ")");
    }
}
