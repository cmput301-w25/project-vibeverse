package com.example.vibeverse;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BottomNavigationView bottomNavigationView;
    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Set up bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        NavigationHelper.setupBottomNavigation(this, bottomNavigationView);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "Map fragment is null");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add a default marker and move the camera
        LatLng defaultLocation = new LatLng(53.5461, -113.4938); // Edmonton coordinates
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Edmonton"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
    }
}