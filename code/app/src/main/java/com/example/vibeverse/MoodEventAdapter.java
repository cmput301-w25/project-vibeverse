package com.example.vibeverse;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoodEventAdapter extends RecyclerView.Adapter<MoodEventViewHolder> {

    private  List<MoodEvent> moodEventList;
    private List<MoodEvent> originalList;
    private List<MoodEvent> currentList;
    private final SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
    private final Context context;

    public MoodEventAdapter(Context context, List<MoodEvent> moodEventList) {
        this.context = context;
        this.moodEventList = new ArrayList<>(moodEventList); // Initialize moodEventList
        this.originalList = new ArrayList<>(moodEventList);
        this.currentList = new ArrayList<>(moodEventList);
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
        holder.textTitle.setText(moodEvent.getTrigger());
        holder.textSubtitle.setText(formatter.format(moodEvent.getDate()) + " • " + moodEvent.getMoodTitle());
        Photograph photo = moodEvent.getPhotograph();
        if (photo != null) {
            holder.imagePost.setImageBitmap(photo.getBitmap());
        } else {
            holder.imagePost.setImageResource(R.drawable.demo_image);
        }

        // Set click listener for the menu button
        holder.buttonPostMenu.setOnClickListener(v -> {
            showPostMenu(v, position, moodEvent);
        });
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
                intent.putExtra("socialSituation", moodEvent.getSocialSituation());
                intent.putExtra("intensity", moodEvent.getIntensity());
                intent.putExtra("timestamp", moodEvent.getTimestamp());
                intent.putExtra("photoUri", moodEvent.getPhotoUri());
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