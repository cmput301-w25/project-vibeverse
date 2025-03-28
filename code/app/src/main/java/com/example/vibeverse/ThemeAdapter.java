package com.example.vibeverse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Set;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {

    private List<String> themeList;
    private Set<String> lockedThemes;
    private FirebaseFirestore db;
    private String userId;
    private String selectedTheme; // current selected theme

    public ThemeAdapter(List<String> themeList, Set<String> lockedThemes, FirebaseFirestore db, String userId, String selectedTheme) {
        this.themeList = themeList;
        this.lockedThemes = lockedThemes;
        this.db = db;
        this.userId = userId;
        this.selectedTheme = selectedTheme;
    }

    // Optional method to update the selected theme and refresh views
    public void updateSelectedTheme(String newSelectedTheme) {
        this.selectedTheme = newSelectedTheme;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme, parent, false);
        return new ThemeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        String theme = themeList.get(position);
        Context context = holder.itemView.getContext();

        // Set the title text (capitalize first letter and add "Bundle")
        String title = theme.substring(0, 1).toUpperCase() + theme.substring(1) + " Bundle";
        holder.themeTitle.setText(title);

        // Set the background image using naming convention: "theme_" + theme
        int bgResId = getThemeBackgroundResId(context, theme);
        holder.backgroundImage.setImageResource(bgResId);

        // Retrieve the MaterialCardView (the root view)
        // Ensure that your item_theme.xml has the MaterialCardView with id "cardView"
        com.google.android.material.card.MaterialCardView cardView =
                (com.google.android.material.card.MaterialCardView) holder.itemView.findViewById(R.id.cardView);

        // Check if this theme is locked (if it appears in the locked set)
        if (lockedThemes.contains(theme)) {
            holder.lockedOverlay.setVisibility(View.VISIBLE);
            holder.selectButton.setVisibility(View.GONE); // no select button for locked themes
            // Optionally, remove any border if locked
            cardView.setStrokeWidth(0);
        } else {
            holder.lockedOverlay.setVisibility(View.GONE);
            holder.selectButton.setVisibility(View.VISIBLE);
            // If this is the currently selected theme, change button appearance and container border
            if (theme.equals(selectedTheme)) {
                holder.selectButton.setText("Selected");
                holder.selectButton.setBackgroundTintList(null);
                holder.selectButton.setBackgroundResource(R.drawable.button_background_selected);
                holder.selectButton.setEnabled(false);

                // Set a more pronounced blue outline for the selected container
                cardView.setStrokeColor(Color.parseColor("#2979FF")); // a vibrant blue
                int strokeWidth = (int) (6 * context.getResources().getDisplayMetrics().density); // 6dp in pixels
                cardView.setStrokeWidth(strokeWidth);
            } else {
                holder.selectButton.setText("Select");
                holder.selectButton.setBackgroundResource(R.drawable.button_background);
                holder.selectButton.setBackgroundTintList(null);
                holder.selectButton.setEnabled(true);
                // Remove any border for non-selected containers
                cardView.setStrokeWidth(0);
                // Add onClickListener as before...
                holder.selectButton.setOnClickListener(v -> {
                    db.collection("users").document(userId)
                            .update("selectedTheme", theme)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("ThemeAdapter", "Selected theme updated to " + theme);
                                updateSelectedTheme(theme);
                            })
                            .addOnFailureListener(e -> Log.e("ThemeAdapter", "Error updating selected theme", e));
                });
            }
        }
    }


    @Override
    public int getItemCount() {
        return themeList.size();
    }

    /**
     * Helper method that returns the drawable resource ID for a given theme string.
     */
    private int getThemeBackgroundResId(Context context, String theme) {
        String resourceName = "theme_" + theme.toLowerCase();
        return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    }

    public static class ThemeViewHolder extends RecyclerView.ViewHolder {
        ImageView backgroundImage;
        TextView themeTitle;
        FrameLayout lockedOverlay;
        Button selectButton;

        public ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            backgroundImage = itemView.findViewById(R.id.backgroundImage);
            themeTitle = itemView.findViewById(R.id.themeTitle);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
            selectButton = itemView.findViewById(R.id.selectButton);
        }
    }
}
