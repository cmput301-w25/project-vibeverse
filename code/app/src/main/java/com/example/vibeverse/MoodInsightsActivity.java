package com.example.vibeverse;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Shows a pie chart and a RecyclerView that lists daily mood cards for the selected time range.
 */
public class MoodInsightsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // UI Components
    private PieChart pieChart;
    private TextView summaryText;
    private RadioGroup timeFilterGroup;
    private RecyclerView dailyMoodRecyclerView;

    // Data
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    // Adjust this format if needed to match your Firestore string timestamps
    private SimpleDateFormat sourceFormat = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mood_insight_main);

        // Initialize UI
        timeFilterGroup = findViewById(R.id.time_filter_group);
        pieChart = findViewById(R.id.pie_chart);
        summaryText = findViewById(R.id.mood_summary_text);
        dailyMoodRecyclerView = findViewById(R.id.dailyMoodRecyclerView);
        dailyMoodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyMoodRecyclerView.setHasFixedSize(true);

        // RadioGroup logic: update insights based on selection
        timeFilterGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.past_week) {
                updateInsights(6);
            } else if (checkedId == R.id.last2_weeks) {
                updateInsights(13);
            } else if (checkedId == R.id.this_month) {
                updateInsights(29);
            }
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Determine user ID
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }

        // Load data
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
                    // Default: Past week
                    timeFilterGroup.check(R.id.past_week);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MoodInsightsActivity.this,
                                "Error loading moods: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Filters mood events within the past 'days' days.
     */
    private ArrayList<MoodEvent> filterMoods(int days) {
        ArrayList<MoodEvent> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();
        long cutoff = now - (days * 24L * 60L * 60L * 1000L);

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
        updateDailyMoodCards(filteredMoods, days);
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

    /**
     * Groups moods by day in the given range and shows them in the RecyclerView.
     * Also includes days with no moods (displays "No moods").
     */
    private void updateDailyMoodCards(ArrayList<MoodEvent> moods, int days) {
        // 1. Group existing moods by day
        Map<String, ArrayList<MoodEvent>> moodsByDay = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());

        for (MoodEvent event : moods) {
            if (event.getDate() != null) {
                String dayKey = dayFormat.format(event.getDate());
                moodsByDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(event);
            }
        }

        // 2. Build a day-by-day list from the cutoff day to "today"
        ArrayList<DailyMood> dailyMoodsList = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L));
        truncateToDay(startCal); // e.g., sets hours/min/sec to 0

        Calendar endCal = Calendar.getInstance();
        truncateToDay(endCal);

        while (!startCal.after(endCal)) {
            String dayLabel = dayFormat.format(startCal.getTime());
            ArrayList<MoodEvent> dayMoods = moodsByDay.getOrDefault(dayLabel, new ArrayList<>());
            dailyMoodsList.add(new DailyMood(dayLabel, dayMoods));
            startCal.add(Calendar.DAY_OF_YEAR, 1);
        }

        Collections.reverse(dailyMoodsList);

        // 4. Set the adapter
        DailyMoodAdapter adapter = new DailyMoodAdapter(dailyMoodsList);
        dailyMoodRecyclerView.setAdapter(adapter);
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
        // Find top mood
        String topMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topMood = entry.getKey();
            }
        }
        // Map mood to emoji
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

    /**
     * Helper to truncate Calendar to 00:00:00 for day-based iteration.
     */
    private void truncateToDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    // -------------------------------------------------------------------------
    // Helper Classes
    // -------------------------------------------------------------------------

    /**
     * Represents all moods for a single day.
     */
    public static class DailyMood {
        public String dayLabel;          // e.g., "Monday, Mar 24"
        public ArrayList<MoodEvent> moods; // Moods recorded on this day

        public DailyMood(String dayLabel, ArrayList<MoodEvent> moods) {
            this.dayLabel = dayLabel;
            this.moods = moods;
        }
    }

    /**
     * Adapter for showing each day's moods in a card (item_daily_mood.xml).
     */
    private class DailyMoodAdapter extends RecyclerView.Adapter<DailyMoodAdapter.DailyMoodViewHolder> {
        private final ArrayList<DailyMood> dailyMoods;

        public DailyMoodAdapter(ArrayList<DailyMood> dailyMoods) {
            this.dailyMoods = dailyMoods;
        }

        @Override
        public DailyMoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_daily_mood, parent, false);
            return new DailyMoodViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DailyMoodViewHolder holder, int position) {
            DailyMood dailyMood = dailyMoods.get(position);
            holder.dayHeader.setText(dailyMood.dayLabel);

            // Clear previous chips
            holder.moodChipGroup.removeAllViews();

            // If no moods, show a "No moods" chip
            if (dailyMood.moods.isEmpty()) {
                Chip noMoodsChip = new Chip(MoodInsightsActivity.this);
                noMoodsChip.setText("No moods");
                holder.moodChipGroup.addView(noMoodsChip);
                return;
            }

            // Otherwise, add a chip for each mood
            for (MoodEvent event : dailyMood.moods) {
                Chip chip = new Chip(MoodInsightsActivity.this);
                chip.setText(event.getMoodTitle());
                holder.moodChipGroup.addView(chip);
            }
        }

        @Override
        public int getItemCount() {
            return dailyMoods.size();
        }

        class DailyMoodViewHolder extends RecyclerView.ViewHolder {
            TextView dayHeader;
            ChipGroup moodChipGroup;

            public DailyMoodViewHolder(View itemView) {
                super(itemView);
                dayHeader = itemView.findViewById(R.id.dayHeader);
                moodChipGroup = itemView.findViewById(R.id.moodChipGroup);
            }
        }
    }
}
