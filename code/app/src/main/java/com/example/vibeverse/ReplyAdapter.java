package com.example.vibeverse;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import de.hdodenhof.circleimageview.CircleImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReplyAdapter extends RecyclerView.Adapter<ReplyAdapter.ReplyViewHolder> {

    private Context context;
    private List<Comment> replyList;
    private FirebaseFirestore db;

    private String moodUserId;
    private String moodDocId;

    public ReplyAdapter(Context context, List<Comment> replyList, String moodUserId, String moodDocId) {
        this.context = context;
        this.replyList = replyList;
        this.moodUserId = moodUserId;
        this.moodDocId = moodDocId;
        db = FirebaseFirestore.getInstance();
    }

    public static class ReplyViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profilePic;
        TextView username;
        TextView dateTime;
        TextView commentContent;
        ImageView replyIcon;
        ImageView deleteIcon;
        public ReplyViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.profilePic);
            username = itemView.findViewById(R.id.username);
            dateTime = itemView.findViewById(R.id.dateTime);
            commentContent = itemView.findViewById(R.id.commentContent);
            replyIcon = itemView.findViewById(R.id.replyIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
        }
    }

    @NonNull
    @Override
    public ReplyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false);
        return new ReplyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReplyViewHolder holder, int position) {
        Comment reply = replyList.get(position);
        holder.commentContent.setText(reply.getContent());
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String dateStr = formatter.format(reply.getTimestamp());
        holder.dateTime.setText(dateStr);

        // Load author details from Firestore
        String authorUserId = reply.getAuthorUserId();
        db.collection("users").document(authorUserId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String username = documentSnapshot.getString("username");
                holder.username.setText(username != null ? username : "Unknown");
                Boolean hasProfilePic = documentSnapshot.getBoolean("hasProfilePic");
                String profilePicUri = documentSnapshot.getString("profilePicUri");
                if (hasProfilePic != null && hasProfilePic && profilePicUri != null && !profilePicUri.isEmpty()) {
                    Glide.with(context).load(profilePicUri).into(holder.profilePic);
                } else {
                    holder.profilePic.setImageResource(R.drawable.user_icon);
                }
            } else {
                holder.username.setText("Unknown");
                holder.profilePic.setImageResource(R.drawable.user_icon);
            }
        }).addOnFailureListener(e -> {
            holder.username.setText("Unknown");
            holder.profilePic.setImageResource(R.drawable.user_icon);
        });

        holder.replyIcon.setVisibility(View.GONE);

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId.equals(authorUserId)) {
            holder.deleteIcon.setVisibility(View.VISIBLE);
            holder.itemView.setOnClickListener(null);
        } else {
            holder.deleteIcon.setVisibility(View.GONE);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, UsersProfile.class);
                intent.putExtra("userId", authorUserId);
                context.startActivity(intent);
            });
        }

        holder.deleteIcon.setOnClickListener(v -> {
            // Check that this reply is actually a reply by verifying the 'repliesTo' field
            if (reply.getRepliesTo() != null && !reply.getRepliesTo().equals("N/A")) {
                // Delete the reply from its parent's replies subcollection
                String parentCommentId = reply.getRepliesTo();
                db.collection("Usermoods")
                        .document(moodUserId)
                        .collection("moods")
                        .document(moodDocId)
                        .collection("comments")
                        .document(parentCommentId)
                        .collection("replies")
                        .document(reply.getCommentId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Reply deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // Handle error
                        });
            }
        });



    }

    @Override
    public int getItemCount() {
        return replyList.size();
    }

    public void updateReplies(List<Comment> newReplies) {
        this.replyList = newReplies;
        notifyDataSetChanged();
    }
}
