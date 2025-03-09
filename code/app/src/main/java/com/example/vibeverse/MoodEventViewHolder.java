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
 * ViewHolder class for displaying a MoodEvent in a RecyclerView.
 * <p>
 * This class holds references to all the UI elements used in the layout for a single
 * MoodEvent item. It supports both the basic layout (image, title, subtitle, and menu button)
 * as well as an enhanced layout that includes additional views for mood color, emoji, trigger and
 * social information, intensity indicators, and a like button.
 * </p>
 */
public class MoodEventViewHolder extends RecyclerView.ViewHolder {

    // Original views
    /** ImageView displaying the mood event photo. */
    ImageView imagePost;
    /** TextView displaying the primary title (trigger or mood title). */
    TextView textTitle;
    /** TextView displaying the subtitle (formatted date and mood title). */
    TextView textSubtitle;
    /** ImageButton for opening the overflow menu for the mood event. */
    ImageButton buttonPostMenu;

    // Views for enhanced layout
    /** A view representing the color strip for the mood. */
    View moodColorStrip;
    /** TextView displaying the mood emoji. */
    TextView textEmoji;
    /** Label for the trigger text (if available). */
    TextView triggerLabel;
    /** TextView displaying the trigger of the mood event. */
    TextView triggerText;
    /** Label for the social situation text (if available). */
    TextView socialLabel;
    /** TextView displaying the social situation of the mood event. */
    TextView socialText;
    /** ProgressBar indicating the intensity of the mood. */
    ProgressBar intensityProgressBar;
    /** Label for intensity (if needed). */
    TextView intensityLabel;

    // Premium UI containers
    /** Container (CardView) for the emoji. */
    CardView emojiContainer;
    /** Container (CardView) for the intensity indicators. */
    CardView intensityContainer;
    /** Container (CardView) for the main content (excluding the emoji). */
    CardView contentContainer;
    /** Container (CardView) for displaying the image (if available). */
    CardView imageContainer;
    /** Layout container for the trigger information. */
    LinearLayout triggerContainer;
    /** Layout container for the social information. */
    LinearLayout socialContainer;

    // Updated button type for Material Design
    /** MaterialButton representing a "like" action. */
    MaterialButton buttonLike;

    /**
     * Constructs a new MoodEventViewHolder and initializes all view references.
     *
     * @param itemView The root view of the MoodEvent item layout.
     */
    public MoodEventViewHolder(@NonNull View itemView) {
        super(itemView);

        // Original views
        imagePost = itemView.findViewById(R.id.imagePost);
        textTitle = itemView.findViewById(R.id.textTitle);
        textSubtitle = itemView.findViewById(R.id.textSubtitle);
        buttonPostMenu = itemView.findViewById(R.id.buttonPostMenu);

        // Enhanced views
        moodColorStrip = itemView.findViewById(R.id.moodColorStrip);
        textEmoji = itemView.findViewById(R.id.textEmoji);
        triggerLabel = itemView.findViewById(R.id.triggerLabel);
        triggerText = itemView.findViewById(R.id.triggerText);
        socialLabel = itemView.findViewById(R.id.socialLabel);
        socialText = itemView.findViewById(R.id.socialText);
        intensityProgressBar = itemView.findViewById(R.id.intensityProgressBar);
        intensityLabel = itemView.findViewById(R.id.intensityLabel);

        // Premium UI containers
        emojiContainer = itemView.findViewById(R.id.emojiContainer);
        intensityContainer = itemView.findViewById(R.id.intensityContainer);
        contentContainer = itemView.findViewById(R.id.contentContainer);
        imageContainer = itemView.findViewById(R.id.imageContainer);
        triggerContainer = itemView.findViewById(R.id.triggerContainer);
        socialContainer = itemView.findViewById(R.id.socialContainer);

        // Material button for like action
        buttonLike = itemView.findViewById(R.id.buttonLike);
    }
}