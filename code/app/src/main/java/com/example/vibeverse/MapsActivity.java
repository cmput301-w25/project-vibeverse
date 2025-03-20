package com.example.vibeverse;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

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

        // Default location (will be overridden by user's moods if available)
        LatLng defaultLocation = new LatLng(53.5461, -113.4938); // Edmonton coordinates
        boolean hasMoodLocations = false;

        // Fetch user mood data from Firestore
        loadUserMoods(defaultLocation);
    }

    /**
     * Loads the user's moods from Firestore and adds them to the map
     */
    private void loadUserMoods(LatLng defaultLocation) {
        // Show loading toast
        Toast.makeText(this, "Loading your mood map...", Toast.LENGTH_SHORT).show();

        db.collection("Usermoods")
                .document(userId)
                .collection("moods")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(MapsActivity.this, "No mood locations found", Toast.LENGTH_SHORT).show();
                        // If no moods, center on default location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                        return;
                    }

                    LatLng latestMoodLocation = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // Check if the mood has location data
                        if (document.contains("moodLatitude") && document.contains("moodLongitude")) {
                            double latitude = document.getDouble("moodLatitude");
                            double longitude = document.getDouble("moodLongitude");
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

                            // Remember the most recent mood location to center the map
                            if (latestMoodLocation == null) {
                                latestMoodLocation = moodLocation;
                            }

                            // Add a custom marker for this mood
                            addMoodMarker(moodLocation, moodTitle, emoji, moodColor, reasonWhy, locationName);
                        }
                    }

                    // Center and zoom the map on the latest mood location if available
                    if (latestMoodLocation != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latestMoodLocation, 13));
                    } else {
                        // If no mood locations, center on default location
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading mood data", e);
                    Toast.makeText(MapsActivity.this, "Error loading mood data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    // Center on default location if error
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12));
                });
    }

    /**
     * Adds a custom marker to the map representing a mood
     */
    private void addMoodMarker(LatLng position, String moodTitle, String emoji, int moodColor, String reasonWhy, String locationName) {
        // Create a custom marker icon
        Bitmap markerBitmap = createCustomMarkerBitmap(emoji, moodTitle, moodColor);

        // Create the marker
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(position)
                .title(moodTitle)
                .snippet(reasonWhy + " @ " + locationName)
                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap)));

        // Set custom info window if needed
        // mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
    }

    /**
     * Creates a custom bitmap for the mood marker with a sleek, professional design
     * that matches the screenshot provided
     */
    private Bitmap createCustomMarkerBitmap(String emoji, String moodTitle, int color) {
        // Create a higher resolution bitmap for sharper edges
        int width = 400;
        int height = 480;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Draw transparent background
        canvas.drawColor(Color.TRANSPARENT);

        // Create colored background card with rounded corners
        Paint cardPaint = new Paint();
        cardPaint.setColor(color);
        cardPaint.setAntiAlias(true);
        cardPaint.setShadowLayer(12, 0, 4, Color.parseColor("#33000000"));
        cardPaint.setStyle(Paint.Style.FILL);

        float cardRadius = dpToPx(20);
        float cardTop = dpToPx(8);
        float cardBottom = height - dpToPx(70); // Leave space for pointer

        // Draw card background with mood color
        canvas.drawRoundRect(dpToPx(8), cardTop, width - dpToPx(8), cardBottom,
                cardRadius, cardRadius, cardPaint);

        // Draw pointer (triangle at bottom)
        Paint pointerPaint = new Paint();
        pointerPaint.setColor(color);
        pointerPaint.setAntiAlias(true);
        pointerPaint.setStyle(Paint.Style.FILL);

        android.graphics.Path pointerPath = new android.graphics.Path();
        float pointerTop = cardBottom;
        float pointerMiddle = cardBottom + dpToPx(30);
        float pointerLeft = width/2 - dpToPx(16);
        float pointerRight = width/2 + dpToPx(16);

        pointerPath.moveTo(pointerLeft, pointerTop);
        pointerPath.lineTo(width/2, pointerMiddle);
        pointerPath.lineTo(pointerRight, pointerTop);
        pointerPath.close();

        // Draw pointer with shadow
        Paint shadowPaint = new Paint(pointerPaint);
        shadowPaint.setShadowLayer(8, 0, 4, Color.parseColor("#33000000"));
        canvas.drawPath(pointerPath, shadowPaint);

        // Add a subtle border for better definition
        Paint borderPaint = new Paint();
        borderPaint.setColor(adjustColorBrightness(color, 0.85f));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(2));
        borderPaint.setAntiAlias(true);
        canvas.drawRoundRect(dpToPx(8), cardTop, width - dpToPx(8), cardBottom,
                cardRadius, cardRadius, borderPaint);

        // Draw emoji with improved positioning
        Paint emojiPaint = new Paint();
        emojiPaint.setColor(Color.BLACK);
        emojiPaint.setTextSize(width/2.5f);
        emojiPaint.setAntiAlias(true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);

        // Position emoji in the upper portion of the card
        float cardHeight = cardBottom - cardTop;
        float emojiY = cardTop + cardHeight * 0.35f;
        float emojiTextHeight = emojiPaint.descent() - emojiPaint.ascent();
        float emojiTextOffset = emojiTextHeight / 2 - emojiPaint.descent();
        canvas.drawText(emoji, width/2f, emojiY + emojiTextOffset, emojiPaint);

        // Draw divider line (optional)
        Paint dividerPaint = new Paint();
        dividerPaint.setColor(adjustColorBrightness(color, 0.85f));
        dividerPaint.setStrokeWidth(dpToPx((int) 1.5f));
        dividerPaint.setAntiAlias(true);
        float dividerY = cardTop + cardHeight * 0.65f;
        canvas.drawLine(dpToPx(30), dividerY, width - dpToPx(30), dividerY, dividerPaint);

        // Draw mood title with better typography
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(dpToPx(24));
        titlePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titlePaint.setAntiAlias(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        // Position the title in the lower portion of the card
        float titleY = cardTop + cardHeight * 0.82f;
        canvas.drawText(moodTitle, width/2f, titleY, titlePaint);

        return bitmap;
    }

    // Helper method to adjust color brightness
    private int adjustColorBrightness(int color, float factor) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= factor; // Adjust brightness
        return Color.HSVToColor(hsv);
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
     * Converts dp to pixels
     */
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}