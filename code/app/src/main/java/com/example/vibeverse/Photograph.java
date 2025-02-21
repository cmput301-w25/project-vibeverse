package com.example.vibeverse;

import android.graphics.Bitmap;
import android.net.Uri;

public class Photograph {
    private Uri imageUri;
    private long fileSizeKB;
    private Bitmap bitmap;
    // add fields for date, location, etc.

    public Photograph(Uri imageUri, long fileSizeKB, Bitmap bitmap) {
        this.imageUri = imageUri;
        this.fileSizeKB = fileSizeKB;
        this.bitmap = bitmap;
    }

    public Uri getImageUri() { return imageUri; }
    public long getFileSizeKB() { return fileSizeKB; }
    public Bitmap getBitmap() { return bitmap; }

}
