package com.example.vibeverse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

public class MoodInsights extends Fragment {

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // UI Components
    private PieChart pieChart;
    private LineChart lineChart;
    private TextView summaryText;
    private RadioGroup timeFilterGroup;
    private View progressLoading;
    private View recyclerFeed;
    private View emptyStateView;

    // Data variables
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
    private MoodEventAdapter moodEventAdapter; // Assume this adapter is properly implemented elsewhere.
    private static final String TAG = "MoodInsights";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Determine user ID
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            // Not logged in: use device-based ID from SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);
            if (userId == null) {
                userId = UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the fragment layout (ensure this layout file exists)
        View rootView = inflater.inflate(R.layout.mood_insight_main, container, false);

        // Initialize UI components
        pieChart = rootView.findViewById(R.id.pie_chart);
        lineChart = rootView.findViewById(R.id.line_chart);
        summaryText = rootView.findViewById(R.id.mood_summary_text);
        timeFilterGroup = rootView.findViewById(R.id.time_filter_group);

        // Set up time filter toggle listener (if desired)
        timeFilterGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // For example: 7 days for one option, 14 for another, etc.
                if (checkedId == R.id.past_week) {
                    updateInsights(7);
                } else if (checkedId == R.id.last2_weeks) {
                    updateInsights(14);
                } else if (checkedId == R.id.this_month) {
                    updateInsights(30);
                }
            }
        });

        loadMoodsFromFirestore();

        return rootView;
    }

    private void loadMoodsFromFirestore() {
        // Show progress indicator and recycler view; hide empty state view
        if (progressLoading != null) {
            progressLoading.setVisibility(View.VISIBLE);
        }
        if (recyclerFeed != null) {
            recyclerFeed.setVisibility(View.VISIBLE);
        }
        if (emptyStateView != null) {
            emptyStateView.setVisibility(View.GONE);
        }

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

                            // Build subtitle for social situation, if available.
                            StringBuilder subtitle = new StringBuilder();
                            if (moodEvent.getSocialSituation() != null && !moodEvent.getSocialSituation().isEmpty()) {
                                if (subtitle.length() > 0) {
                                    subtitle.append(" | ");
                                }
                                subtitle.append("Social: ").append(moodEvent.getSocialSituation());
                            }
                            moodEvent.setSubtitle(subtitle.toString());

                            allMoodEvents.add(moodEvent);
                        } catch (ParseException e) {
                            Log.e(TAG, "Error parsing timestamp", e);
                        }
                    }
                    // Update your adapter with the new mood events (if adapter is initialized)
                    if (moodEventAdapter != null) {
                        moodEventAdapter.updateMoodEvents(new ArrayList<>(allMoodEvents));
                    }

                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    if (allMoodEvents.isEmpty()) {
                        if (emptyStateView != null) {
                            if (recyclerFeed != null) {
                                recyclerFeed.setVisibility(View.GONE);
                            }
                            emptyStateView.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(requireContext(),
                                    "No mood entries found. Add one!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // Update charts with a default time range (for example, past week)
                        updateInsights(7);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressLoading != null) {
                        progressLoading.setVisibility(View.GONE);
                    }
                    Toast.makeText(requireContext(),
                            "Error loading moods: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Filter the mood events based on the number of days from now
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

    // Update the charts and summary based on the filtered moods for the given range (in days)
    private void updateInsights(int days) {
        ArrayList<MoodEvent> filteredMoods = filterMoods(days);
        drawPieChart(filteredMoods);
        drawLineChart(filteredMoods);
        updateEmojiSummary(filteredMoods);
    }

    // Draw the Pie Chart using the filtered mood data
    private void drawPieChart(ArrayList<MoodEvent> moods) {
        // Create a map to count each mood type.
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle(); // Assume getEmotion() returns a String
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }

        // Create PieChart entries.
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mood Breakdown");
        // Optionally, set colors using a ColorTemplate.
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.invalidate(); // Refresh the chart
    }

    // Draw the Line Chart showing the mood trend using the filtered mood data
    private void drawLineChart(ArrayList<MoodEvent> moods) {
        // Define a local mapping for mood scores.
        HashMap<String, Integer> moodScores = new HashMap<>();
        moodScores.put("Anger", 1);
        moodScores.put("Sadness", 2);
        moodScores.put("Confusion", 3);
        moodScores.put("Fear", 2);
        moodScores.put("Disgust", 2);
        moodScores.put("Shame", 1);
        moodScores.put("Surprise", 4);
        moodScores.put("Happiness", 5);

        // Group moods by day (using only the day part).
        HashMap<String, ArrayList<Integer>> scoresByDate = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        for (MoodEvent event : moods) {
            if (event.getDate() != null) {
                String day = dayFormat.format(event.getDate());
                int score = moodScores.getOrDefault(event.getMoodTitle(), 3);
                if (!scoresByDate.containsKey(day)) {
                    scoresByDate.put(day, new ArrayList<>());
                }
                scoresByDate.get(day).add(score);
            }
        }

        // Calculate daily averages and prepare chart entries.
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
        lineChart.invalidate(); // Refresh the chart
    }

    // Update the emoji summary based on the most frequent mood in the filtered data
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

        // Simple emoji mapping
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
