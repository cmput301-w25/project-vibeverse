package com.example.vibeverse;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mock version of Uri for unit tests
 * This replaces android.net.Uri to avoid Android dependencies in unit tests
 */
class mockUri implements Serializable {
    private final String uriString;

    private mockUri(String uriString) {
        this.uriString = uriString;
    }

    public static mockUri parse(String uriString) {
        return new mockUri(uriString);
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
class mockPhotograph implements Serializable {
    private mockUri imageUri;
    private String imageUriString;
    private long fileSize;
    private Object bitmap; // Replaced Bitmap with Object for mock
    private Date dateTaken;
    private String location;

    /**
     * Constructs a Photograph with a bitmap (mock version)
     */
    public mockPhotograph(mockUri imageMockUri, long fileSize, Object bitmap, Date dateTaken, String location) {
        this.imageUri = imageMockUri;
        this.imageUriString = imageMockUri != null ? imageMockUri.toString() : null;
        this.fileSize = fileSize;
        this.bitmap = bitmap;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    /**
     * Constructs a Photograph without a bitmap (mock version)
     */
    public mockPhotograph(mockUri imageMockUri, long fileSize, Date dateTaken, String location) {
        this.imageUri = imageMockUri;
        this.imageUriString = imageMockUri != null ? imageMockUri.toString() : null;
        this.fileSize = fileSize;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    // For testing only - constructor that accepts a String uri
    public mockPhotograph(String imageUriString, long fileSize, Date dateTaken, String location) {
        this.imageUri = imageUriString != null ? mockUri.parse(imageUriString) : null;
        this.imageUriString = imageUriString;
        this.fileSize = fileSize;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    public String getImageUriString() {
        return imageUriString;
    }

    public void setImageUriString(String imageUriString) {
        this.imageUriString = imageUriString;
        if (imageUriString != null) {
            this.imageUri = mockUri.parse(imageUriString);
        }
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
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

    public void setImageUri(mockUri imageMockUri) {
        this.imageUri = imageMockUri;
        this.imageUriString = imageMockUri != null ? imageMockUri.toString() : null;
    }

    public mockUri getImageUri() {
        return imageUri;
    }

    public long getFileSize() {
        return fileSize;
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


}