package com.example.vibeverse;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MoodEventViewHolder extends RecyclerView.ViewHolder {
    ImageView imagePost;
    TextView textTitle, textSubtitle;
    ImageButton buttonPostMenu;

    public MoodEventViewHolder(@NonNull View itemView) {
        super(itemView);
        imagePost = itemView.findViewById(R.id.imagePost);
        textTitle = itemView.findViewById(R.id.textTitle);
        textSubtitle = itemView.findViewById(R.id.textSubtitle);
        buttonPostMenu = itemView.findViewById(R.id.buttonPostMenu);
    }
}