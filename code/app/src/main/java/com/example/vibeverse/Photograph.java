package com.example.vibeverse;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents a photograph associated with a mood event.
 * <p>
 * This class stores information about an image including its URI,
 * file size, bitmap, the date it was taken, and an optional location.
 * It implements {@link Serializable} so that Photograph instances can be
 * passed between activities via Intents.
 * </p>
 */
public class Photograph implements Serializable {
    private Uri imageUri;
    private String imageUriString;
    private long fileSizeKB;
    private Bitmap bitmap;
    private Date dateTaken;
    private String location;

    /**
     * Constructs a Photograph with a bitmap.
     *
     * @param imageUri   The URI of the image.
     * @param fileSizeKB The size of the image file in kilobytes.
     * @param bitmap     The bitmap of the image.
     * @param dateTaken  The date when the image was taken.
     * @param location   The location where the image was taken.
     */
    public Photograph(Uri imageUri, long fileSizeKB, Bitmap bitmap, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.fileSizeKB = fileSizeKB;
        this.bitmap = bitmap;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    /**
     * Constructs a Photograph without a bitmap.
     * <p>
     * The imageUri is stored as a string.
     * </p>
     *
     * @param imageUri   The URI of the image.
     * @param fileSizeKB The size of the image file in kilobytes.
     * @param dateTaken  The date when the image was taken.
     * @param location   The location where the image was taken.
     */
    public Photograph(Uri imageUri, long fileSizeKB, Date dateTaken, String location) {
        this.imageUriString = imageUri.toString();
        this.fileSizeKB = fileSizeKB;
        this.dateTaken = dateTaken;
        this.location = location;
    }


    /**
     * Returns the image URI as a string.
     *
     * @return The image URI string.
     */
    public String getImageUriString() {
        return imageUriString;
    }

    /**
     * Sets the image URI string.
     *
     * @param imageUriString The new image URI string.
     */
    public void setImageUriString(String imageUriString) {
        this.imageUriString = imageUriString;
    }

    /**
     * Sets the file size of the image in kilobytes.
     *
     * @param fileSizeKB The file size in KB.
     */
    public void setFileSizeKB(long fileSizeKB) {
        this.fileSizeKB = fileSizeKB;
    }

    /**
     * Sets the bitmap associated with this photograph.
     *
     * @param bitmap The bitmap to set.
     */
    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    /**
     * Sets the date when the image was taken.
     *
     * @param dateTaken The date the image was taken.
     */
    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    /**
     * Sets the location where the image was taken.
     *
     * @param location The location to set.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the image URI.
     *
     * @param imageUri The URI of the image.
     */
    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    /**
     * Returns the image URI.
     *
     * @return The image URI.
     */
    public Uri getImageUri() {
        return imageUri;
    }

    /**
     * Returns the file size of the image in kilobytes.
     *
     * @return The file size in KB.
     */
    public long getFileSizeKB() {
        return fileSizeKB;
    }

    /**
     * Returns the bitmap of the image.
     *
     * @return The bitmap.
     */
    public Bitmap getBitmap() {
        return bitmap;
    }

    /**
     * Returns the date when the image was taken.
     *
     * @return The date taken.
     */
    public Date getDateTaken() {
        return dateTaken;
    }

    /**
     * Returns the location where the image was taken.
     *
     * @return The location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Returns formatted details about the photograph, including the date, file size, and location.
     *
     * @return A formatted string containing the image details.
     */
    public String getFormattedDetails() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        String dateStr = dateTaken != null ? sdf.format(dateTaken) : "Date unknown";
        String locationStr = location != null ? location : "Location unknown";

        // Format size to be more readable
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
