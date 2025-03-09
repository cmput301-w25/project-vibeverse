package com.example.vibeverse;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

/**
 * MoodAdapter is a custom adapter for displaying mood entries in a ListView.
 * <p>
 * Each mood entry is represented as a String. This adapter provides functionality
 * to display the list of moods and also offers callbacks for editing or deleting a mood
 * entry via the {@link OnMoodActionListener} interface.
 * </p>
 */
public class MoodAdapter extends BaseAdapter {

    private Context context;
    private List<String> moodList;
    private OnMoodActionListener moodActionListener;

    /**
     * Interface for handling actions on mood entries.
     */
    public interface OnMoodActionListener {
        /**
         * Called when a mood entry is requested to be deleted.
         *
         * @param position The position of the mood entry to delete.
         */
        void onMoodDelete(int position);

        /**
         * Called when a mood entry is requested to be edited.
         *
         * @param position The position of the mood entry to edit.
         */
        void onMoodEdit(int position);
    }

    /**
     * Constructs a new MoodAdapter.
     *
     * @param context            The context in which the adapter is operating.
     * @param moodList           The list of mood entries to display.
     * @param moodActionListener The listener to handle edit and delete actions.
     */
    public MoodAdapter(Context context, List<String> moodList, OnMoodActionListener moodActionListener) {
        this.context = context;
        this.moodList = moodList;
        this.moodActionListener = moodActionListener;
    }

    /**
     * Returns the number of mood entries in the list.
     *
     * @return The number of mood entries.
     */
    @Override
    public int getCount() {
        return moodList.size();
    }

    /**
     * Returns the mood entry at the specified position.
     *
     * @param position The position of the item.
     * @return The mood entry as a String.
     */
    @Override
    public Object getItem(int position) {
        return moodList.get(position);
    }

    /**
     * Returns the item ID for the mood entry at the specified position.
     *
     * @param position The position of the item.
     * @return The position itself as the item ID.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Provides a view for an AdapterView (ListView).
     * <p>
     * The view is inflated from {@code R.layout.list_item_mood} and displays the mood entry.
     * It includes a delete button; clicking the delete button triggers the
     * {@link OnMoodActionListener#onMoodDelete(int)} callback, whereas clicking anywhere
     * else in the view triggers the {@link OnMoodActionListener#onMoodEdit(int)} callback.
     * </p>
     *
     * @param position    The position of the mood entry within the adapter's data set.
     * @param convertView The old view to reuse, if possible.
     * @param parent      The parent that this view will eventually be attached to.
     * @return A view corresponding to the mood entry at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_mood, parent, false);
        }

        TextView moodTextView = convertView.findViewById(R.id.moodTextView);
        Button deleteButton = convertView.findViewById(R.id.deleteButton);

        moodTextView.setText(moodList.get(position));

        // Handle delete button click
        deleteButton.setOnClickListener(v -> moodActionListener.onMoodDelete(position));

        // Allow editing when clicking anywhere except the delete button
        convertView.setOnClickListener(v -> moodActionListener.onMoodEdit(position));

        return convertView;
    }
}