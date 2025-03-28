package com.example.vibeverse;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
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
 * Shows a pie chart and a RecyclerView that lists daily mood cards for the selected time range,
 * plus a dynamic gradient background based on the user's most frequent mood in the filtered data.
 */
public class MoodInsightsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // UI Components
    private FrameLayout rootContainer;        // For dynamic background
    private MaterialCardView moodSummaryCard; // For coloring behind summary text
    private PieChart pieChart;
    private TextView summaryText;
    private MaterialButtonToggleGroup timeFilterGroup;
    private RecyclerView dailyMoodRecyclerView;
    private int backgroundColorForNoMoods = Color.parseColor("#000000");


    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    private final Map<String, Integer> moodColorMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mood_insight_main);

        // Initialize UI
        rootContainer = findViewById(R.id.rootContainer);
        moodSummaryCard = findViewById(R.id.moodSummaryCard);
        timeFilterGroup = findViewById(R.id.time_filter_group);
        pieChart = findViewById(R.id.pie_chart);
        summaryText = findViewById(R.id.mood_summary_text);
        dailyMoodRecyclerView = findViewById(R.id.dailyMoodRecyclerView);
        dailyMoodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyMoodRecyclerView.setHasFixedSize(true);

        // Initialize color map from XML
        initMoodColorMap();

        // ToggleGroup logic
        timeFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // Only handle "checked"
            if (checkedId == R.id.past_week) {
                updateInsights(6);  // Past 6 days + today = 7
            } else if (checkedId == R.id.last2_weeks) {
                updateInsights(13); // Past 13 days + today = 14
            } else if (checkedId == R.id.this_month) {
                updateInsights(29); // Past 29 days + today = 30
            }
        });

        // Firebase
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

        // Load moods
        loadMoodsFromFirestore();

        checkConsecutiveSadMoods(allMoodEvents);
    }

    /**
     * Load color resources from XML into our moodColorMap.
     */
    private void initMoodColorMap() {
        moodColorMap.put("Happy", ContextCompat.getColor(this, R.color.happy_color));
        moodColorMap.put("Sad", ContextCompat.getColor(this, R.color.sad_color));
        moodColorMap.put("Angry", ContextCompat.getColor(this, R.color.angry_color));
        moodColorMap.put("Surprised", ContextCompat.getColor(this, R.color.surprised_color));
        moodColorMap.put("Afraid", ContextCompat.getColor(this, R.color.afraid_color));
        moodColorMap.put("Disgusted", ContextCompat.getColor(this, R.color.disgusted_color));
        moodColorMap.put("Confused", ContextCompat.getColor(this, R.color.confused_color));
        moodColorMap.put("Shameful", ContextCompat.getColor(this, R.color.shameful_color));
        moodColorMap.put("Neutral", ContextCompat.getColor(this, R.color.happy_color));
    }

    /**
     * Fetch moods from Firestore, order by timestamp desc, store in allMoodEvents.
     */
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
                    // Default filter: Past Week
                    timeFilterGroup.check(R.id.past_week);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MoodInsightsActivity.this,
                                "Error loading moods: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Filter moods by last X days
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

    /**
     * Update the UI (pie chart, daily mood cards, summary, dynamic background) based on last X days.
     */
    private void updateInsights(int days) {
        ArrayList<MoodEvent> filteredMoods = filterMoods(days);
        drawPieChart(filteredMoods);
        updateDailyMoodCards(filteredMoods, days);
        updateEmojiSummary(filteredMoods);
        // Also apply dynamic background & button outlines
        setDynamicBackground(filteredMoods);
    }

    /**
     * Pie chart with no center color, no description label, using mood colors for slices.
     */
    private void drawPieChart(ArrayList<MoodEvent> moods) {
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }

        // 1) Map moods to emojis
        HashMap<String, String> moodEmojis = new HashMap<>();
        moodEmojis.put("Angry", "😠");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Confused", "😕");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Shameful", "😳");
        moodEmojis.put("Surprised", "😮");
        moodEmojis.put("Happy", "😄");
        moodEmojis.put("Neutral", "🙂");

        // 2) Build the PieEntries with emojis in the label
        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            String mood = entry.getKey();
            int count = entry.getValue();

            String emoji = moodEmojis.getOrDefault(mood, "🙂");
            String label = emoji + " " + mood;

            entries.add(new PieEntry(count, label));

            // Use your moodColorMap for slice color
            if (moodColorMap.containsKey(mood)) {
                colors.add(moodColorMap.get(mood));
            } else {
                colors.add(Color.WHITE);
            }
        }

        // 3) Create the DataSet
        PieDataSet dataSet = new PieDataSet(entries, /* chart label */ "");
        dataSet.setColors(colors);
        // If you only want slice labels (not numeric values)
        dataSet.setDrawValues(false);

        // 4) Assign data to the PieChart
        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // 5) Enable slice labels and set styling
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);

        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleAlpha(0);
        pieChart.getDescription().setEnabled(false);

        // Add some padding to prevent clipping
        pieChart.setExtraOffsets(10f, 10f, 10f, 10f);

        // Refresh
        pieChart.invalidate();
    }


    /**
     * Groups moods by day, sets RecyclerView adapter.
     */
    private void updateDailyMoodCards(ArrayList<MoodEvent> moods, int days) {
        Map<String, ArrayList<MoodEvent>> moodsByDay = new HashMap<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
        for (MoodEvent event : moods) {
            if (event.getDate() != null) {
                String dayKey = dayFormat.format(event.getDate());
                moodsByDay.computeIfAbsent(dayKey, k -> new ArrayList<>()).add(event);
            }
        }
        ArrayList<DailyMood> dailyMoodsList = new ArrayList<>();
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(System.currentTimeMillis() - (days * 24L * 60L * 60L * 1000L));
        truncateToDay(startCal);
        Calendar endCal = Calendar.getInstance();
        truncateToDay(endCal);

        while (!startCal.after(endCal)) {
            String dayLabel = dayFormat.format(startCal.getTime());
            ArrayList<MoodEvent> dayMoods = moodsByDay.getOrDefault(dayLabel, new ArrayList<>());
            dailyMoodsList.add(new DailyMood(dayLabel, dayMoods));
            startCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        Collections.reverse(dailyMoodsList);

        // Pass moodColorMap to the adapter so we can color the cards & chips
        DailyMoodAdapter adapter = new DailyMoodAdapter(dailyMoodsList, moodColorMap);
        dailyMoodRecyclerView.setAdapter(adapter);
    }

    /**
     * Show "You mostly felt 😄 Happy" and color the summary card.
     */
    private void updateEmojiSummary(ArrayList<MoodEvent> moods) {
        if (moods.isEmpty()) {
            summaryText.setText("No mood data 😶");
            // Reset card color if no data
            moodSummaryCard.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.card_background)
            );
            return;
        }

        // 1) Count each mood
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }

        // 2) Find top mood
        String topMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                topMood = entry.getKey();
            }
        }

        // 3) Moods to emojis
        HashMap<String, String> moodEmojis = new HashMap<>();
        moodEmojis.put("Angry", "😠");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Confused", "😕");
        moodEmojis.put("Fear", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Shameful", "😳");
        moodEmojis.put("Surprised", "😮");
        moodEmojis.put("Happy", "😄");

        String emoji = moodEmojis.getOrDefault(topMood, " ");

        // 4) "You mostly felt Happy"
        summaryText.setText("You mostly felt " + topMood +" " + emoji);

        // 5) Color the summary card to match top mood
        int moodColor = moodColorMap.containsKey(topMood)
                ? moodColorMap.get(topMood)
                : moodColorMap.get("Neutral");

        // Option B: gradient from a slightly darker color to moodColor
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {
                        ColorUtils.blendColors(moodColor, Color.BLACK, 0.1f),
                        moodColor
                }
        );
        gradient.setCornerRadius(16f);
        moodSummaryCard.setBackground(gradient);
    }

    /**
     * Helper to set date to 00:00:00
     */
    private void truncateToDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Check if the three most recent moods are "😢"
     */
    private void checkConsecutiveSadMoods(ArrayList<MoodEvent> moods) {
        if (moods == null || moods.size() < 3) {
            Log.d(TAG, "Not enough moods to check for three consecutive sad moods.");
            return;
        }
        ArrayList<MoodEvent> sortedMoods = new ArrayList<>(moods);
        Collections.sort(sortedMoods, (m1, m2) -> m2.getDate().compareTo(m1.getDate()));
        int consecutiveSadCount = 0;
        for (int i = 0; i < 3; i++) {
            MoodEvent event = sortedMoods.get(i);
            if (event.getMoodTitle().equals("😢")) {
                consecutiveSadCount++;
            } else {
                break;
            }
        }
        if (consecutiveSadCount >= 3) {
            showAreYouOkPopup();
        }
    }

    /**
     * Show a popup if user has 3 consecutive sad moods
     */
    private void showAreYouOkPopup() {
        new AlertDialog.Builder(this)
                .setTitle("Feeling Sad?")
                .setMessage("You've logged Sad 3 times in a row. Are you okay?")
                .setPositiveButton("I'm OK", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("I need help", (dialog, which) -> {
                    // Handle help action
                    dialog.dismiss();
                })
                .show();
    }

    // -------------------------------------------------------------------------
    // Dynamic Background Logic
    // -------------------------------------------------------------------------

    /**
     * Apply dynamic background to root container, and update the toggle button outlines.
     */
    private void setDynamicBackground(ArrayList<MoodEvent> moods) {
        // Determine top mood color across all filtered moods
        ArrayList<String> moodTitles = new ArrayList<>();
        for (MoodEvent event : moods) {
            moodTitles.add(event.getMoodTitle());
        }
        String mostFrequent = findMostFrequentMood(moodTitles);

        int primaryColor = moodColorMap.containsKey(mostFrequent)
                ? moodColorMap.get(mostFrequent)
                : moodColorMap.get("Neutral");

        // Store this color so we can use it for no-mood days
        backgroundColorForNoMoods = primaryColor;

        // Animate gradient on the root
        applyGradientBackground(primaryColor);
        // Toggle button outlines
        updateButtonOutlines(primaryColor);
    }


    /**
     * Finds the most frequent mood or "Neutral" if none
     */
    private String findMostFrequentMood(ArrayList<String> moods) {
        if (moods.isEmpty()) return "Neutral";
        Map<String, Integer> freqMap = new HashMap<>();
        for (String m : moods) {
            freqMap.put(m, freqMap.getOrDefault(m, 0) + 1);
        }
        String mostFrequent = "Neutral";
        int maxCount = 0;
        for (Map.Entry<String, Integer> e : freqMap.entrySet()) {
            if (e.getValue() > maxCount) {
                maxCount = e.getValue();
                mostFrequent = e.getKey();
            }
        }
        return mostFrequent;
    }

    /**
     * Animates a gradient from dark->light->baseColor on the root container
     */
    private void applyGradientBackground(int baseColor) {
        int lighterColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.7f);
        int mediumColor = ColorUtils.blendColors(baseColor, Color.WHITE, 0.3f);
        int[] colors = new int[] { Color.parseColor("#2D2D3A"), lighterColor, mediumColor, baseColor };
        GradientDrawable gradient = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        gradient.setCornerRadius(0f);

        TransitionManager.beginDelayedTransition(rootContainer);
        rootContainer.setBackground(gradient);
    }

    /**
     * Update stroke color of each button in timeFilterGroup
     */
    private void updateButtonOutlines(int color) {
        int count = timeFilterGroup.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = timeFilterGroup.getChildAt(i);
            if (child instanceof MaterialButton) {
                MaterialButton button = (MaterialButton) child;
                button.setStrokeColor(ColorStateList.valueOf(color));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper Classes for RecyclerView
    // -------------------------------------------------------------------------

    /**
     * Represents all moods for a single day.
     */
    public static class DailyMood {
        public String dayLabel;
        public ArrayList<MoodEvent> moods;

        public DailyMood(String dayLabel, ArrayList<MoodEvent> moods) {
            this.dayLabel = dayLabel;
            this.moods = moods;
        }
    }

    /**
     * Adapter for daily moods (item_daily_mood.xml)
     */
    private class DailyMoodAdapter extends RecyclerView.Adapter<DailyMoodAdapter.DailyMoodViewHolder> {

        private final ArrayList<DailyMood> dailyMoods;
        private final Map<String, Integer> moodColorMap;

        public DailyMoodAdapter(ArrayList<DailyMood> dailyMoods, Map<String, Integer> moodColorMap) {
            this.dailyMoods = dailyMoods;
            this.moodColorMap = moodColorMap;
        }

        @Override
        public DailyMoodViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_daily_mood, parent, false);
            return new DailyMoodViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DailyMoodViewHolder holder, int position) {
            DailyMood dailyMood = dailyMoods.get(position);

            // 1) Set the day label
            holder.dayHeader.setText(dailyMood.dayLabel);

            // 2) Find the day's top mood color
            int topMoodColor = getTopMoodColorForDay(dailyMood.moods);

            // 3) Create a gradient background for the card
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                            ColorUtils.blendColors(topMoodColor, Color.BLACK, 0.2f),
                            topMoodColor
                    }
            );
            gradient.setCornerRadius(16f);
            holder.dayCard.setBackground(gradient);

            // 4) Clear any existing Chips
            holder.moodChipGroup.removeAllViews();

            // If no moods logged that day
            if (dailyMood.moods.isEmpty()) {
                Chip noMoodsChip = new Chip(holder.itemView.getContext());
                noMoodsChip.setText("No moods");
                noMoodsChip.setTextColor(Color.WHITE);
                // Give the "No moods" chip a subtle background
                noMoodsChip.setChipBackgroundColor(
                        ColorStateList.valueOf(ColorUtils.blendColors(topMoodColor, Color.BLACK, 0.4f))
                );
                holder.moodChipGroup.addView(noMoodsChip);
                return;
            }

            // 5) Add a Chip for each MoodEvent, colored by that mood
            for (MoodEvent event : dailyMood.moods) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(event.getMoodTitle());
                chip.setTextColor(Color.WHITE);

                // Base color from the mood map or fallback to the top mood color
                int moodBaseColor = moodColorMap.getOrDefault(event.getMoodTitle(), topMoodColor);
                // Darken slightly for contrast
                int chipColor = ColorUtils.blendColors(moodBaseColor, Color.BLACK, 0.2f);

                chip.setChipBackgroundColor(ColorStateList.valueOf(chipColor));
                holder.moodChipGroup.addView(chip);
            }
        }

        @Override
        public int getItemCount() {
            return dailyMoods.size();
        }

        /**
         * Returns the color of the most frequent mood for that day.
         */
        private int getTopMoodColorForDay(ArrayList<MoodEvent> dayMoods) {
            if (dayMoods == null || dayMoods.isEmpty()) {
                return backgroundColorForNoMoods;
            }


            // Count each mood
            HashMap<String, Integer> moodCount = new HashMap<>();
            for (MoodEvent event : dayMoods) {
                String moodTitle = event.getMoodTitle();
                moodCount.put(moodTitle, moodCount.getOrDefault(moodTitle, 0) + 1);
            }

            // Find the top mood
            int maxCount = 0;
            String topMood = "Neutral";
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    topMood = entry.getKey();
                }
            }

            // Return that mood's color, or a default
            return moodColorMap.getOrDefault(topMood, moodColorMap.get("Neutral"));
        }

        class DailyMoodViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView dayCard;
            TextView dayHeader;
            ChipGroup moodChipGroup;

            DailyMoodViewHolder(View itemView) {
                super(itemView);
                dayCard = itemView.findViewById(R.id.dayCard);
                dayHeader = itemView.findViewById(R.id.dayHeader);
                moodChipGroup = itemView.findViewById(R.id.moodChipGroup);
            }
        }
    }

    /**
     * Helper class for blending colors
     */
    private static class ColorUtils {
        /**
         * Blends two colors with the given ratio (0..1).
         */
        public static int blendColors(int color1, int color2, float ratio) {
            float inverseRatio = 1f - ratio;
            float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRatio);
            float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRatio);
            float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRatio);
            return Color.rgb((int) r, (int) g, (int) b);
        }
    }
}
