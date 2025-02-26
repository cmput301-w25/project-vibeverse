package com.example.vibeverse;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private Button openUserDetailsButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the button
        openUserDetailsButton = findViewById(R.id.openUserDetailsButton);

        // Set OnClickListener to open ActivityUserDetails
        openUserDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, activity_user_details.class);
                startActivity(intent);
            }
        });
    }
}