package com.example.vibeverse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    /**
     * Interface for handling click events on a user item.
     */
    public interface OnUserClickListener {
        /**
         * Called when a user item is clicked.
         *
         * @param user The User object that was clicked.
         */
        void onUserClick(User user);
    }

    /**
     * Constructs a new UserAdapter.
     *
     * @param userList The list of User objects to display.
     * @param listener The listener for handling user click events.
     */
    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    /**
     * Creates a new UserViewHolder by inflating the user item layout.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new instance of UserViewHolder.
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    /**
     * Binds data from a User object to the UserViewHolder.
     *
     * @param holder The UserViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Set username
        holder.textUsername.setText(user.getUsername());

        // Load profile picture using Glide if available
        if (user.isHasProfilePic() && user.getProfilePicUri() != null && !user.getProfilePicUri().isEmpty()) {
            Glide.with(holder.imageProfile.getContext())
                    .load(user.getProfilePicUri())
                    .placeholder(R.drawable.user_icon)
                    .error(R.drawable.user_icon)
                    .circleCrop()
                    .into(holder.imageProfile);
        } else {
            // If no profile pic, show default
            holder.imageProfile.setImageResource(R.drawable.user_icon);
        }

        // Set click listener for the entire item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onUserClick(user);
            }
        });

    }

    /**
     * Returns the total number of user items.
     *
     * @return The size of the user list.
     */
    @Override
    public int getItemCount() {
        return userList.size();
    }

    /**
     * UserViewHolder holds the view elements for a single user item.
     */
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProfile;
        TextView textUsername;

        /**
         * Constructs a new UserViewHolder.
         *
         * @param itemView The view representing a single user item.
         */
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            textUsername = itemView.findViewById(R.id.textUsername);

        }
    }
}