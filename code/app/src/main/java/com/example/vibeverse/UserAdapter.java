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

    // Interface for click events
    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    // Constructor
    public UserAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

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

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ViewHolder class
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView imageProfile;
        TextView textUsername;


        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            imageProfile = itemView.findViewById(R.id.imageProfile);
            textUsername = itemView.findViewById(R.id.textUsername);

        }
    }
}