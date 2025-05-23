package com.example.vibeverse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

/**
 * Adapter for displaying a list of achievements in a RecyclerView.
 */
public class AchievementAdapter extends RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder> {

    private Context context;
    private List<Achievement> achievementList;
    private String userId;

    /**
     * Constructs an AchievementAdapter.
     *
     * @param context         the context in which the adapter is used.
     * @param achievementList the list of achievements to display.
     */
    public AchievementAdapter(Context context, List<Achievement> achievementList) {
        this.context = context;
        this.achievementList = achievementList;
        this.userId = FirebaseAuth.getInstance().getUid();
    }

    /**
     * Called when RecyclerView needs a new {@link AchievementViewHolder} of the given type to represent an item.
     *
     * @param parent   the parent ViewGroup.
     * @param viewType the view type of the new View.
     * @return a new AchievementViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public AchievementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_achievement, parent, false);
        return new AchievementViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     *
     * @param holder   the AchievementViewHolder which should be updated to represent the contents of the item.
     * @param position the position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull AchievementViewHolder holder, int position) {
        Achievement achievement = achievementList.get(position);

        // Reset view states (in case recycled)
        holder.container.setAlpha(1f);
        holder.claimButton.setVisibility(View.GONE);
        holder.completedTextView.setVisibility(View.GONE);
        holder.progressBar.setVisibility(View.VISIBLE);
        holder.progressTextView.setVisibility(View.VISIBLE);

        // Set static achievement details
        holder.nameTextView.setText(achievement.getName());
        holder.descriptionTextView.setText(achievement.getDescription());
        holder.xpTextView.setText(achievement.getCompletion_xp() + " XP");

        // Load and set the emblem image resource (assuming iconRes contains drawable resource name)
        int emblemResId = context.getResources().getIdentifier(achievement.getIconRes(), "drawable", context.getPackageName());
        if (emblemResId != 0) {
            holder.emblemImageView.setImageResource(emblemResId);
        }

        // Determine the tier theme color for the emblem overlay
        int tierColor;
        switch (achievement.getTier()) {
            case 1:
                tierColor = context.getResources().getColor(R.color.bronze);
                break;
            case 2:
                tierColor = context.getResources().getColor(R.color.silver);
                break;
            case 3:
                tierColor = context.getResources().getColor(R.color.gold);
                break;
            default:
                tierColor = context.getResources().getColor(android.R.color.darker_gray);
                break;
        }
        // Apply the tier overlay only to the emblem image
        holder.emblemImageView.setColorFilter(tierColor);

        // Set the progress bar to use the overall blue color
        int blueColor = context.getResources().getColor(android.R.color.holo_blue_light);
        holder.progressBar.setProgressTintList(ColorStateList.valueOf(blueColor));

        // Query Firestore for user's achievement progress data.
        if (userId != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                    .collection("achievements")
                    .document(achievement.getId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        int progress = 0;
                        if (documentSnapshot.exists() && documentSnapshot.getLong("progress") != null) {
                            progress = documentSnapshot.getLong("progress").intValue();
                        }
                        // Set progress values (if not complete)
                        if (progress < achievement.getTotal()) {
                            holder.progressBar.setMax(achievement.getTotal());
                            holder.progressBar.setProgress(progress);
                            holder.progressTextView.setText(progress + " / " + achievement.getTotal());
                            // Ensure claim button and completed text remain hidden
                            holder.claimButton.setVisibility(View.GONE);
                            holder.completedTextView.setVisibility(View.GONE);
                        } else {
                            // Achievement progress is complete.
                            // Check the achievement status in the document
                            String status = documentSnapshot.getString("completion_status");
                            if ("unclaimed".equals(status)) {
                                // Show claim button
                                holder.claimButton.setVisibility(View.VISIBLE);
                                // Hide progress bar and text
                                holder.progressBar.setVisibility(View.GONE);
                                holder.progressTextView.setVisibility(View.GONE);
                                holder.completedTextView.setVisibility(View.GONE);

                                // Set click listener on the claim button
                                holder.claimButton.setOnClickListener(v -> {
                                    // Update achievement status to "complete"
                                    FirebaseFirestore.getInstance()
                                            .collection("users")
                                            .document(userId)
                                            .collection("achievements")
                                            .document(achievement.getId())
                                            .update("completion_status", "complete")
                                            .addOnSuccessListener(aVoid -> {
                                                // Increment user's total XP using FieldValue.increment
                                                FirebaseFirestore.getInstance()
                                                        .collection("users")
                                                        .document(userId)
                                                        .update("totalXP", FieldValue.increment(achievement.getCompletion_xp()))
                                                        .addOnSuccessListener(aVoid2 -> {
                                                            // Update UI: hide claim button, show completed text, gray out container
                                                            holder.claimButton.setVisibility(View.GONE);
                                                            holder.completedTextView.setVisibility(View.VISIBLE);
                                                            holder.progressBar.setVisibility(View.GONE);
                                                            holder.progressTextView.setVisibility(View.GONE);
                                                            holder.container.setAlpha(0.5f);
                                                            if (context instanceof AchievementActivity) {
                                                                ((AchievementActivity) context).animateXpGain(holder.claimButton, achievement.getCompletion_xp());
                                                            }

                                                            if (achievement.getUnlocks() != null && !achievement.getUnlocks().equals("N/A")) {
                                                                String themeToUnlock = achievement.getUnlocks();

                                                                // Remove theme from lockedThemes
                                                                FirebaseFirestore.getInstance()
                                                                        .collection("users")
                                                                        .document(userId)
                                                                        .collection("lockedThemes")
                                                                        .document("list")
                                                                        .update("themeNames", FieldValue.arrayRemove(themeToUnlock))
                                                                        .addOnSuccessListener(aVoid3 -> {
                                                                            // Add theme to unlockedThemes
                                                                            FirebaseFirestore.getInstance()
                                                                                    .collection("users")
                                                                                    .document(userId)
                                                                                    .collection("unlockedThemes")
                                                                                    .document("list")
                                                                                    .update("themeNames", FieldValue.arrayUnion(themeToUnlock))
                                                                                    .addOnSuccessListener(aVoid4 -> {
                                                                                        // Show the unlocked theme popup.
                                                                                        if (context instanceof AchievementActivity) {
                                                                                            AchievementActivity achievementActivity = (AchievementActivity) context;
                                                                                            // Load themes from assets if not already loaded
                                                                                            if (achievementActivity.themeList == null) {
                                                                                                achievementActivity.themeList = achievementActivity.loadThemesFromAssets();
                                                                                            }
                                                                                            ThemeData unlockedTheme = achievementActivity.findThemeData(themeToUnlock);
                                                                                            if (unlockedTheme != null) {
                                                                                                achievementActivity.showUnlockedThemePopup(unlockedTheme);
                                                                                            }
                                                                                        }
                                                                                    })
                                                                                    .addOnFailureListener(e -> {
                                                                                        // Handle error adding theme to unlockedThemes if needed.
                                                                                    });
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            // Handle error removing theme from lockedThemes if needed.
                                                                        });
                                                            }
                                                        });
                                            });
                                });
                            } else {
                                // If status is already "complete", show the Completed text.
                                holder.claimButton.setVisibility(View.GONE);
                                holder.completedTextView.setVisibility(View.VISIBLE);
                                holder.progressBar.setVisibility(View.GONE);
                                holder.progressTextView.setVisibility(View.GONE);
                                holder.container.setAlpha(0.5f);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // On error, default to showing progress
                        holder.progressBar.setMax(achievement.getTotal());
                        holder.progressBar.setProgress(0);
                        holder.progressTextView.setText("0 / " + achievement.getTotal());
                        holder.claimButton.setVisibility(View.GONE);
                        holder.completedTextView.setVisibility(View.GONE);
                    });
        }
    }

    /**
     * Returns the total number of achievements.
     *
     * @return the size of the achievement list.
     */
    @Override
    public int getItemCount() {
        return achievementList.size();
    }

    /**
     * ViewHolder class for achievement items.
     */
    public static class AchievementViewHolder extends RecyclerView.ViewHolder {
        View container;
        ImageView emblemImageView;
        TextView nameTextView, descriptionTextView, progressTextView, xpTextView, completedTextView;
        ProgressBar progressBar;
        Button claimButton;

        /**
         * Constructs an AchievementViewHolder.
         *
         * @param itemView the view representing the achievement item.
         */
        public AchievementViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.achievementContainer);
            emblemImageView = itemView.findViewById(R.id.emblemImageView);
            nameTextView = itemView.findViewById(R.id.achievementNameTextView);
            descriptionTextView = itemView.findViewById(R.id.achievementDescriptionTextView);
            progressBar = itemView.findViewById(R.id.achievementProgressBar);
            progressTextView = itemView.findViewById(R.id.achievementProgressTextView);
            xpTextView = itemView.findViewById(R.id.achievementXpTextView);
            claimButton = itemView.findViewById(R.id.claimButton);
            completedTextView = itemView.findViewById(R.id.completedTextView);
        }
    }
}
