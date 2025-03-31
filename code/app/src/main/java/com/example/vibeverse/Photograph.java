package com.example.vibeverse;

import android.graphics.Bitmap;
import android.net.Uri;

import java.io.Serializable;
import java.util.Date;

/**
 * Represents a photograph associated with a mood event.
 * <p>
 * This class stores information about an image including its URI,
 * file size (in kilobytes), an optional bitmap, the date the image was taken,
 * and an optional location. It implements {@link Serializable} so that instances
 * can be passed between activities via Intents.
 * </p>
 */
public class Photograph implements Serializable {
    private Uri imageUri;
    private String imageUriString;
    private long fileSize;
    private Bitmap bitmap;
    private Date dateTaken;
    private String location;

    /**
     * Constructs a Photograph with a bitmap.
     *
     * @param imageUri   The URI of the image.
     * @param fileSize The size of the image file in kilobytes.
     * @param bitmap     The bitmap of the image.
     * @param dateTaken  The date when the image was taken.
     * @param location   The location where the image was taken.
     */
    public Photograph(Uri imageUri, long fileSize, Bitmap bitmap, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.fileSize = fileSize;
        this.bitmap = bitmap;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    /**
     * Constructs a Photograph without a bitmap.
     * <p>
     * The image URI is stored as a string.
     * </p>
     *
     * @param imageUri   The URI of the image.
     * @param fileSize The size of the image file in kilobytes.
     * @param dateTaken  The date when the image was taken.
     * @param location   The location where the image was taken.
     */
    public Photograph(Uri imageUri, long fileSize, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.imageUriString = imageUri.toString();
        this.fileSize = fileSize;
        this.dateTaken = dateTaken;
        this.location = location;
    }

    public Photograph(String imageUri, long photoSize, Date dateTaken, String location) {
        this.imageUriString = imageUri;
        this.fileSize = photoSize;
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
     * @param fileSize The file size
     */
    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
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
     * @return The file size.
     */
    public long getFileSize() {
        return fileSize;
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
     * @return The date the image was taken.
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


}
