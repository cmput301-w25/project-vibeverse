package com.example.vibeverse;

import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

/**
 * ViewHolder for displaying a mood event in a RecyclerView.
 * <p>
 * This ViewHolder holds references to the UI components used to display a mood event,
 * including post images, titles, subtitles, mood emojis, and various UI containers.
 * It is used by the MoodEventAdapter to efficiently manage and recycle views.
 * </p>
 */
public class MoodEventViewHolder extends RecyclerView.ViewHolder {

    // Original views
    public ImageView imagePost;
    public TextView textTitle;
    public TextView textSubtitle;
    public ImageButton buttonPostMenu;

    // Enhanced views
    public View moodColorStrip;
    // Replace the old TextView with an ImageView for the emoji.
    public ImageView imageEmoji;
    public TextView triggerText;
    public TextView socialLabel;
    public TextView socialText;
    public ProgressBar intensityProgressBar;
    public TextView intensityLabel;

    // Premium UI containers
    public CardView emojiContainer;
    public CardView intensityContainer;
    public CardView contentContainer;
    public CardView imageContainer;
    public LinearLayout socialContainer;

    public MaterialButton buttonLike;
    public ImageView imageProfile;
    public TextView textUsername;

    /**
     * Constructs a new MoodEventViewHolder.
     *
     * @param itemView The view representing a single mood event item.
     */
    public MoodEventViewHolder(@NonNull View itemView) {
        super(itemView);
        imagePost = itemView.findViewById(R.id.imagePost);
        textTitle = itemView.findViewById(R.id.textTitle);
        textSubtitle = itemView.findViewById(R.id.textSubtitle);
        buttonPostMenu = itemView.findViewById(R.id.buttonPostMenu);

        moodColorStrip = itemView.findViewById(R.id.moodColorStrip);
        // Initialize imageEmoji instead of textEmoji.
        imageEmoji = itemView.findViewById(R.id.imageEmoji);
        socialLabel = itemView.findViewById(R.id.socialLabel);
        socialText = itemView.findViewById(R.id.socialText);
        intensityProgressBar = itemView.findViewById(R.id.intensityProgressBar);
        intensityLabel = itemView.findViewById(R.id.intensityLabel);

        emojiContainer = itemView.findViewById(R.id.emojiContainer);
        intensityContainer = itemView.findViewById(R.id.intensityContainer);
        contentContainer = itemView.findViewById(R.id.contentContainer);
        imageContainer = itemView.findViewById(R.id.imageContainer);
        socialContainer = itemView.findViewById(R.id.socialContainer);
        imageProfile = itemView.findViewById(R.id.imageProfile);
        textUsername = itemView.findViewById(R.id.textUsername);
    }
}
