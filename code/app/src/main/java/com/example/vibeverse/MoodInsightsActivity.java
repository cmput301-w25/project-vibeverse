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
import com.google.android.material.appbar.MaterialToolbar;
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
 * Displays mood insights with a PieChart and a RecyclerView for daily moods,
 * along with a dynamic background that reflects the most frequent mood in the selected range.
 * Users can switch between time filters (past week, two weeks, or month).
 */
public class MoodInsightsActivity extends AppCompatActivity {

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String userId;

    // UI Components
    private FrameLayout rootContainer;         // Dynamically colored background
    private MaterialCardView moodSummaryCard;  // Colored card behind summary text
    private PieChart pieChart;
    private TextView summaryText;
    private MaterialButtonToggleGroup timeFilterGroup;
    private RecyclerView dailyMoodRecyclerView;
    private int backgroundColorForNoMoods = Color.parseColor("#000000"); // Default fallback color

    // Data
    private ArrayList<MoodEvent> allMoodEvents = new ArrayList<>();
    private final SimpleDateFormat sourceFormat =
            new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());

    // Map linking mood titles to colors (loaded from XML resources)
    private final Map<String, Integer> moodColorMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mood_insight_main);

        // 1) Initialize UI
        rootContainer = findViewById(R.id.rootContainer);
        moodSummaryCard = findViewById(R.id.moodSummaryCard);
        timeFilterGroup = findViewById(R.id.time_filter_group);
        pieChart = findViewById(R.id.pie_chart);
        summaryText = findViewById(R.id.mood_summary_text);
        dailyMoodRecyclerView = findViewById(R.id.dailyMoodRecyclerView);
        dailyMoodRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        dailyMoodRecyclerView.setHasFixedSize(true);

        // 2) Initialize color map for moods
        initMoodColorMap();

        // 3) Set up time filter toggles
        timeFilterGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return; // Only handle "checked"
            if (checkedId == R.id.past_week) {
                updateInsights(6);  // 6 days ago + today = 7
            } else if (checkedId == R.id.last2_weeks) {
                updateInsights(13); // 13 days ago + today = 14
            } else if (checkedId == R.id.this_month) {
                updateInsights(29); // 29 days ago + today = 30
            }
        });

        // 4) Firebase initialization
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 5) Determine the user's ID
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

        // 6) Load moods from Firestore
        loadMoodsFromFirestore();

        // 7) Check for consecutive sad moods
        checkConsecutiveSadMoods(allMoodEvents);

        // 8) Toolbar back button
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    /**
     * Loads mood -> color mappings from XML resources and populates moodColorMap.
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
     * Fetches mood entries from Firestore, ordered by timestamp descending,
     * and populates allMoodEvents.
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
     * Filters moods to only include entries from the past {@code days} days, plus today.
     *
     * @param days number of days to look back
     * @return a filtered list of MoodEvent objects
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
     * Updates insights (pie chart, daily list, summary, background) for the past {@code days}.
     *
     * @param days the number of days to include in the analysis
     */
    private void updateInsights(int days) {
        ArrayList<MoodEvent> filteredMoods = filterMoods(days);
        drawPieChart(filteredMoods);
        updateDailyMoodCards(filteredMoods, days);
        updateEmojiSummary(filteredMoods);
        setDynamicBackground(filteredMoods);
    }

    /**
     * Draws a pie chart representing the breakdown of moods in the given list of MoodEvents.
     *
     * @param moods the list of moods to plot
     */
    private void drawPieChart(ArrayList<MoodEvent> moods) {
        HashMap<String, Integer> moodCount = new HashMap<>();
        for (MoodEvent event : moods) {
            String mood = event.getMoodTitle();
            moodCount.put(mood, moodCount.getOrDefault(mood, 0) + 1);
        }

        // Map moods to emojis
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

        ArrayList<PieEntry> entries = new ArrayList<>();
        ArrayList<Integer> colors = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
            String mood = entry.getKey();
            int count = entry.getValue();

            // Create a label with emoji
            String emoji = moodEmojis.getOrDefault(mood, "🙂");
            String label = emoji + " " + mood;

            // Pie entry
            entries.add(new PieEntry(count, label));

            // Slice color from moodColorMap, or white if not found
            if (moodColorMap.containsKey(mood)) {
                colors.add(moodColorMap.get(mood));
            } else {
                colors.add(Color.WHITE);
            }
        }

        // Create PieDataSet
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false); // Hide numeric slice values

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Entry labels
        pieChart.setDrawEntryLabels(true);
        pieChart.setEntryLabelColor(Color.WHITE);
        pieChart.setEntryLabelTextSize(12f);

        // Hide center hole background
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.TRANSPARENT);
        pieChart.setTransparentCircleAlpha(0);

        // Disable description text
        pieChart.getDescription().setEnabled(false);

        // Add padding
        pieChart.setExtraOffsets(10f, 10f, 10f, 10f);

        // Refresh the chart
        pieChart.invalidate();
    }

    /**
     * Groups the moods by day and sets up a RecyclerView adapter to display daily mood cards.
     *
     * @param moods the list of mood events
     * @param days  the number of days used in filtering
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

        // Build the daily range from 'startCal' to 'endCal'
        while (!startCal.after(endCal)) {
            String dayLabel = dayFormat.format(startCal.getTime());
            ArrayList<MoodEvent> dayMoods = moodsByDay.getOrDefault(dayLabel, new ArrayList<>());
            dailyMoodsList.add(new DailyMood(dayLabel, dayMoods));
            startCal.add(Calendar.DAY_OF_YEAR, 1);
        }
        // Reverse so the most recent day is at the top
        Collections.reverse(dailyMoodsList);

        DailyMoodAdapter adapter = new DailyMoodAdapter(dailyMoodsList, moodColorMap);
        dailyMoodRecyclerView.setAdapter(adapter);
    }

    /**
     * Updates the summary text to reflect the most frequent mood in the provided list
     * and colors the summary card accordingly.
     *
     * @param moods the list of filtered mood events
     */
    private void updateEmojiSummary(ArrayList<MoodEvent> moods) {
        if (moods.isEmpty()) {
            summaryText.setText("No mood data 😶");
            moodSummaryCard.setCardBackgroundColor(
                    ContextCompat.getColor(this, R.color.card_background)
            );
            return;
        }

        // Count each mood
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

        // Map moods to emojis
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

        summaryText.setText("You mostly felt " + topMood + " " + emoji);

        // Color the summary card
        int moodColor = moodColorMap.containsKey(topMood)
                ? moodColorMap.get(topMood)
                : moodColorMap.get("Neutral");

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
     * Resets the given Calendar to midnight (00:00:00).
     *
     * @param cal the Calendar instance to truncate
     */
    private void truncateToDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Checks if the user has logged "Sad" moods at least three times in a row.
     * If so, displays a supportive popup.
     *
     * @param moods the complete list of mood events
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
     * Displays a popup dialog if the user has three consecutive "Sad" moods logged.
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
     * Analyzes the filtered moods, determines the most frequent mood color,
     * applies a gradient background, and updates toggle button outlines.
     *
     * @param moods the list of mood events to analyze
     */
    private void setDynamicBackground(ArrayList<MoodEvent> moods) {
        // Find most frequent mood
        ArrayList<String> moodTitles = new ArrayList<>();
        for (MoodEvent event : moods) {
            moodTitles.add(event.getMoodTitle());
        }
        String mostFrequent = findMostFrequentMood(moodTitles);

        int primaryColor = moodColorMap.containsKey(mostFrequent)
                ? moodColorMap.get(mostFrequent)
                : moodColorMap.get("Neutral");

        // For no-mood days
        backgroundColorForNoMoods = primaryColor;

        // Animate gradient background
        applyGradientBackground(primaryColor);
        // Update button outlines
        updateButtonOutlines(primaryColor);
    }


    /**
     * Finds the most frequent mood in a list of mood titles, or returns "Neutral" if empty.
     *
     * @param moods a list of mood titles
     * @return the most frequent mood string
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
     * Applies a vertical gradient background to the root container,
     * blending from a dark color to the base mood color.
     *
     * @param baseColor the color representing the top mood
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
     * Updates the stroke (outline) color of each button in the toggle group to the given color.
     *
     * @param color the color to apply to the button outlines
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

        /**
         * Constructs a DailyMood object with a label and a list of mood events.
         *
         * @param dayLabel a string label (e.g., "Monday, Mar 24")
         * @param moods    the list of MoodEvent objects for that day
         */
        public DailyMood(String dayLabel, ArrayList<MoodEvent> moods) {
            this.dayLabel = dayLabel;
            this.moods = moods;
        }
    }

    /**
     * Adapter for displaying daily mood data, grouped by day, in a RecyclerView.
     */
    private class DailyMoodAdapter extends RecyclerView.Adapter<DailyMoodAdapter.DailyMoodViewHolder> {

        private final ArrayList<DailyMood> dailyMoods;
        private final Map<String, Integer> moodColorMap;

        /**
         * Constructs the adapter with daily moods and a mood-color mapping.
         *
         * @param dailyMoods   the list of DailyMood objects
         * @param moodColorMap a Map linking mood titles to their integer color values
         */
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

            // Set day header
            holder.dayHeader.setText(dailyMood.dayLabel);

            // Determine top mood color for the day
            int topMoodColor = getTopMoodColorForDay(dailyMood.moods);

            // Create a gradient background for the day's card
            GradientDrawable gradient = new GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {
                            ColorUtils.blendColors(topMoodColor, Color.BLACK, 0.2f),
                            topMoodColor
                    }
            );
            gradient.setCornerRadius(16f);
            holder.dayCard.setBackground(gradient);

            // Clear any existing chips
            holder.moodChipGroup.removeAllViews();

            // If no moods logged that day
            if (dailyMood.moods.isEmpty()) {
                Chip noMoodsChip = new Chip(holder.itemView.getContext());
                noMoodsChip.setText("No moods");
                noMoodsChip.setTextColor(Color.WHITE);
                noMoodsChip.setChipBackgroundColor(
                        ColorStateList.valueOf(ColorUtils.blendColors(topMoodColor, Color.BLACK, 0.4f))
                );
                holder.moodChipGroup.addView(noMoodsChip);
                return;
            }

            // For each MoodEvent, create a Chip
            for (MoodEvent event : dailyMood.moods) {
                Chip chip = new Chip(holder.itemView.getContext());
                chip.setText(event.getMoodTitle());
                chip.setTextColor(Color.WHITE);

                // Base color from mood map or fallback
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
         * Determines the top mood color for a given day by finding the most frequent mood.
         *
         * @param dayMoods the list of MoodEvent objects for the day
         * @return the integer color value corresponding to the top mood
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

            // Find highest count
            int maxCount = 0;
            String topMood = "Neutral";
            for (Map.Entry<String, Integer> entry : moodCount.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    topMood = entry.getKey();
                }
            }

            // Return color from map or default
            return moodColorMap.getOrDefault(topMood, moodColorMap.get("Neutral"));
        }

        /**
         * Holds references to the views for each daily mood item.
         */
        class DailyMoodViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView dayCard;
            TextView dayHeader;
            ChipGroup moodChipGroup;

            /**
             * Constructs the ViewHolder for daily mood items.
             *
             * @param itemView the item view containing dayCard, dayHeader, and moodChipGroup
             */
            DailyMoodViewHolder(View itemView) {
                super(itemView);
                dayCard = itemView.findViewById(R.id.dayCard);
                dayHeader = itemView.findViewById(R.id.dayHeader);
                moodChipGroup = itemView.findViewById(R.id.moodChipGroup);
            }
        }
    }

    /**
     * Utility class for blending two colors.
     */
    private static class ColorUtils {
        /**
         * Blends two colors with a given ratio.
         * A ratio of 0f returns color2, 1f returns color1.
         *
         * @param color1 the first color
         * @param color2 the second color
         * @param ratio  blending ratio between 0..1
         * @return the blended color as an int
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
