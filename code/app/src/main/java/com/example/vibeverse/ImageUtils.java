package com.example.vibeverse;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * ImageUtils provides helper methods for processing images,
 * including loading, compressing, and displaying image previews.
 * <p>
 * It includes methods to process an image from a URI, compress a bitmap,
 * create a temporary image file, and show a preview dialog to let the user confirm the image.
 * </p>
 */
public class ImageUtils {

    /**
     * Callback interface for when the user confirms the image preview.
     */
    public interface ImageProcessCallback {

        /**
         * Called when the image is confirmed by the user.
         *
         * @param bitmap   The processed Bitmap.
         * @param imageUri The URI of the image.
         * @param size The size of the image .
         */
        void onImageConfirmed(Bitmap bitmap, Uri imageUri, long size);

    }

    /**
     * Processes the image by loading the Bitmap from the provided URI,
     * compressing it if its size exceeds a threshold, and then showing a preview dialog.
     *
     * @param activity The Activity context.
     * @param imageUri The URI of the image to process.
     * @param callback The callback invoked when the user confirms the image.
     */
    public static void processImage(final Activity activity, final Uri imageUri, final ImageProcessCallback callback) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);

            // Initialize compression settings
            int quality = 100;
            Bitmap currentBitmap = bitmap;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            byte[] imageBytes = baos.toByteArray();

            // Iteratively compress until the image is below 65,536 bytes
            while (imageBytes.length > 65536) {
                // Lower the quality in steps of 5 if possible
                if (quality > 10) {
                    quality -= 5;
                } else {
                    // If quality is too low, scale down the image and reset quality
                    currentBitmap = compressBitmap(currentBitmap);
                    quality = 100;
                }
                baos.reset();
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                imageBytes = baos.toByteArray();
            }

            // The final compressed byte array is guaranteed to be below 65,536 bytes
            long newSizeInBytes = imageBytes.length;

            Date dateTaken = new Date(); // current date for demo purposes
            String location = "Test Location"; // Replace with an actual location if available

            // Pass the compressed bitmap and the final byte array to the preview dialog
            showPreviewDialog(activity, currentBitmap, imageBytes, imageUri, newSizeInBytes, dateTaken, location, callback);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Compresses the provided Bitmap by scaling it down by 80%.
     *
     * @param bitmap The original Bitmap.
     * @return The compressed Bitmap.
     */
    public static Bitmap compressBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (int) (width * 0.8);
        int newHeight = (int) (height * 0.8);
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    /**
     * Creates an image file in the app's external pictures directory.
     *
     * @param context The Context.
     * @return The newly created image File.
     * @throws IOException if file creation fails.
     */
    public static File createImageFile(Context context) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }


    /**
     * Shows a preview dialog containing the processed image. When the user confirms,
     * the provided callback is invoked.
     *
     * @param activity       The Activity context.
     * @param bitmap         The processed Bitmap.
     * @param imageBytes     The final compressed image bytes (guaranteed to be below 65,536 bytes).
     * @param imageUri       The URI of the image.
     * @param fileSizeBytes  The size of the image in bytes.
     * @param dateTaken      The date the image was taken.
     * @param location       The location information.
     * @param callback       Callback to be invoked on confirmation.
     */
    public static void showPreviewDialog(final Activity activity, final Bitmap bitmap, final byte[] imageBytes, final Uri imageUri, final long fileSizeBytes, final Date dateTaken, final String location, final ImageProcessCallback callback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View dialogView = activity.getLayoutInflater().inflate(R.layout.image_preview_dialog, null);
        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        previewImageView.setImageBitmap(bitmap);

        builder.setView(dialogView)
                .setTitle("Preview Image")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                        StorageReference imageRef = storageRef.child("images/" + System.currentTimeMillis() + ".jpg");

                        // Upload the already compressed image bytes that are below 65,536 bytes
                        UploadTask uploadTask = imageRef.putBytes(imageBytes);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            // Retrieve the download URL AFTER successful upload
                            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                Uri uploadedImageUri = downloadUri; // Firebase Storage URL
                                callback.onImageConfirmed(bitmap, uploadedImageUri, fileSizeBytes);
                                Toast.makeText(activity, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                            }).addOnFailureListener(e -> {
                                Toast.makeText(activity, "Failed to get download URL: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }).addOnFailureListener(e -> {
                            Toast.makeText(activity, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

}
