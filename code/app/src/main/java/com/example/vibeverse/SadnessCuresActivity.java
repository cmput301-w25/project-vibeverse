package com.example.vibeverse;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SadnessCuresActivity extends AppCompatActivity {

    private ScrollView rootScroll;
    private Button btnBack, btnMoreTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sadness_cures);

        // 1) Setup UI
        rootScroll  = findViewById(R.id.rootScroll);
        btnBack     = findViewById(R.id.btnBack);
        btnMoreTips = findViewById(R.id.btnMoreTips);

        // 2) Gradient background with #42A5F5 (blended darker -> lighter)
        int baseColor   = Color.parseColor("#42A5F5");
        int darkerBlue  = blendColors(baseColor, Color.BLACK, 0.2f);
        int lighterBlue = blendColors(baseColor, Color.WHITE, 0.2f);

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { darkerBlue, lighterBlue }
        );
        gradient.setCornerRadius(0f);
        rootScroll.setBackground(gradient);

        // 3) Style the buttons (optional: black background, white text)
        btnBack.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        btnBack.setTextColor(Color.WHITE);

        btnMoreTips.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        btnMoreTips.setTextColor(Color.WHITE);

        // 4) Button Listeners
        btnBack.setOnClickListener(v -> finish());
        btnMoreTips.setOnClickListener(this::showMoreTipsDialog);
    }

    /**
     * Blends two colors (ratio=0 => color2, ratio=1 => color1).
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        int r = (int) (Color.red(color1) * ratio + Color.red(color2) * inverseRatio);
        int g = (int) (Color.green(color1) * ratio + Color.green(color2) * inverseRatio);
        int b = (int) (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio);
        return Color.rgb(r, g, b);
    }

    /**
     * Show the "More Tips?" dialog with black background + white text,
     * same text as before, but styled like your second dialog.
     */
    private void showMoreTipsDialog(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Additional Suggestions")
                .setMessage("• Try free journaling apps\n" +
                        "• Explore guided meditations on YouTube\n" +
                        "• Hydrate & keep a balanced diet\n" +
                        "• Reach out to a mental health hotline if needed\n\n" +
                        "For immediate help in Canada, call 1.833.456.4566")
                .setPositiveButton("Got it", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            // Force the positive button's text color to blue (#33B5E5)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#33B5E5"));
        });
        dialog.show();
    }
}
