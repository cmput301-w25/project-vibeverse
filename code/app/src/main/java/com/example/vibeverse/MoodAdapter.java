package com.example.vibeverse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class MoodAdapter extends BaseAdapter {

    private Context context;
    private List<String> moodList;
    private OnMoodActionListener moodActionListener;

    public interface OnMoodActionListener {
        void onMoodDelete(int position);
        void onMoodEdit(int position);  // Added method for editing
    }

    public MoodAdapter(Context context, List<String> moodList, OnMoodActionListener moodActionListener) {
        this.context = context;
        this.moodList = moodList;
        this.moodActionListener = moodActionListener;
    }

    @Override
    public int getCount() {
        return moodList.size();
    }

    @Override
    public Object getItem(int position) {
        return moodList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_mood, parent, false);
        }

        TextView moodTextView = convertView.findViewById(R.id.moodTextView);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        moodTextView.setText(moodList.get(position));

        // Handle delete button
        deleteButton.setOnClickListener(v -> moodActionListener.onMoodDelete(position));

        // Allow editing when clicking anywhere *except* the delete button
        convertView.setOnClickListener(v -> moodActionListener.onMoodEdit(position));

        return convertView;
    }
}
