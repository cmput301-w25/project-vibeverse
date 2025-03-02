package com.example.vibeverse;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class FilterDialog {

    public interface FilterListener {
        void onFilterApplied(String timeFilter, boolean isHappy, boolean isSad, boolean isAfraid, boolean isConfused);
    }

    public static void show(Context context, FilterListener listener) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_filters);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }

        // Filter selection views
        RadioGroup radioGroupTime = dialog.findViewById(R.id.radioGroupTime);
        RadioButton radioLast24 = dialog.findViewById(R.id.radioLast24);
        RadioButton radioLast3Days = dialog.findViewById(R.id.radioLast3Days);
        RadioButton radioLastWeek = dialog.findViewById(R.id.radioLastWeek);
        RadioButton radioLastMonth = dialog.findViewById(R.id.radioLastMonth);
        CheckBox checkHappy = dialog.findViewById(R.id.checkHappy);
        CheckBox checkSad = dialog.findViewById(R.id.checkSad);
        CheckBox checkAfraid = dialog.findViewById(R.id.checkAfraid);
        CheckBox checkConfused = dialog.findViewById(R.id.checkConfused);
        Button buttonApplyFilters = dialog.findViewById(R.id.buttonApplyFilters);

        buttonApplyFilters.setOnClickListener(v -> {
            String timeFilter = "";
            int selectedId = radioGroupTime.getCheckedRadioButtonId();
            if (selectedId == radioLast24.getId()) {
                timeFilter = "last_24_hours";
            }
            else if (selectedId == radioLast3Days.getId()) {
                timeFilter = "3Days";
            }
            else if (selectedId == radioLastWeek.getId()) {
                timeFilter = "last_week";
            }
            else if (selectedId == radioLastMonth.getId()) {
                timeFilter = "last_month";
            }

            boolean isHappy = checkHappy.isChecked();
            boolean isSad = checkSad.isChecked();
            boolean isAfraid = checkAfraid.isChecked();
            boolean isConfused = checkConfused.isChecked();

            if (timeFilter.isEmpty() && !isHappy && !isSad && !isAfraid && !isConfused) {
                listener.onFilterApplied("all_time", true, true, true, true);
            }
            else if (timeFilter.isEmpty() && (isHappy || isSad || isAfraid || isConfused)) {
                timeFilter = "all_time";
                listener.onFilterApplied(timeFilter, isHappy, isSad, isAfraid, isConfused);
            }
            else {
                listener.onFilterApplied(timeFilter, isHappy, isSad, isAfraid, isConfused);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}
