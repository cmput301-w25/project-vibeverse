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

public class MoodEventViewHolder extends RecyclerView.ViewHolder {
    // Original views
    ImageView imagePost;
    TextView textTitle, textSubtitle;
    ImageButton buttonPostMenu;

    // Views for enhanced layout
    View moodColorStrip;
    TextView textEmoji;
    TextView triggerLabel, triggerText;
    TextView socialLabel, socialText;
    ProgressBar intensityProgressBar;
    TextView intensityLabel;

    // Premium UI containers
    CardView emojiContainer;
    CardView intensityContainer;
    CardView contentContainer;
    CardView imageContainer;
    LinearLayout triggerContainer;
    LinearLayout socialContainer;

    // Updated button type for Material Design
    MaterialButton buttonLike;

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

        // Material button
        buttonLike = itemView.findViewById(R.id.buttonLike);
    }
}