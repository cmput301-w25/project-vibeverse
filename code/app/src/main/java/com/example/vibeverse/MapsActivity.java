package com.example.vibeverse;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // New UI elements
    private ToggleButton mapToggleButton;
    private SeekBar radiusSlider;
    private TextView radiusValueText;

    // Map mode and radius
    private boolean showFollowersMoods = false;
    private float currentRadiusKm = 5.0f;
    private static final float MIN_RADIUS_KM = 5.0f;
    private static final float MAX_RADIUS_KM = 100.0f;

    // Location-related fields
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LatLng currentUserLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Maps to store mood colors and emojis (copied from SelectMoodActivity)
    private final Map<String, Integer> moodColors = new HashMap<>();
    private final Map<String, String> moodEmojis = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize mood data
        initializeMoodData();

        // Initialize Firebase
        initializeFirebase();

        // Initialize UI elements
        initializeUIElements();

        // Initialize location services
        initializeLocationServices();

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

        // Set Map as the selected item - put this AFTER everything else
        bottomNavigationView.setSelectedItemId(R.id.nav_map);
    }

    /**
     * Initialize UI elements for map control
     */
    private void initializeUIElements() {
        // Initialize toggle button
        mapToggleButton = findViewById(R.id.map_toggle_button);
        mapToggleButton.setTextOff("MY MOODS");
        mapToggleButton.setTextOn("FOLLOWERS' MOODS");
        mapToggleButton.setChecked(false); // Default to showing own moods

        // Find the radius control panel to show/hide it entirely
        View radiusControlPanel = findViewById(R.id.radius_control_panel);

        mapToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                showFollowersMoods = isChecked;

                // Show/hide radius panel based on mode
                if (isChecked) {
                    // When switching to followers mode, show the radius panel
                    radiusControlPanel.setVisibility(View.VISIBLE);
                    // Reset to default radius
                    currentRadiusKm = MIN_RADIUS_KM;
                    radiusSlider.setProgress(0); // First position
                    updateRadiusText();
                } else {
                    // In personal mode, hide the radius panel
                    radiusControlPanel.setVisibility(View.GONE);
                }

                // Refresh the map
                if (mMap != null) {
                    loadMoodData();
                }
            }
        });

        // Initialize radius slider
        radiusSlider = findViewById(R.id.radius_slider);
        radiusValueText = findViewById(R.id.radius_value_text);

        // Configure slider range
        int maxProgress = 95; // For 95 intervals between 5 and 100
        radiusSlider.setMax(maxProgress);
        radiusSlider.setProgress(0); // Default to minimum (5km)

        radiusSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Calculate radius based on progress (5km to 100km)
                currentRadiusKm = MIN_RADIUS_KM + (progress * (MAX_RADIUS_KM - MIN_RADIUS_KM) / maxProgress);
                updateRadiusText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Not needed
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Refresh the map when user stops dragging
                if (mMap != null) {
                    loadMoodData();
                }
            }
        });

        // Initially hide radius panel if starting in personal mode
        if (!showFollowersMoods) {
            radiusControlPanel.setVisibility(View.GONE);
        }

        updateRadiusText();
    }

    /**
     * Update the radius text display
     */
    private void updateRadiusText() {
        radiusValueText.setText(String.format("Radius: %.1f km", currentRadiusKm));
    }

    /**
     * Initialize location services and request permission if needed
     */
    private void initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update current location
                    currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());

                    // If map is ready, center on current location and load mood data
                    if (mMap != null) {
                        // First center map on current location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14));
                        // Then load mood data without changing map center
                        loadMoodData();
                    }

                    // We only need one location update
                    stopLocationUpdates();
                    break;
                }
            }
        };

        // Check for location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable location features
                if (mMap != null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        mMap.setMyLocationEnabled(true);
                    }
                }
                startLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied. Some features may not work properly.",
                        Toast.LENGTH_SHORT).show();
                // Use default location
                currentUserLocation = new LatLng(53.5461, -113.4938); // Edmonton
                if (mMap != null) {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
                }
                loadMoodData();
            }
        }
    }

    /**
     * Initialize Firebase Auth and Firestore
     */
    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Get current user ID or use a device ID if not logged in
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        } else {
            SharedPreferences prefs = getSharedPreferences("VibeVersePrefs", Context.MODE_PRIVATE);
            userId = prefs.getString("device_id", null);

            // If no device ID exists, create one
            if (userId == null) {
                userId = java.util.UUID.randomUUID().toString();
                prefs.edit().putString("device_id", userId).apply();
            }
        }
    }

    /**
     * Initialize mood colors and emojis
     */
    private void initializeMoodData() {
        moodColors.put("Happy", Color.parseColor("#FBC02D"));      // Warm yellow
        moodColors.put("Sad", Color.parseColor("#42A5F5"));        // Soft blue
        moodColors.put("Angry", Color.parseColor("#EF5350"));      // Vibrant red
        moodColors.put("Surprised", Color.parseColor("#FF9800"));  // Orange
        moodColors.put("Afraid", Color.parseColor("#5C6BC0"));     // Indigo blue
        moodColors.put("Disgusted", Color.parseColor("#66BB6A"));  // Green
        moodColors.put("Confused", Color.parseColor("#AB47BC"));   // Purple
        moodColors.put("Shameful", Color.parseColor("#EC407A"));   // Pink

        moodEmojis.put("Happy", "😃");
        moodEmojis.put("Sad", "😢");
        moodEmojis.put("Angry", "😡");
        moodEmojis.put("Surprised", "😲");
        moodEmojis.put("Afraid", "😨");
        moodEmojis.put("Disgusted", "🤢");
        moodEmojis.put("Confused", "🤔");
        moodEmojis.put("Shameful", "😳");
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Set up map UI settings
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Try to enable my-location layer if permission is granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Get current location and center map on it immediately
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Update current location
                            currentUserLocation = new LatLng(location.getLatitude(), location.getLongitude());

                            // Center map on current location immediately
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14));

                            // Then load mood data without changing map center
                            loadMoodData();
                        } else {
                            // If null location, request a fresh location update
                            startLocationUpdates();

                            // Use default location temporarily
                            currentUserLocation = new LatLng(53.5461, -113.4938); // Edmonton
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
                            loadMoodData();
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Use default location if location retrieval fails
                        currentUserLocation = new LatLng(53.5461, -113.4938); // Edmonton
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
                        loadMoodData();
                    });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

            // Use default location until permission granted
            currentUserLocation = new LatLng(53.5461, -113.4938); // Edmonton
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 12));
            loadMoodData();
        }
    }

    /**
     * Call this method to load moods based on current toggle state
     */
    private void loadMoodData() {
        // Show loading toast
        Toast.makeText(this, "Loading mood map...", Toast.LENGTH_SHORT).show();

        // Clear the map
        mMap.clear();

        if (showFollowersMoods) {
            // Load only followers' moods with current radius
            loadFollowedUsersMoods();
            Toast.makeText(this, "Showing followers' moods within " +
                    String.format("%.1f", currentRadiusKm) + " km", Toast.LENGTH_SHORT).show();
        } else {
            // Load only user's own moods
            loadUserMoodsWithoutCentering();
            Toast.makeText(this, "Showing your moods", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Sets initial map position to the current user location
     */
    private void centerMapOnCurrentLocation() {
        if (currentUserLocation != null && mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14));
        }
    }

    /**
     * Loads the user's own moods from Firestore and adds them to the map
     * without changing the map center
     */
    private void loadUserMoodsWithoutCentering() {
        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Check if the mood has location data
                        if (document.contains("moodLatitude") && document.contains("moodLongitude")) {
                            Double latitudeObj = document.getDouble("moodLatitude");
                            Double longitudeObj = document.getDouble("moodLongitude");

                            if (latitudeObj != null && longitudeObj != null) {
                                double latitude = latitudeObj;
                                double longitude = longitudeObj;
                                String locationName = document.getString("moodLocation");
                                String moodTitle = document.getString("mood");
                                String emoji = document.getString("emoji");
                                Long intensity = document.getLong("intensity");
                                String reasonWhy = document.getString("reasonWhy");

                                // Get mood color
                                int moodColor = Color.GRAY; // Default color
                                if (moodColors.containsKey(moodTitle)) {
                                    moodColor = moodColors.get(moodTitle);
                                }

                                // Adjust color based on intensity if available
                                if (intensity != null) {
                                    moodColor = adjustColorIntensity(moodColor, intensity.intValue());
                                }

                                // Create a LatLng object for the mood location
                                LatLng moodLocation = new LatLng(latitude, longitude);

                                // Add a custom marker for this mood
                                addMoodMarker(moodLocation, moodTitle, emoji, moodColor, reasonWhy, locationName, userId, true);
                        } else {
                            Log.e(TAG, "Latitude or longitude is null for document: " + document.getId());
                        }

                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user mood data", e);
                    Toast.makeText(MapsActivity.this, "Error loading user mood data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads the moods of users that the current user is following
     */
    private void loadFollowedUsersMoods() {
        // Get the list of followed users first
        db.collection("users")
                .document(userId)
                .collection("following")
                .document("list")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get the list of user IDs that the current user is following
                        @SuppressWarnings("unchecked")
                        List<String> followingIds = (List<String>) documentSnapshot.get("followingIds");

                        if (followingIds != null && !followingIds.isEmpty()) {
                            // Fetch mood data for each followed user
                            for (String followedUserId : followingIds) {
                                loadFollowedUserMoods(followedUserId);
                            }
                        } else {
                            Log.d(TAG, "User is not following anyone");
                            Toast.makeText(MapsActivity.this, "You are not following anyone yet", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading followed users", e);
                });
    }

    /**
     * Loads mood data for a specific followed user and filters by distance
     */
    private void loadFollowedUserMoods(String followedUserId) {
        db.collection("Usermoods")
                .document(followedUserId)
                .collection("moods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Check if the mood has location data
                        if (document.contains("moodLatitude") && document.contains("moodLongitude")) {
                            Double latitudeObj = document.getDouble("moodLatitude");
                            Double longitudeObj = document.getDouble("moodLongitude");


                            if (latitudeObj != null && longitudeObj != null) {
                                double latitude = latitudeObj;
                                double longitude = longitudeObj;

                                LatLng moodLocation = new LatLng(latitude, longitude);

                                // Check if the mood is within the current radius of the user's location
                                if (isWithinRange(moodLocation, currentUserLocation, currentRadiusKm)) {
                                    String locationName = document.getString("moodLocation");
                                    String moodTitle = document.getString("mood");
                                    String emoji = document.getString("emoji");
                                    Long intensity = document.getLong("intensity");
                                    String reasonWhy = document.getString("reasonWhy");

                                    // Get mood color
                                    int moodColor = Color.GRAY; // Default color
                                    if (moodColors.containsKey(moodTitle)) {
                                        moodColor = moodColors.get(moodTitle);
                                    }

                                    // Adjust color based on intensity if available
                                    if (intensity != null) {
                                        moodColor = adjustColorIntensity(moodColor, intensity.intValue());
                                    }

                                    // Add a custom marker for this mood
                                    addMoodMarker(moodLocation, moodTitle, emoji, moodColor, reasonWhy, locationName, followedUserId, false);
                            }else {
                                Log.e(TAG, "Latitude or longitude is null for document: " + document.getId());
                            }
                            // Create a LatLng object for the mood location
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading followed user mood data", e);
                });
    }

    /**
     * Checks if a mood location is within the specified range of the user's location
     */
    private boolean isWithinRange(LatLng moodLocation, LatLng userLocation, float maxDistanceKm) {
        if (userLocation == null) {
            return false; // Can't determine distance without user location
        }

        // Calculate distance between the two points using Android's Location class
        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.latitude, userLocation.longitude,
                moodLocation.latitude, moodLocation.longitude,
                results);

        // Convert m to km and check if within range
        float distanceKm = results[0] / 1000;
        return distanceKm <= maxDistanceKm;
    }

    /**
     * Adds a custom marker to the map representing a mood
     *
     * @param position The geographic position of the marker
     * @param moodTitle The mood title to display
     * @param emoji The emoji representing the mood
     * @param moodColor The color for the marker
     * @param reasonWhy The reason for the mood
     * @param locationName The location name
     * @param ownerId The user ID of the mood owner
     * @param isOwnMood Whether this is the current user's own mood
     */
    private void addMoodMarker(LatLng position, String moodTitle, String emoji, int moodColor,
                               String reasonWhy, String locationName, String ownerId, boolean isOwnMood) {
        if (!isOwnMood) {
            // For a follower's mood, get their username first, then create the marker
            db.collection("users").document(ownerId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String username = null;
                        if (documentSnapshot.exists()) {
                            username = documentSnapshot.getString("username");
                        }

                        // Create marker with username on the bitmap
                        Bitmap markerBitmap = createCustomMarkerBitmap(emoji, moodTitle, moodColor, username);

                        // Add the marker with the custom bitmap
                        mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(moodTitle)
                                .snippet(reasonWhy + " @ " + locationName)
                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting user info for marker", e);
                        // Fallback: create marker without username
                        Bitmap markerBitmap = createCustomMarkerBitmap(emoji, moodTitle, moodColor, null);
                        mMap.addMarker(new MarkerOptions()
                                .position(position)
                                .title(moodTitle)
                                .snippet(reasonWhy + " @ " + locationName)
                                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
                    });
        } else {
            // Create marker for own mood (without username)
            Bitmap markerBitmap = createCustomMarkerBitmap(emoji, moodTitle, moodColor, null);
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(moodTitle) // Just the mood title, no username
                    .snippet(reasonWhy + " @ " + locationName)
                    .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));
        }
    }

    /**
     * Creates a custom marker bitmap with mood and username (if available)
     *
     * @param emoji The emoji representing the mood
     * @param moodTitle The mood title text
     * @param color The background color for the marker
     * @param username Optional username to display (for followers' moods)
     * @return A bitmap for the custom marker
     */
    private Bitmap createCustomMarkerBitmap(String emoji, String moodTitle, int color, String username) {
        // Adjust height if username is present
        int width = 240;
        int height = username != null ? 310 : 280; // Extra height for username

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Calculate dimensions
        float pinWidth = width * 0.85f;
        float pinHeight = username != null ? height * 0.7f : height * 0.75f; // Slightly shorter to make room for username
        float pinLeft = (width - pinWidth) / 2;
        float pinTop = 0;
        float pinBottom = pinTop + pinHeight;
        float pinRadius = dpToPx(12);

        // Create drop shadow
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setColor(Color.parseColor("#44000000"));
        shadowPaint.setMaskFilter(new BlurMaskFilter(dpToPx(4), BlurMaskFilter.Blur.NORMAL));
        Path shadowPath = new Path();

        // Main pin body for shadow
        RectF shadowRect = new RectF(pinLeft + dpToPx(2), pinTop + dpToPx(2),
                pinLeft + pinWidth + dpToPx(2), pinBottom + dpToPx(2));
        shadowPath.addRoundRect(shadowRect, pinRadius, pinRadius, Path.Direction.CW);

        // Pin pointer for shadow
        float pointerTip = height - dpToPx(2);
        float pointerWidth = pinWidth * 0.3f;
        float pointerLeft = width/2 - pointerWidth/2 + dpToPx(2);
        float pointerRight = width/2 + pointerWidth/2 + dpToPx(2);

        // Shadow pointer path
        shadowPath.moveTo(pointerLeft, pinBottom + dpToPx(2));
        shadowPath.lineTo(width/2 + dpToPx(2), pointerTip);
        shadowPath.lineTo(pointerRight, pinBottom + dpToPx(2));
        shadowPath.close();

        canvas.drawPath(shadowPath, shadowPaint);

        // Main background color with subtle gradient for dimension
        Paint pinPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int lighterColor = adjustColorBrightness(color, 1.1f);
        int darkerColor = adjustColorBrightness(color, 0.9f);

        // Create gradient
        LinearGradient gradient = new LinearGradient(
                0, pinTop, 0, pinBottom,
                lighterColor, darkerColor,
                Shader.TileMode.CLAMP);
        pinPaint.setShader(gradient);

        // Create pin path
        Path pinPath = new Path();

        // Main pin body
        RectF pinRect = new RectF(pinLeft, pinTop, pinLeft + pinWidth, pinBottom);
        pinPath.addRoundRect(pinRect, pinRadius, pinRadius, Path.Direction.CW);

        // Pin pointer
        float pointerTipY = height - dpToPx(4);
        float pointerLeftX = width/2 - pointerWidth/2;
        float pointerRightX = width/2 + pointerWidth/2;

        pinPath.moveTo(pointerLeftX, pinBottom);
        pinPath.lineTo(width/2, pointerTipY);
        pinPath.lineTo(pointerRightX, pinBottom);
        pinPath.close();

        canvas.drawPath(pinPath, pinPaint);

        // Add subtle highlight for depth
        Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        highlightPaint.setColor(Color.WHITE);
        highlightPaint.setAlpha(40); // Very subtle

        Path highlightPath = new Path();
        float highlightHeight = pinHeight * 0.4f;
        RectF highlightRect = new RectF(pinLeft, pinTop, pinLeft + pinWidth, pinTop + highlightHeight);
        highlightPath.addRoundRect(highlightRect, pinRadius, pinRadius, Path.Direction.CW);

        // Only add highlight to top portion with proper clipping
        canvas.save();
        Path clipPath = new Path();
        clipPath.addRoundRect(pinRect, pinRadius, pinRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);
        canvas.drawPath(highlightPath, highlightPaint);
        canvas.restore();

        // Add subtle outline
        Paint outlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outlinePaint.setStyle(Paint.Style.STROKE);
        outlinePaint.setStrokeWidth(dpToPx(0.5f));
        outlinePaint.setColor(Color.parseColor("#22000000"));
        canvas.drawPath(pinPath, outlinePaint);

        // Draw emoji with proper scaling
        float emojiSize = Math.min(width, height) * 0.28f;
        float emojiY = pinTop + (pinHeight * 0.35f);

        Paint emojiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emojiPaint.setTextSize(emojiSize);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(emoji, width/2, emojiY, emojiPaint);

        // Draw mood title with modern typography
        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(getContrastColor(color));
        titlePaint.setTextSize(dpToPx(12));
        titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        titlePaint.setTextAlign(Paint.Align.CENTER);

        // Add minimal divider
        Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dividerPaint.setColor(Color.parseColor("#22000000"));
        dividerPaint.setStyle(Paint.Style.STROKE);
        dividerPaint.setStrokeWidth(dpToPx(0.5f));

        float dividerY = pinTop + (pinHeight * 0.6f);
        float dividerPadding = pinWidth * 0.2f;
        canvas.drawLine(pinLeft + dividerPadding, dividerY,
                pinLeft + pinWidth - dividerPadding, dividerY, dividerPaint);

        // Draw mood label
        float titleY = pinTop + (pinHeight * 0.75f);
        canvas.drawText(moodTitle.toUpperCase(), width/2, titleY, titlePaint);

        // Draw username if provided (for followers' moods)
        if (username != null && !username.isEmpty()) {
            Paint usernamePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            usernamePaint.setColor(getContrastColor(color));
            usernamePaint.setTextSize(dpToPx(11));
            usernamePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            usernamePaint.setTextAlign(Paint.Align.CENTER);

            // Draw "@username" below the mood title
            float usernameY = titleY + dpToPx(14); // Position below the mood title
            canvas.drawText("@" + username, width/2, usernameY, usernamePaint);
        }

        return bitmap;
    }

    /**
     * Alternative implementation using views to create custom marker
     */
    private Bitmap createViewBasedMarkerBitmap(String emoji, String moodTitle, int color) {
        // Create a view for the marker
        CardView cardView = new CardView(this);
        cardView.setCardBackgroundColor(color);
        cardView.setRadius(dpToPx(40));
        cardView.setCardElevation(dpToPx(4));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));

        TextView emojiView = new TextView(this);
        emojiView.setText(emoji);
        emojiView.setTextSize(32);
        emojiView.setGravity(Gravity.CENTER);

        TextView titleView = new TextView(this);
        titleView.setText(moodTitle);
        titleView.setTextSize(16);
        titleView.setTextColor(Color.WHITE);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(null, Typeface.BOLD);

        layout.addView(emojiView);
        layout.addView(titleView);
        cardView.addView(layout);

        // Measure and layout the view
        cardView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        cardView.layout(0, 0, cardView.getMeasuredWidth(), cardView.getMeasuredHeight());

        // Create a bitmap from the view
        Bitmap bitmap = Bitmap.createBitmap(cardView.getMeasuredWidth(),
                cardView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cardView.draw(canvas);

        return bitmap;
    }

    /**
     * Determines the optimal text color (black or white) based on background color brightness
     * Using the W3C contrast algorithm for better readability
     */
    private int getContrastColor(int color) {
        // Calculate perceived brightness using the improved formula
        // (0.299*R + 0.587*G + 0.114*B)
        double brightness = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color));
        return brightness >= 128 ? Color.BLACK : Color.WHITE;
    }

    /**
     * Adjusts the color intensity based on slider progress, similar to SelectMoodActivity
     */
    private int adjustColorIntensity(int baseColor, int intensity) {
        if (intensity < 5) {
            float blendRatio = 0.5f + (intensity / 10f); // 0.5 to 1.0
            return blendColors(baseColor, Color.GRAY, blendRatio);
        }
        // For high intensity, make more vibrant/darker
        else if (intensity > 5) {
            // Increase saturation and adjust brightness
            float factor = 1.0f + ((intensity - 5) / 5f * 0.3f); // 1.0 to 1.3
            return adjustColorSaturation(baseColor, factor);
        }
        // Middle intensity, return base color
        else {
            return baseColor;
        }
    }

    /**
     * Adjusts the saturation of a color
     */
    private int adjustColorSaturation(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[1] = Math.min(1f, hsv[1] * factor);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * (factor > 1 ? 0.9f : 1.1f)));
        return Color.HSVToColor(hsv);
    }

    /**
     * Blends two colors together using the specified ratio
     */
    private int blendColors(int color1, int color2, float ratio) {
        final float inverseRatio = 1f - ratio;
        float r = (Color.red(color1) * ratio) + (Color.red(color2) * inverseRatio);
        float g = (Color.green(color1) * ratio) + (Color.green(color2) * inverseRatio);
        float b = (Color.blue(color1) * ratio) + (Color.blue(color2) * inverseRatio);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    /**
     * Helper method to adjust color brightness
     * @param color The base color
     * @param factor Factor > 1 brightens, factor < 1 darkens
     */
    private int adjustColorBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = Math.max(0f, Math.min(1f, hsv[2] * factor)); // Adjust brightness (V in HSV)
        return Color.HSVToColor(hsv);
    }

    /**
     * Converts dp to pixels - for float values
     */
    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null) {
            // Always center on current location first if available
            if (currentUserLocation != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentUserLocation, 14));
                // Then load mood data without changing the map center
                loadMoodData();
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates when the activity is paused
        stopLocationUpdates();
    }
}