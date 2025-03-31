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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * MoodEventAdapter populates a RecyclerView with MoodEvent data.
 * <p>
 * It displays each MoodEvent with its emoji, trigger (or mood title if trigger is absent),
 * timestamp, mood color, intensity, and an optional image. An overflow menu allows the user
 * to edit or delete a MoodEvent. The adapter also supports filtering the list based on a query.
 * </p>
 */
public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventViewHolder> {

    private static final String TAG = "MoodEventAdapter";

    /** The list of MoodEvent objects currently displayed. */
    private List<MoodEvent> moodEventList;
    /** The complete list of MoodEvent objects for filtering. */
    private List<MoodEvent> originalList;
    /** The current filtered list used for display. */
    private List<MoodEvent> currentList;
    /** Formatter for displaying date/time in the item view. */
    private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
    /** The context in which the adapter is operating. */
    private final Context context;

    /** Map of mood titles to their associated colors. */
    private final Map<String, Integer> moodColors = new HashMap<>();
    /** Flag to show or hide the menu button in the mood event items. */
    private boolean showMenuButton = true;
    private boolean showProfileInfo = false;

    private String selectedTheme;

    /**
     * Constructs a new MoodEventAdapter.
     *
     * @param context       The context in which the adapter is used.
     * @param moodEventList The list of MoodEvent objects to display.
     */
    public MoodEventAdapter(Context context, List<MoodEvent> moodEventList) {
        this.context = context;
        this.moodEventList = new ArrayList<>(moodEventList);
        this.originalList = new ArrayList<>(moodEventList);
        this.currentList = new ArrayList<>(moodEventList);
        initializeMoodColors();
        fetchUserTheme();
    }
    /**
     * Queries Firestore for the current user's selectedTheme and updates the adapter.
     */
    private void fetchUserTheme() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = (auth.getCurrentUser() != null) ? auth.getCurrentUser().getUid() : null;
        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists() && documentSnapshot.contains("selectedTheme")) {
                            selectedTheme = documentSnapshot.getString("selectedTheme");
                        } else {
                            selectedTheme = "default";
                        }
                        // Refresh the list so that emojis are reloaded based on the theme.
                        notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> {
                        selectedTheme = "default";
                        notifyDataSetChanged();
                    });
        } else {
            // No logged in user; keep default theme.
            selectedTheme = "default";
        }
    }

    /**
     * Initializes the moodColors map with pre-defined color values for each mood.
     */
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

    /**
     * Updates the adapter's dataset with a new list of MoodEvent objects.
     *
     * @param newMoodEvents The new list of MoodEvent objects.
     */
    public void updateMoodEvents(List<MoodEvent> newMoodEvents) {
        moodEventList.clear();
        moodEventList.addAll(newMoodEvents);
        originalList = new ArrayList<>(newMoodEvents);
        currentList = new ArrayList<>(newMoodEvents);
        notifyDataSetChanged();
    }
    /**
     * Sets the visibility of the menu button in the mood event items.
     *
     * @param shouldShow True to show the menu button, false to hide it.
     */
    public void setMenuButtonVisibility(boolean shouldShow) {
        this.showMenuButton = shouldShow;
        notifyDataSetChanged(); // Refresh all items
    }

    /**
     * Sets the visibility of the profile picture and username.
     *
     * @param shouldShow True to show the profile info, false to hide it.
     */
    public void setProfileVisibility(boolean shouldShow) {
        this.showProfileInfo = shouldShow;
        notifyDataSetChanged(); // Refresh all items
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

        // Instead of setting the emoji as text, load the appropriate PNG based on the theme.
        // Assume that MoodEventViewHolder now has an ImageView called imageEmoji.
        int emojiResId = getEmojiResourceId(moodEvent.getMoodTitle(), selectedTheme);
        if (holder.imageEmoji != null) {
            holder.imageEmoji.setImageResource(emojiResId);
        }

        // Set the menu button visibility.
        holder.buttonPostMenu.setVisibility(showMenuButton ? View.VISIBLE : View.GONE);

        // Handle profile picture and username visibility
        if (holder.imageProfile != null) {
            holder.imageProfile.setVisibility(showProfileInfo ? View.VISIBLE : View.GONE);
        }
        if (holder.textUsername != null) {
            holder.textUsername.setVisibility(showProfileInfo ? View.VISIBLE : View.GONE);
        }



        // Use the trigger as the title if it exists; otherwise, use the mood title.
        String reasonWhy = moodEvent.getReasonWhy();
        if (reasonWhy != null && !reasonWhy.trim().isEmpty()) {
            holder.textTitle.setText(reasonWhy);
        }

        // Set the subtitle with formatted date and mood title.
        holder.textSubtitle.setText(formatter.format(moodEvent.getDate()) + " • " + moodEvent.getMoodTitle());

        // Retrieve the color associated with the mood.
        int moodColor = getMoodColor(moodEvent.getMoodTitle());

        // Set the mood color for the top strip if available.
        if (holder.moodColorStrip != null) {
            holder.moodColorStrip.setBackgroundColor(moodColor);
        }

        // Tint the emoji container with a lighter version of the mood color.
        if (holder.emojiContainer != null) {
            int lighterMoodColor = lightenColor(moodColor, 0.7f);
            holder.emojiContainer.setCardBackgroundColor(lighterMoodColor);
        }

        // Set the intensity progress bar.
        if (holder.intensityProgressBar != null) {
            holder.intensityProgressBar.setProgress(moodEvent.getIntensity());
            holder.intensityProgressBar.setProgressTintList(ColorStateList.valueOf(moodColor));
        }

        // Handle the visibility and content of the trigger container.
        String socialSituation = moodEvent.getSocialSituation();


        // Handle the social situation container.
        if (holder.socialContainer != null) {
            if (socialSituation != null && !socialSituation.trim().isEmpty()) {
                holder.socialContainer.setVisibility(View.VISIBLE);
                holder.socialText.setText(socialSituation);
            } else {
                holder.socialContainer.setVisibility(View.GONE);
            }
        } else {
            // Fallback for older layouts.
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

        if (showProfileInfo) {
            // Set username
            if (holder.textUsername != null && moodEvent.getUsername() != null) {
                holder.textUsername.setText(moodEvent.getUsername());
                holder.textUsername.setVisibility(View.VISIBLE);
            } else {
                holder.textUsername.setVisibility(View.GONE);
            }

            // Set profile picture
            if (holder.imageProfile != null) {
                holder.imageProfile.setVisibility(View.VISIBLE);
//                String profilePicUrl = moodEvent.getProfilePictureUrl();
//                Toast.makeText(context, profilePicUrl, Toast.LENGTH_SHORT).show();
//                if (moodEvent.getProfilePictureUrl() != null && !(moodEvent.getProfilePictureUrl().isEmpty())) {
//                    Toast.makeText(context, "123", Toast.LENGTH_SHORT).show();
                    Glide.with(context)
                            .load(moodEvent.getProfilePictureUrl())
                            .placeholder(R.drawable.user_icon) // fallback placeholder
                            .error(R.drawable.user_icon)       // error placeholder// Make the image circular
                            .into(holder.imageProfile);
//                }
            }
        } else {
            if (holder.textUsername != null) {
                holder.textUsername.setVisibility(View.GONE);
            }
            if (holder.imageProfile != null) {
                holder.imageProfile.setVisibility(View.GONE);
            }
        }

        // Hide the entire content container if both trigger and social situation are empty.
        if (holder.contentContainer != null) {
            if ((socialSituation == null || socialSituation.trim().isEmpty())) {
                holder.contentContainer.setVisibility(View.GONE);
            } else {
                holder.contentContainer.setVisibility(View.VISIBLE);
            }
        }

        // Handle photo display.
        Photograph photo = moodEvent.getPhotograph();
        String photoUri = moodEvent.getPhotoUri();
        if (photoUri != null) {
            Log.d(TAG, "Position " + position + " has photoUri: " + photoUri);
        }
        if (photo != null && photo.getBitmap() != null) {
            Log.d(TAG, "Position " + position + " has bitmap");
        }
        boolean hasPhoto = (photo != null && photo.getBitmap() != null) ||
                (photoUri != null && !photoUri.isEmpty() && !photoUri.equals("N/A"));

        // Set image visibility.
        holder.imagePost.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        if (holder.imageContainer != null) {
            holder.imageContainer.setVisibility(hasPhoto ? View.VISIBLE : View.GONE);
        }
        if (hasPhoto) {
            if (photo != null && photo.getBitmap() != null) {
                Log.d(TAG, "Setting bitmap directly for position " + position);
                holder.imagePost.setImageBitmap(photo.getBitmap());
            } else if (photoUri != null && !photoUri.isEmpty() && !photoUri.equals("N/A")) {
                Log.d(TAG, "Loading with Glide for position " + position + ": " + photoUri);
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
                    holder.imagePost.setImageResource(R.drawable.demo_image);
                }
            }
            // Log ImageView dimensions for debugging.
            holder.imagePost.post(() -> {
                Log.d(TAG, "imagePost dimensions at position " + position + ": " +
                        holder.imagePost.getWidth() + "x" + holder.imagePost.getHeight());
            });
        }

        // Ensure the like button is styled correctly.
        if (holder.buttonLike != null) {
            if (holder.buttonLike instanceof MaterialButton) {
                ((MaterialButton) holder.buttonLike).setBackgroundTintList(ColorStateList.valueOf(Color.BLACK));
            } else {
                holder.buttonLike.setBackgroundColor(Color.BLACK);
            }
        }

        // Set up click listener for the menu button.
        holder.buttonPostMenu.setOnClickListener(v -> {
            showPostMenu(v, position, moodEvent);
        });

        // Apply additional animations to enhance the user experience.
        addAnimations(holder);

        // Inside onBindViewHolder(...) in MoodEventAdapter:
        Button buttonComment = holder.itemView.findViewById(R.id.buttonComment);
        buttonComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentSectionActivity.class);
            // Pass the relevant post data using extras (adjust as needed)
            intent.putExtra("reasonWhy", moodEvent.getReasonWhy());
            intent.putExtra("moodTitle", moodEvent.getMoodTitle());
            intent.putExtra("emoji", moodEvent.getEmoji());
            intent.putExtra("timestamp", moodEvent.getTimestamp());
            intent.putExtra("photoUri", moodEvent.getPhotoUri());
            intent.putExtra("hasPhoto", hasPhoto);
            intent.putExtra("moodDocId", moodEvent.getDocumentId());
            intent.putExtra("moodOwnerId", moodEvent.getOwnerUserId());
            intent.putExtra("socialSituation", moodEvent.getSocialSituation());
            intent.putExtra("moodColor", moodColor);
            intent.putExtra("intensity", moodEvent.getIntensity());


            // ... add any additional fields you want to show in the post item view

            context.startActivity(intent);


        });
    }

    /**
     * Lightens a given color by blending it with white.
     *
     * @param color  The original color.
     * @param factor The factor by which to lighten (0.0 to 1.0).
     * @return The lightened color.
     */
    private int lightenColor(int color, float factor) {
        int red = (int) ((Color.red(color) * (1 - factor) + 255 * factor));
        int green = (int) ((Color.green(color) * (1 - factor) + 255 * factor));
        int blue = (int) ((Color.blue(color) * (1 - factor) + 255 * factor));
        return Color.rgb(red, green, blue);
    }

    /**
     * Adds subtle animations to the view holder to enhance UI responsiveness.
     *
     * @param holder The MoodEventViewHolder containing the views to animate.
     */
    private void addAnimations(MoodEventViewHolder holder) {
        if (holder.emojiContainer != null) {
            // Animate emoji container
            holder.emojiContainer.setAlpha(0.8f);
            holder.emojiContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start();

            // Animate content container if visible
            if (holder.contentContainer != null && holder.contentContainer.getVisibility() == View.VISIBLE) {
                holder.contentContainer.setAlpha(0f);
                holder.contentContainer.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(200)
                        .start();
            }

            // Animate image container if visible
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

    /**
     * Returns the color associated with the given mood title.
     *
     * @param mood The mood title (e.g., "Happy").
     * @return The corresponding color, or gray if not found.
     */
    private int getMoodColor(String mood) {
        return moodColors.getOrDefault(mood, Color.GRAY);
    }

    /**
     * Displays a popup menu for a given mood event, allowing the user to edit or delete the event.
     *
     * @param view      The anchor view for the popup menu.
     * @param position  The position of the mood event in the adapter.
     * @param moodEvent The MoodEvent object.
     */
    private void showPostMenu(View view, int position, MoodEvent moodEvent) {
        Context wrapper = new ContextThemeWrapper(context, R.style.CustomPopupMenu);
        PopupMenu popup = new PopupMenu(wrapper, view);
        Menu menu = popup.getMenu();
        menu.add(0, 1, 0, "Edit");
        menu.add(0, 2, 0, "Delete");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) { // Edit
                Intent intent = new Intent(context, EditMoodActivity.class);
                intent.putExtra("selectedMood", moodEvent.getMoodTitle());
                intent.putExtra("selectedEmoji", moodEvent.getEmoji());
                intent.putExtra("reasonWhy", moodEvent.getReasonWhy());
                intent.putExtra("socialSituation", moodEvent.getSocialSituation());
                intent.putExtra("intensity", moodEvent.getIntensity());
                intent.putExtra("timestamp", moodEvent.getTimestamp());
                intent.putExtra("photoUri", moodEvent.getPhotoUri());
                intent.putExtra("photoDateTaken", moodEvent.getPhotoDate());
                intent.putExtra("photoLocation", moodEvent.getPhotoLocation());
                intent.putExtra("photoSizeKB", moodEvent.getPhotoSize());
                intent.putExtra("isPublic", moodEvent.isPublic());

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

    /**
     * Filters the list of MoodEvents based on a query string.
     * <p>
     * The query is matched against the trigger and subtitle of each MoodEvent.
     * </p>
     *
     * @param query The search query.
     */
    public void filter(String query) {
        query = query.toLowerCase().trim();
        currentList.clear();
        if (query.isEmpty()) {
            currentList.addAll(originalList);
        } else {
            for (MoodEvent moodEvent : originalList) {
                boolean titleMatches = moodEvent.getReasonWhy() != null && moodEvent.getReasonWhy().toLowerCase().contains(query);
                if (titleMatches) {
                    currentList.add(moodEvent);
                }
            }
        }
        moodEventList.clear();
        moodEventList.addAll(currentList);
        notifyDataSetChanged();
    }

    /**
     * Helper method to return the drawable resource ID for the emoji image.
     * Uses the naming convention: "emoji_<mood>_<theme>".
     */

    private int getEmojiResourceId(String moodId, String theme) {
        String resourceName = "emoji_" + moodId.toLowerCase() + "_" + theme.toLowerCase();
        return context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
    }
}