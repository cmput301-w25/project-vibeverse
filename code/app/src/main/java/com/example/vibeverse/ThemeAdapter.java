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
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Set;

public class ThemeAdapter extends RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder> {

    /**
     * List of ThemeData objects representing available themes.
     */
    private List<ThemeData> themeList;
    /**
     * Set of theme IDs that are locked.
     */
    private Set<String> lockedThemes;
    /**
     * FirebaseFirestore instance for database operations.
     */
    private FirebaseFirestore db;
    /**
     * The user ID for the current user.
     */
    private String userId;
    /**
     * The currently selected theme ID.
     */
    private String selectedTheme; // current selected theme

    /**
     * Constructs a new ThemeAdapter.
     *
     * @param themeList     List of ThemeData objects.
     * @param lockedThemes  Set of locked theme IDs.
     * @param db            FirebaseFirestore instance.
     * @param userId        The current user's ID.
     * @param selectedTheme The currently selected theme ID.
     */
    public ThemeAdapter(List<ThemeData> themeList, Set<String> lockedThemes, FirebaseFirestore db, String userId, String selectedTheme) {
        this.themeList = themeList;
        this.lockedThemes = lockedThemes;
        this.db = db;
        this.userId = userId;
        this.selectedTheme = selectedTheme;
    }

    /**
     * Optional method to update the selected theme and refresh the views.
     *
     * @param newSelectedTheme The new selected theme ID.
     */
    public void updateSelectedTheme(String newSelectedTheme) {
        this.selectedTheme = newSelectedTheme;
        notifyDataSetChanged();
    }

    /**
     * Called when RecyclerView needs a new ThemeViewHolder of the given type.
     *
     * @param parent   The parent ViewGroup.
     * @param viewType The view type.
     * @return A new ThemeViewHolder.
     */
    @NonNull
    @Override
    public ThemeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_theme, parent, false);
        return new ThemeViewHolder(v);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   The ThemeViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ThemeViewHolder holder, int position) {
        ThemeData themeData = themeList.get(position);
        Context context = holder.itemView.getContext();

        // Use the bundleTitle for display
        holder.themeTitle.setText(themeData.getBundleTitle());

        // Set the background image using naming convention
        int bgResId = context.getResources().getIdentifier(themeData.getBackgroundRes(), "drawable", context.getPackageName());
        holder.backgroundImage.setImageResource(bgResId);

        // Retrieve the MaterialCardView (the root view)
        com.google.android.material.card.MaterialCardView cardView = holder.itemView.findViewById(R.id.cardView);

        if (lockedThemes.contains(themeData.getId())) {
            // For locked themes
            holder.lockedOverlay.setVisibility(View.VISIBLE);
            holder.selectButton.setVisibility(View.GONE); // no select button for locked themes
            cardView.setStrokeWidth(0);
        } else {
            // For unlocked themes
            holder.lockedOverlay.setVisibility(View.GONE);
            holder.selectButton.setVisibility(View.VISIBLE);

            if (themeData.getId().equals(selectedTheme)) {
                holder.selectButton.setText("Selected");
                holder.selectButton.setBackgroundTintList(null);
                holder.selectButton.setBackgroundResource(R.drawable.button_background_selected);
                holder.selectButton.setEnabled(false);
                cardView.setStrokeColor(Color.parseColor("#2979FF")); // blue outline
                int strokeWidth = (int) (6 * context.getResources().getDisplayMetrics().density);
                cardView.setStrokeWidth(strokeWidth);
            } else {
                holder.selectButton.setText("Select");
                holder.selectButton.setBackgroundResource(R.drawable.button_background);
                holder.selectButton.setBackgroundTintList(null);
                holder.selectButton.setEnabled(true);
                cardView.setStrokeWidth(0);
                holder.selectButton.setOnClickListener(v -> {
                    db.collection("users").document(userId)
                            .update("selectedTheme", themeData.getId())
                            .addOnSuccessListener(aVoid -> {
                                updateSelectedTheme(themeData.getId());
                            })
                            .addOnFailureListener(e -> {
                                // Handle error if needed
                            });
                });
            }
        }

        // Always set the onClickListener so the popup shows for both locked and unlocked themes.
        holder.itemView.setOnClickListener(v -> {
            showThemePopup(context, themeData);
        });
    }

    /**
     * Returns the total number of themes.
     *
     * @return The size of the theme list.
     */
    @Override
    public int getItemCount() {
        return themeList.size();
    }

    /**
     * Displays a popup with theme details and selection options.
     *
     * @param context   The context.
     * @param themeData The ThemeData object for which details are to be shown.
     */
    private void showThemePopup(Context context, ThemeData themeData) {
        // Inflate the custom popup layout.
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_theme_details, null);

        TextView bundleTitle = popupView.findViewById(R.id.popupBundleTitle);
        GridLayout emojiGrid = popupView.findViewById(R.id.emojiGrid);
        TextView lockedMessage = popupView.findViewById(R.id.lockedMessage);
        Button selectButton = popupView.findViewById(R.id.selectThemeButton);

        // Set the bundle title.
        bundleTitle.setText(themeData.getBundleTitle());

        // Clear any existing views (if reusing).
        emojiGrid.removeAllViews();

        // For each mood in your Mood enum, create a container with the emoji.
        for (Mood mood : Mood.values()) {
            // Create a container for the emoji.
            FrameLayout emojiContainer = new FrameLayout(context);
            GridLayout.LayoutParams containerParams = new GridLayout.LayoutParams();
            int containerSize = (int) (70 * context.getResources().getDisplayMetrics().density);
            containerParams.width = containerSize;
            containerParams.height = containerSize;
            containerParams.setMargins(8, 8, 8, 8);
            emojiContainer.setLayoutParams(containerParams);
            emojiContainer.setBackgroundResource(R.drawable.emoji_container);

            // Create the ImageView for the emoji.
            ImageView emojiView = new ImageView(context);
            int resId = getEmojiResourceId(context, mood.getName().toLowerCase(), themeData.getId());
            emojiView.setImageResource(resId);
            // Set layout parameters for the emoji inside its container.
            FrameLayout.LayoutParams emojiParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            emojiView.setLayoutParams(emojiParams);
            emojiView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

            // Add the ImageView to the container.
            emojiContainer.addView(emojiView);
            // Add the container to the grid.
            emojiGrid.addView(emojiContainer);
        }

        // Determine if this theme is locked.
        boolean isLocked = lockedThemes.contains(themeData.getId());


        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
                .setView(popupView)
                .create();

        if (isLocked) {
            // For locked themes, show the "Unlocked by ..." text.
            lockedMessage.setText("Unlocked by " + themeData.getUnlockedBy());
            lockedMessage.setVisibility(View.VISIBLE);
            selectButton.setVisibility(View.GONE);
        } else {
            // For unlocked themes
            selectButton.setVisibility(View.VISIBLE);
            lockedMessage.setVisibility(View.GONE);

            if (themeData.getId().equals(selectedTheme)) {
                selectButton.setText("Selected");
                selectButton.setEnabled(false);
                selectButton.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY)); // grey tint for selected
            } else {
                selectButton.setText("Select");
                selectButton.setEnabled(true);
                selectButton.setBackgroundTintList(null);
                selectButton.setOnClickListener(v -> {
                    // Update the Firestore selected theme
                    db.collection("users").document(userId)
                            .update("selectedTheme", themeData.getId())
                            .addOnSuccessListener(aVoid -> {
                                updateSelectedTheme(themeData.getId());
                            })
                            .addOnFailureListener(e -> {
                                // Handle error if needed.
                            });
                    // Dismiss the dialog
                    dialog.dismiss();
                });
            }

        dialog.show();

            return; // Exit early so that we don’t show the dialog twice.
        }

        dialog.show();
    }

    /**
     * Retrieves the resource ID for an emoji based on the mood and theme.
     *
     * @param context The context.
     * @param moodId  The mood identifier.
     * @param theme   The theme identifier.
     * @return The resource ID of the emoji drawable.
     */
    private int getEmojiResourceId(Context context, String moodId, String theme) {
        String resourceName = "emoji_" + moodId.toLowerCase() + "_" + theme.toLowerCase();
        return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    }

    /**
     * ThemeViewHolder holds the view elements for a single theme item.
     */
    public static class ThemeViewHolder extends RecyclerView.ViewHolder {
        /**
         * ImageView for displaying the theme background image.
         */
        ImageView backgroundImage;
        /**
         * TextView for displaying the theme title.
         */
        TextView themeTitle;
        /**
         * FrameLayout that overlays a locked indicator.
         */
        FrameLayout lockedOverlay;
        /**
         * Button to select the theme.
         */
        Button selectButton;

        /**
         * Constructs a new ThemeViewHolder.
         *
         * @param itemView The view representing a single theme item.
         */
        public ThemeViewHolder(@NonNull View itemView) {
            super(itemView);
            backgroundImage = itemView.findViewById(R.id.backgroundImage);
            themeTitle = itemView.findViewById(R.id.themeTitle);
            lockedOverlay = itemView.findViewById(R.id.lockedOverlay);
            selectButton = itemView.findViewById(R.id.selectButton);
        }
    }
}
