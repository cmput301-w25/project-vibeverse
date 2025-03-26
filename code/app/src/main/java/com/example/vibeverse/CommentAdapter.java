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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<Comment> commentList;
    private FirebaseFirestore db;
    private OnReplyClickListener replyClickListener;

    private String moodUserId;
    private String moodDocId;

    public interface OnReplyClickListener {
        void onReplyClicked(Comment comment);
    }

    public void setOnReplyClickListener(OnReplyClickListener listener) {
        this.replyClickListener = listener;
    }

    public CommentAdapter(Context context, List<Comment> commentList, String moodUserId, String moodDocId) {
        this.context = context;
        this.commentList = commentList;
        this.moodUserId = moodUserId;
        this.moodDocId = moodDocId;
        db = FirebaseFirestore.getInstance();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profilePic;
        TextView username;
        TextView dateTime;
        TextView commentContent;
        ImageView replyButton, deleteIcon;
        RecyclerView repliesRecycler;



        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.profilePic);
            username = itemView.findViewById(R.id.username);
            dateTime = itemView.findViewById(R.id.dateTime);
            commentContent = itemView.findViewById(R.id.commentContent);
            replyButton = itemView.findViewById(R.id.replyIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);
            repliesRecycler = itemView.findViewById(R.id.repliesRecycler);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentViewHolder holder, int position) {
        final Comment comment = commentList.get(position);

        // Set the parent comment content
        holder.commentContent.setText(comment.getContent());
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
        String dateStr = formatter.format(comment.getTimestamp());
        holder.dateTime.setText(dateStr);

        // Load author details for parent comment
        String authorUserId = comment.getAuthorUserId();
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

        // Set reply button click listener for parent comment
        holder.replyButton.setOnClickListener(v -> {
            if (replyClickListener != null) {
                replyClickListener.onReplyClicked(comment);
            }
        });

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
            // Determine if the comment is a parent comment or a reply.
            if (comment.getRepliesTo() == null || comment.getRepliesTo().equals("N/A")) {
                // It's a parent comment
                db.collection("Usermoods")
                        .document(moodUserId)
                        .collection("moods")
                        .document(moodDocId)
                        .collection("comments")
                        .document(comment.getCommentId())
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Comment deleted", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {

                        });
            }
        });

        // Now load replies for this comment into the nested RecyclerView
        holder.repliesRecycler.setLayoutManager(new LinearLayoutManager(context));
        List<Comment> repliesList = new ArrayList<>();
        ReplyAdapter replyAdapter = new ReplyAdapter(context, repliesList, moodUserId, moodDocId);
        holder.repliesRecycler.setAdapter(replyAdapter);
        // Initially hide replies RecyclerView until replies are found
        holder.repliesRecycler.setVisibility(View.GONE);

        // Query Firestore for replies for this comment
        db.collection("Usermoods")
                .document(moodUserId)
                .collection("moods")
                .document(moodDocId)
                .collection("comments")
                .document(comment.getCommentId())
                .collection("replies")
                .orderBy("timestamp")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null) {
                        // Optionally handle the error
                        return;
                    }
                    if (querySnapshot != null) {
                        repliesList.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Comment reply = doc.toObject(Comment.class);
                            if (reply != null) {
                                repliesList.add(reply);
                            }
                        }
                        replyAdapter.notifyDataSetChanged();
                        holder.repliesRecycler.setVisibility(repliesList.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                });

    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // Method to update the adapter's dataset
    public void updateComments(List<Comment> newComments) {
        this.commentList = newComments;
        notifyDataSetChanged();
    }
}
