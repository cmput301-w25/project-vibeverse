package com.example.vibeverse;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile); // Update if using another layout

        // Initialize Bottom Navigation
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Handle Navigation Item Clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                Toast.makeText(MainActivity.this, "Home Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_search) {
                Toast.makeText(MainActivity.this, "Search Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_add) {
                Toast.makeText(MainActivity.this, "Add Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_map) {
                Toast.makeText(MainActivity.this, "Map Clicked", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.nav_profile) {
                Toast.makeText(MainActivity.this, "Profile Clicked", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }
}
