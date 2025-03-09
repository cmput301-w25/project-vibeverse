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

public class ImageUtils {

    // Callback interface for when the user confirms the image preview.
    public interface ImageProcessCallback {
        void onImageConfirmed(Bitmap bitmap, Uri imageUri, long sizeKB);
    }

    /**
     * Processes the image by loading the bitmap from the provided Uri,
     * compressing it if the size is over a threshold, and then showing a preview dialog.
     *
     * @param activity The Activity context.
     * @param imageUri The Uri of the image.
     * @param callback Callback invoked when the user confirms the image.
     */
    public static void processImage(final Activity activity, final Uri imageUri, final ImageProcessCallback callback) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), imageUri);

            // Estimate file size
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            long sizeKB = imageBytes.length / 1024;

            // Compress if needed
            if (sizeKB > 65536) {
                bitmap = compressBitmap(bitmap);
                baos.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                imageBytes = baos.toByteArray();
                sizeKB = imageBytes.length / 1024;
            }

            Date dateTaken = new Date(); // current date for demo purposes
            String location = "Test Location"; // Replace with an actual location if available

            // Show preview dialog for confirmation
            showPreviewDialog(activity, bitmap, imageUri, sizeKB, dateTaken, location, callback);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Compresses the provided bitmap by scaling it down by 80%.
     *
     * @param bitmap The original bitmap.
     * @return The compressed bitmap.
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
     * @param activity  The Activity context.
     * @param bitmap    The processed bitmap.
     * @param imageUri  The Uri of the image.
     * @param fileSizeKB Estimated file size in KB.
     * @param dateTaken The date the image was taken.
     * @param location  The location information.
     * @param callback  Callback to be invoked on confirmation.
     */
    public static void showPreviewDialog(final Activity activity, final Bitmap bitmap, final Uri imageUri, final long fileSizeKB, final Date dateTaken, final String location, final ImageProcessCallback callback) {
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

                        // Convert compressed bitmap to byte array
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        byte[] compressedData = baos.toByteArray();

                        // Upload compressed image as byte array
                        UploadTask uploadTask = imageRef.putBytes(compressedData);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            // Retrieve the download URL AFTER successful upload
                            imageRef.getDownloadUrl().addOnSuccessListener(downloadUri -> {
                                Uri uploadedImageUri = downloadUri; // Firebase Storage URL
                                callback.onImageConfirmed(bitmap, uploadedImageUri, fileSizeKB);
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
