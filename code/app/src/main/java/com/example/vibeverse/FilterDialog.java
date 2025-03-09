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

    public interface FilterListener {
        // Updated method signature to include all 9 parameters
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

    public static void show(Context context, FilterListener listener) {
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

        RadioGroup radioGroupTime = dialog.findViewById(R.id.radioGroupTime);
        RadioButton radioLast24 = dialog.findViewById(R.id.radioLast24);
        RadioButton radioLast3Days = dialog.findViewById(R.id.radioLast3Days);
        RadioButton radioLastWeek = dialog.findViewById(R.id.radioLastWeek);
        RadioButton radioLastMonth = dialog.findViewById(R.id.radioLastMonth);
        RadioButton radioAllTime = dialog.findViewById(R.id.radioAllTime);

        CheckBox checkHappy     = dialog.findViewById(R.id.checkHappy);
        CheckBox checkSad       = dialog.findViewById(R.id.checkSad);
        CheckBox checkAngry     = dialog.findViewById(R.id.checkAngry);
        CheckBox checkSurprised = dialog.findViewById(R.id.checkSurprised);
        CheckBox checkAfraid    = dialog.findViewById(R.id.checkAfraid);
        CheckBox checkDisgusted = dialog.findViewById(R.id.checkDisgusted);
        CheckBox checkConfused  = dialog.findViewById(R.id.checkConfused);
        CheckBox checkShameful  = dialog.findViewById(R.id.checkShameful);

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

            // Call onFilterApplied (if you need local logic in the activity)
            listener.onFilterApplied(
                    timeFilter,
                    isHappy, isSad, isAngry, isSurprised,
                    isAfraid, isDisgusted, isConfused, isShameful
            );

            // Perform the Firestore fetch + filter, then return results
            fetchAndFilterMoods(
                    context, listener, timeFilter,
                    isHappy, isSad, isAngry, isSurprised,
                    isAfraid, isDisgusted, isConfused, isShameful
            );

            dialog.dismiss();
        });

        dialog.show();
    }

    private static void fetchAndFilterMoods(
            Context context,
            FilterListener listener,
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
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(context, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<MoodEvent> filtered = new ArrayList<>();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        MoodEvent m = MoodEvent.fromMap(doc.getData());
                        if (m.getTimestamp() == null) continue;

                        Date date;
                        try {
                            date = sdf.parse(m.getTimestamp());
                        } catch (ParseException e) {
                            Log.e("FilterDialog", "Timestamp parse error", e);
                            continue;
                        }
                        m.setDate(date);
                        long moodTime = date.getTime();

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

                        // Mood filter check
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

                        // If no checkboxes are selected, show all
                        if (!isHappy && !isSad && !isAngry && !isSurprised &&
                                !isAfraid && !isDisgusted && !isConfused && !isShameful) {
                            moodMatch = true;
                        }

                        if (moodMatch) {
                            filtered.add(m);
                        }
                    }

                    // Return final filtered list
                    listener.onFilteredResults(filtered);
                })
                .addOnFailureListener(e ->
                        Log.e("FilterDialog", "Error fetching moods: ", e)
                );
    }
}
