package com.example.vibeverse;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FilterDialog {

    private static String lastTimeFilter = "all_time";
    private static boolean lastIsHappy = false;
    private static boolean lastIsSad = false;
    private static boolean lastIsAngry = false;
    private static boolean lastIsSurprised = false;
    private static boolean lastIsAfraid = false;
    private static boolean lastIsDisgusted = false;
    private static boolean lastIsConfused = false;
    private static boolean lastIsShameful = false;

    public interface FilterListener {
        void onFilterApplied(String timeFilter,
                             boolean isHappy,
                             boolean isSad,
                             boolean isAngry,
                             boolean isSurprised,
                             boolean isAfraid,
                             boolean isDisgusted,
                             boolean isConfused,
                             boolean isShameful);

        void onFilteredResults(List<MoodEvent> results);
    }

    public static void show(Context context, FilterListener listener, List<MoodEvent> allMoodEvents) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filters);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        // Find radio buttons for time filter selection.
        RadioGroup radioGroupTime = dialog.findViewById(R.id.radioGroupTime);
        RadioButton radioLast24 = dialog.findViewById(R.id.radioLast24);
        RadioButton radioLast3Days = dialog.findViewById(R.id.radioLast3Days);
        RadioButton radioLastWeek = dialog.findViewById(R.id.radioLastWeek);
        RadioButton radioLastMonth = dialog.findViewById(R.id.radioLastMonth);
        RadioButton radioAllTime = dialog.findViewById(R.id.radioAllTime);

        // Set last selected time filter
        switch (lastTimeFilter) {
            case "last_24_hours":
                radioLast24.setChecked(true);
                break;
            case "3Days":
                radioLast3Days.setChecked(true);
                break;
            case "last_week":
                radioLastWeek.setChecked(true);
                break;
            case "last_month":
                radioLastMonth.setChecked(true);
                break;
            default:
                radioAllTime.setChecked(true);
                break;
        }

        // Find checkboxes for mood selection.
        CheckBox checkHappy     = dialog.findViewById(R.id.checkHappy);
        CheckBox checkSad       = dialog.findViewById(R.id.checkSad);
        CheckBox checkAngry     = dialog.findViewById(R.id.checkAngry);
        CheckBox checkSurprised = dialog.findViewById(R.id.checkSurprised);
        CheckBox checkAfraid    = dialog.findViewById(R.id.checkAfraid);
        CheckBox checkDisgusted = dialog.findViewById(R.id.checkDisgusted);
        CheckBox checkConfused  = dialog.findViewById(R.id.checkConfused);
        CheckBox checkShameful  = dialog.findViewById(R.id.checkShameful);

        // Set last selected moods
        checkHappy.setChecked(lastIsHappy);
        checkSad.setChecked(lastIsSad);
        checkAngry.setChecked(lastIsAngry);
        checkSurprised.setChecked(lastIsSurprised);
        checkAfraid.setChecked(lastIsAfraid);
        checkDisgusted.setChecked(lastIsDisgusted);
        checkConfused.setChecked(lastIsConfused);
        checkShameful.setChecked(lastIsShameful);

        // Find the apply filters button.
        Button buttonApplyFilters = dialog.findViewById(R.id.buttonApplyFilters);
        buttonApplyFilters.setOnClickListener(v -> {
            String timeFilter = "all_time";
            int selectedId = radioGroupTime.getCheckedRadioButtonId();
            if (selectedId == radioLast24.getId()) {
                timeFilter = "last_24_hours";
            } else if (selectedId == radioLast3Days.getId()) {
                timeFilter = "3Days";
            } else if (selectedId == radioLastWeek.getId()) {
                timeFilter = "last_week";
            } else if (selectedId == radioLastMonth.getId()) {
                timeFilter = "last_month";
            } else if (selectedId == radioAllTime.getId()) {
                timeFilter = "all_time";
            }

            boolean isHappy     = checkHappy.isChecked();
            boolean isSad       = checkSad.isChecked();
            boolean isAngry     = checkAngry.isChecked();
            boolean isSurprised = checkSurprised.isChecked();
            boolean isAfraid    = checkAfraid.isChecked();
            boolean isDisgusted = checkDisgusted.isChecked();
            boolean isConfused  = checkConfused.isChecked();
            boolean isShameful  = checkShameful.isChecked();

            // Save current selections for persistence
            lastTimeFilter = timeFilter;
            lastIsHappy = isHappy;
            lastIsSad = isSad;
            lastIsAngry = isAngry;
            lastIsSurprised = isSurprised;
            lastIsAfraid = isAfraid;
            lastIsDisgusted = isDisgusted;
            lastIsConfused = isConfused;
            lastIsShameful = isShameful;

            // Notify the listener immediately with the selected filter options.
            listener.onFilterApplied(
                    timeFilter,
                    isHappy, isSad, isAngry, isSurprised,
                    isAfraid, isDisgusted, isConfused, isShameful
            );

            // Apply filters to the provided list instead of fetching from Firestore
            applyFilters(
                    context, listener, allMoodEvents, timeFilter,
                    isHappy, isSad, isAngry, isSurprised,
                    isAfraid, isDisgusted, isConfused, isShameful
            );

            dialog.dismiss();
        });

        dialog.show();
    }

    private static void applyFilters(
            Context context,
            FilterListener listener,
            List<MoodEvent> allMoodEvents,
            String timeFilter,
            boolean isHappy,
            boolean isSad,
            boolean isAngry,
            boolean isSurprised,
            boolean isAfraid,
            boolean isDisgusted,
            boolean isConfused,
            boolean isShameful
    ) {
        List<MoodEvent> filtered = new ArrayList<>();
        long now = System.currentTimeMillis();

        // Iterate over each mood event and apply filters
        for (MoodEvent m : allMoodEvents) {
            if (m.getDate() == null) continue;

            long moodTime = m.getDate().getTime();

            // Time filter check
            boolean inTime = false;
            switch (timeFilter) {
                case "last_24_hours":
                    inTime = (now - moodTime) <= 86400000L;
                    break;
                case "3Days":
                    inTime = (now - moodTime) <= 259200000L;
                    break;
                case "last_week":
                    inTime = (now - moodTime) <= 604800000L;
                    break;
                case "last_month":
                    inTime = (now - moodTime) <= 2592000000L;
                    break;
                default:
                    inTime = true;
            }
            if (!inTime) continue;

            // Mood filter check: convert mood title to uppercase for comparison
            String title = m.getMoodTitle() != null
                    ? m.getMoodTitle().toUpperCase(Locale.ROOT)
                    : "";

            boolean moodMatch = false;
            if (isHappy     && "HAPPY".equals(title))      moodMatch = true;
            if (isSad       && "SAD".equals(title))        moodMatch = true;
            if (isAngry     && "ANGRY".equals(title))      moodMatch = true;
            if (isSurprised && "SURPRISED".equals(title))  moodMatch = true;
            if (isAfraid    && "AFRAID".equals(title))     moodMatch = true;
            if (isDisgusted && "DISGUSTED".equals(title))  moodMatch = true;
            if (isConfused  && "CONFUSED".equals(title))   moodMatch = true;
            if (isShameful  && "SHAMEFUL".equals(title))   moodMatch = true;

            // If no mood checkboxes are selected, then match all moods
            if (!isHappy && !isSad && !isAngry && !isSurprised &&
                    !isAfraid && !isDisgusted && !isConfused && !isShameful) {
                moodMatch = true;
            }

            if (moodMatch) {
                filtered.add(m);
            }
        }

        // Return the final filtered list via callback
        listener.onFilteredResults(filtered);
    }
}