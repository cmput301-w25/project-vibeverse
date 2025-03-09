package com.example.vibeverse;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventViewHolder> {

    private static final String TAG = "MoodEventAdapter";
    private List<MoodEvent> moodEventList;
    private List<MoodEvent> originalList;
    private List<MoodEvent> currentList;
    private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
    private final Context context;

    // Map for mood colors
    private final Map<String, Integer> moodColors = new HashMap<>();

    public MoodEventAdapter(Context context, List<MoodEvent> moodEventList) {
        this.context = context;
        this.moodEventList = new ArrayList<>(moodEventList);
        this.originalList = new ArrayList<>(moodEventList);
        this.currentList = new ArrayList<>(moodEventList);

        // Initialize mood colors (matching those from SelectMoodActivity)
        initializeMoodColors();
    }

    private void initializeMoodColors() {
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink
    }

    public void updateMoodEvents(List<MoodEvent> newMoodEvents) {
        moodEventList.clear();
        moodEventList.addAll(newMoodEvents);
        originalList = new ArrayList<>(newMoodEvents);
        currentList = new ArrayList<>(newMoodEvents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MoodEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_post, parent, false);
        return new MoodEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MoodEventViewHolder holder, int position) {
        MoodEvent moodEvent = moodEventList.get(position);

        // Set emoji
        holder.textEmoji.setText(moodEvent.getEmoji());

        // Set the trigger as title if it exists, otherwise show the mood title
        String trigger = moodEvent.getTrigger();
        if (trigger != null && !trigger.trim().isEmpty()) {
            holder.textTitle.setText(trigger);
        } else {
            holder.textTitle.setText(moodEvent.getMoodTitle());
        }

        // Set the subtitle (date and mood)
        holder.textSubtitle.setText(formatter.format(moodEvent.getDate()) + " • " + moodEvent.getMoodTitle());

        // Get mood color
        int moodColor = getMoodColor(moodEvent.getMoodTitle());

        // Set mood color for the top strip
        if (holder.moodColorStrip != null) {
            holder.moodColorStrip.setBackgroundColor(moodColor);
        }

        // Tint emoji container background with a lighter version of the mood color
        if (holder.emojiContainer != null) {
            int lighterMoodColor = lightenColor(moodColor, 0.7f);
            holder.emojiContainer.setCardBackgroundColor(lighterMoodColor);
        }

        // Set intensity progress bar
        if (holder.intensityProgressBar != null) {
            holder.intensityProgressBar.setProgress(moodEvent.getIntensity());
            holder.intensityProgressBar.setProgressTintList(ColorStateList.valueOf(moodColor));
        }

        // Handle trigger container
        String socialSituation = moodEvent.getSocialSituation();

        if (holder.triggerContainer != null) {
            if (trigger != null && !trigger.trim().isEmpty()) {
                holder.triggerContainer.setVisibility(View.VISIBLE);
                holder.triggerText.setText(trigger);
            } else {
                holder.triggerContainer.setVisibility(View.GONE);
            }
        } else {
            // Fallback for older layout
            if (holder.triggerLabel != null && holder.triggerText != null) {
                if (trigger != null && !trigger.trim().isEmpty()) {
                    holder.triggerLabel.setVisibility(View.VISIBLE);
                    holder.triggerText.setVisibility(View.VISIBLE);
                    holder.triggerText.setText(trigger);
                } else {
                    holder.triggerLabel.setVisibility(View.GONE);
                    holder.triggerText.setVisibility(View.GONE);
                }
            }
        }

        // Handle social situation container
        if (holder.socialContainer != null) {
            if (socialSituation != null && !socialSituation.trim().isEmpty()) {
                holder.socialContainer.setVisibility(View.VISIBLE);
                holder.socialText.setText(socialSituation);
            } else {
                holder.socialContainer.setVisibility(View.GONE);
            }
        } else {
            // Fallback for older layout
            if (holder.socialLabel != null && holder.socialText != null) {
                if (socialSituation != null && !socialSituation.trim().isEmpty()) {
                    holder.socialLabel.setVisibility(View.VISIBLE);
                    holder.socialText.setVisibility(View.VISIBLE);
                    holder.socialText.setText(socialSituation);
                } else {
                    holder.socialLabel.setVisibility(View.GONE);
                    holder.socialText.setVisibility(View.GONE);
                }
            }
        }

        // Handle content container visibility - hide entire card if both empty
        if (holder.contentContainer != null) {
            if ((trigger == null || trigger.trim().isEmpty()) &&
                    (socialSituation == null || socialSituation.trim().isEmpty())) {
                holder.contentContainer.setVisibility(View.GONE);
            } else {
                holder.contentContainer.setVisibility(View.VISIBLE);
            }
        }

        // IMPROVED PHOTO HANDLING
        Photograph photo = moodEvent.getPhotograph();

        // Debug photo information
        String photoUri = moodEvent.getPhotoUri();
        if (photoUri != null) {
            Log.d(TAG, "Position " + position + " has photoUri: " + photoUri);
        }
        if (photo != null && photo.getBitmap() != null) {
            Log.d(TAG, "Position " + position + " has bitmap");
        }

        // Better check for photo existence
        boolean hasPhoto = (photo != null && photo.getBitmap() != null) ||
                (photoUri != null && !photoUri.isEmpty() && !photoUri.equals("N/A"));

        // Always set the imagePost visibility FIRST
        holder.imagePost.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);

        // Then handle the container visibility
        if (holder.imageContainer != null) {
            holder.imageContainer.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        }

        // Set the actual image content
        if (hasPhoto) {
            if (photo != null && photo.getBitmap() != null) {
                Log.d(TAG, "Setting bitmap directly for position " + position);
                holder.imagePost.setImageBitmap(photo.getBitmap());
            } else if (photoUri != null && !photoUri.isEmpty() && !photoUri.equals("N/A")) {
                Log.d(TAG, "Loading with Glide for position " + position + ": " + photoUri);
                // Enhanced Glide loading with error handling
                try {
                    RequestOptions requestOptions = new RequestOptions()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.demo_image);

                    Glide.with(context)
                            .load(Uri.parse(photoUri))
                            .apply(requestOptions)
                            .into(holder.imagePost);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image with Glide: " + e.getMessage());
                    // Fallback to a placeholder if available
                    holder.imagePost.setImageResource(R.drawable.demo_image);
                }
            }

            // Log the ImageView dimensions to diagnose layout issues
            holder.imagePost.post(() -> {
                Log.d(TAG, "imagePost dimensions at position " + position + ": " +
                        holder.imagePost.getWidth() + "x" + holder.imagePost.getHeight());
            });
        }

        // Make sure like button is black (support both Button and MaterialButton)
        if (holder.buttonLike != null) {
            if (holder.buttonLike instanceof MaterialButton) {
                ((MaterialButton) holder.buttonLike).setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                holder.buttonLike.setBackgroundColor(Color.BLACK);
            }
        }

        // Set click listener for the menu button
        holder.buttonPostMenu.setOnClickListener(v -> {
            showPostMenu(v, position, moodEvent);
        });

        // Add animations for premium feel
        addAnimations(holder);
    }

    // Helper method to lighten a color
    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }

    // Add subtle animations for a premium feel
    private void addAnimations(MoodEventViewHolder holder) {
        // Only animate if using the premium layout with containers
        if (holder.emojiContainer != null) {
            // Subtle scale animation on the emoji
            holder.emojiContainer.setAlpha(0.8f);
            holder.emojiContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();

            // Fade in animation on the content
            if (holder.contentContainer != null && holder.contentContainer.getVisibility() == View.VISIBLE) {
                holder.contentContainer.setAlpha(0f);
                holder.contentContainer.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(200)
                        .start();
            }

            // Subtle animation for the image if present
            if (holder.imageContainer != null && holder.imageContainer.getVisibility() == View.VISIBLE) {
                holder.imageContainer.setTranslationY(20f);
                holder.imageContainer.setAlpha(0f);
                holder.imageContainer.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(500)
                        .setStartDelay(300)
                        .start();
            }
        }
    }

    private int getMoodColor(String mood) {
        return moodColors.getOrDefault(mood, Color.GRAY);
    }

    private void showPostMenu(View view, int position, MoodEvent moodEvent) {
        PopupMenu popup = new PopupMenu(context, view);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, "Edit");
        menu.add(0, 2, 0, "Delete");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { // Edit
                Intent intent = new Intent(context, EditMoodActivity.class);
                intent.putExtra("selectedMood", moodEvent.getMoodTitle());
                intent.putExtra("selectedEmoji", moodEvent.getEmoji());
                intent.putExtra("trigger", moodEvent.getTrigger());
                intent.putExtra("reasonWhy", moodEvent.getReasonWhy());
                intent.putExtra("socialSituation", moodEvent.getSocialSituation());
                intent.putExtra("intensity", moodEvent.getIntensity());
                intent.putExtra("timestamp", moodEvent.getTimestamp());
                intent.putExtra("photoUri", moodEvent.getPhotoUri());
                intent.putExtra("photoDateTaken", moodEvent.getPhotoDate());
                intent.putExtra("photoLocation", moodEvent.getPhotoLocation());
                intent.putExtra("photoSizeKB", moodEvent.getPhotoSize());

                intent.putExtra("moodPosition", position);
                ((ProfilePage) context).startActivityForResult(intent, ProfilePage.EDIT_MOOD_REQUEST_CODE);
                return true;
            } else if (id == 2) { // Delete
                ((ProfilePage) context).deleteMoodFromFirestore(moodEvent.getDocumentId(), position);
                return true;
            }
            return false;
        });

        popup.show();
    }

    @Override
    public int getItemCount() {
        return moodEventList.size();
    }

    public void filter(String query) {
        query = query.toLowerCase().trim();
        currentList.clear();
        if (query.isEmpty()) {
            currentList.addAll(originalList);
        } else {
            for (MoodEvent moodEvent : originalList) {
                boolean titleMatches = moodEvent.getTrigger() != null && moodEvent.getTrigger().toLowerCase().contains(query);
                boolean subtitleMatches = moodEvent.getSubtitle() != null && moodEvent.getSubtitle().toLowerCase().contains(query);
                if (titleMatches || subtitleMatches) {
                    currentList.add(moodEvent);
                }
            }
        }
        moodEventList.clear();
        moodEventList.addAll(currentList);
        notifyDataSetChanged();
    }
}