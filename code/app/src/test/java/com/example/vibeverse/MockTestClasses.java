package com.example.vibeverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mock version of Uri for unit tests
 * This replaces android.net.Uri to avoid Android dependencies in unit tests
 */
class Uri implements Serializable {
    private final String uriString;

    private Uri(String uriString) {
        this.uriString = uriString;
    }

    public static Uri parse(String uriString) {
        return new Uri(uriString);
    }

    @Override
    public String toString() {
        return uriString;
    }
}

/**
 * Mock version of the Photograph class that doesn't depend on Android classes
 * This is a direct implementation that matches the real Photograph class's interface
 * but avoids dependencies on Android-specific classes (like real Bitmap)
 */
class Photograph implements Serializable {
    private Uri imageUri;
    private String imageUriString;
    private long fileSizeKB;
    private Object bitmap; // Replaced Bitmap with Object for mock
    private Date dateTaken;
    private String location;

    /**
     * Constructs a Photograph with a bitmap (mock version)
     */
    public Photograph(Uri imageUri, long fileSizeKB, Object bitmap, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.imageUriString = imageUri != null ? imageUri.toString() : null;
        this.fileSizeKB = fileSizeKB;
        this.bitmap = bitmap;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    /**
     * Constructs a Photograph without a bitmap (mock version)
     */
    public Photograph(Uri imageUri, long fileSizeKB, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.imageUriString = imageUri != null ? imageUri.toString() : null;
        this.fileSizeKB = fileSizeKB;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    // For testing only - constructor that accepts a String uri
    public Photograph(String imageUriString, long fileSizeKB, Date dateTaken, String location) {
        this.imageUri = imageUriString != null ? Uri.parse(imageUriString) : null;
        this.imageUriString = imageUriString;
        this.fileSizeKB = fileSizeKB;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    public String getImageUriString() {
        return imageUriString;
    }

    public void setImageUriString(String imageUriString) {
        this.imageUriString = imageUriString;
        if (imageUriString != null) {
            this.imageUri = Uri.parse(imageUriString);
        }
    }

    public void setFileSizeKB(long fileSizeKB) {
        this.fileSizeKB = fileSizeKB;
    }

    public void setBitmap(Object bitmap) {
        this.bitmap = bitmap;
    }

    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
        this.imageUriString = imageUri != null ? imageUri.toString() : null;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public long getFileSizeKB() {
        return fileSizeKB;
    }

    public Object getBitmap() {
        return bitmap;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public String getLocation() {
        return location;
    }

    public String getFormattedDetails() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = dateTaken != null ? sdf.format(dateTaken) : "Date unknown";
        String locationStr = location != null ? location : "Location unknown";

        String sizeStr;
        if (fileSizeKB >= 1024) {
            float sizeMB = fileSizeKB / 1024f;
            sizeStr = String.format(Locale.getDefault(), "%.2f MB", sizeMB);
        } else {
            sizeStr = fileSizeKB + " KB";
        }

        return String.format(Locale.getDefault(),
                "Date: %s\nSize: %s\nLocation: %s",
                dateStr,
                sizeStr,
                locationStr);
    }
}