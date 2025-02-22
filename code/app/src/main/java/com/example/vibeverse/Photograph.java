package com.example.vibeverse;

import android.graphics.Bitmap;
import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Photograph {
    private Uri imageUri;
    private long fileSizeKB;
    private Bitmap bitmap;
    private Date dateTaken;
    private String location;


    public Photograph(Uri imageUri, long fileSizeKB, Bitmap bitmap, Date dateTaken, String location) {
        this.imageUri = imageUri;
        this.fileSizeKB = fileSizeKB;
        this.bitmap = bitmap;
        this.dateTaken = dateTaken;
        this.location = location;
    }


    public Uri getImageUri() {
        return imageUri;
    }

    public long getFileSizeKB() {
        return fileSizeKB;
    }

    public Bitmap getBitmap() {
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
