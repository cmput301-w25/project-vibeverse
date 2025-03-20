package com.example.vibeverse;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CommentSectionActivity extends AppCompatActivity {

    private TextView textTitle, textSubtitle, textEmoji;
    private RecyclerView recyclerComments;
    private EditText editComment;
    private Button buttonSendComment;
    private ImageView imagePost; // Added for loading the image

    private View imageContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment_section);

        // Retrieve post data from the intent
        Intent intent = getIntent();
        String reasonWhy = intent.getStringExtra("reasonWhy");
        String moodTitle = intent.getStringExtra("moodTitle");
        String emoji = intent.getStringExtra("emoji");
        long timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis());
        String photoUri = intent.getStringExtra("photoUri"); // Get the image URI if available
        Boolean hasPhoto = intent.getBooleanExtra("hasPhoto", false);

        // Find views inside the included post layout
        View postView = findViewById(R.id.includedPost);
        textTitle = postView.findViewById(R.id.textTitle);
        textSubtitle = postView.findViewById(R.id.textSubtitle);
        textEmoji = postView.findViewById(R.id.textEmoji);
        imagePost = postView.findViewById(R.id.imagePost); // Image view for loading photo
        imageContainer = postView.findViewById(R.id.imageContainer);

        // Populate the post view with passed data
        textTitle.setText(reasonWhy);

        // Format the timestamp into a readable date string
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, yyyy • hh:mm:ss a", Locale.US);
        String dateString = formatter.format(new Date(timestamp));
        textSubtitle.setText(dateString + " • " + moodTitle);
        textEmoji.setText(emoji);

        // Load the image if a valid URI exists
        if (hasPhoto && photoUri != null && !photoUri.isEmpty()) {
            imageContainer.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(Uri.parse(photoUri))
                    .into(imagePost);
        } else {
            imageContainer.setVisibility(View.GONE);
        }

        // Setup the comment section
        recyclerComments = findViewById(R.id.recyclerComments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        // TODO: Initialize your CommentAdapter and set it to the recyclerComments
        // CommentAdapter commentAdapter = new CommentAdapter(...);
        // recyclerComments.setAdapter(commentAdapter);

        // Setup the send comment button
        editComment = findViewById(R.id.editComment);
        buttonSendComment = findViewById(R.id.buttonSendComment);
        buttonSendComment.setOnClickListener(v -> {
            String commentText = editComment.getText().toString().trim();
            if (!commentText.isEmpty()) {
                // TODO: Handle posting the comment (e.g., update your database and refresh the comments list)
                // For now, just clear the input field:
                editComment.setText("");
            }
        });
    }
}
