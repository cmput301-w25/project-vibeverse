package com.example.vibeverse;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * SadnessCuresActivity displays a page for "cures for sadness" with a dynamic blue gradient
 * background, a motivational quote, and a bullet list of static suggestions.
 * It also provides a "Back" button to exit and a "More Tips?" button that shows additional suggestions in a custom-styled dialog.
 */
public class SadnessCuresActivity extends AppCompatActivity {

    private ScrollView rootScroll;
    private Button btnBack, btnMoreTips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sadness_cures);

        // Initialize UI elements from XML
        rootScroll  = findViewById(R.id.rootScroll);
        btnBack     = findViewById(R.id.btnBack);
        btnMoreTips = findViewById(R.id.btnMoreTips);

        // Set the gradient background using #42A5F5 as the base color, blended with black and white.
        int baseColor   = Color.parseColor("#42A5F5");
        int darkerBlue  = blendColors(baseColor, Color.BLACK, 0.2f);
        int lighterBlue = blendColors(baseColor, Color.WHITE, 0.2f);

        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] { darkerBlue, lighterBlue }
        );
        gradient.setCornerRadius(0f);
        rootScroll.setBackground(gradient);

        // Style the buttons: black background with white text.
        btnBack.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        btnBack.setTextColor(Color.WHITE);

        btnMoreTips.setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
        btnMoreTips.setTextColor(Color.WHITE);

        // Set button listeners.
        btnBack.setOnClickListener(v -> finish());
        btnMoreTips.setOnClickListener(this::showMoreTipsDialog);
    }

    /**
     * Blends two colors by a specified ratio.
     *
     * @param color1 the first color
     * @param color2 the second color
     * @param ratio  the blending ratio; a ratio of 0 returns color2, a ratio of 1 returns color1
     * @return the blended color as an integer
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        int r = (int) (Color.red(color1) * ratio + Color.red(color2) * inverseRatio);
        int g = (int) (Color.green(color1) * ratio + Color.green(color2) * inverseRatio);
        int b = (int) (Color.blue(color1) * ratio + Color.blue(color2) * inverseRatio);
        return Color.rgb(r, g, b);
    }

    /**
     * Displays an AlertDialog with additional suggestions for coping with sadness.
     * The dialog uses a custom style (CustomAlertDialog) and forces the positive button's text color to blue.
     *
     * @param view the view that was clicked (unused)
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
