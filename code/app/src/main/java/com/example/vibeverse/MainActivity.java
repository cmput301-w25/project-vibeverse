package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.example.vibeverse.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PICK_IMAGE = 2;
    private Uri imageUri;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_mood);

        // Temporary testing button
        FrameLayout btnTestImage = findViewById(R.id.btnTestImage);
        btnTestImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showImagePickerDialog();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // For the camera, imageUri is already set.
                processImage(imageUri);
            } else if (requestCode == REQUEST_PICK_IMAGE) {
                // For gallery, retrieve the image URI from the returned data.
                imageUri = data.getData();
                processImage(imageUri);
            }
        }
    }

    private void processImage(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

            // Convert bitmap to byte array to estimate file size (in KB)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            long sizeKB = imageBytes.length / 1024;

            // If the image is larger than the threshold, compress it
            if (sizeKB > 65536) {
                bitmap = compressBitmap(bitmap);
                // Recalculate size after compression
                baos.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                imageBytes = baos.toByteArray();
                sizeKB = imageBytes.length / 1024;
            }

            currentBitmap = bitmap;
            // Display a preview dialog of the (possibly compressed) image
            showPreviewDialog(bitmap, imageUri, sizeKB);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Bitmap compressBitmap(Bitmap bitmap) {
        // Example: Scale down the bitmap to 80% of its original dimensions
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth = (int) (width * 0.8);
        int newHeight = (int) (height * 0.8);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return scaledBitmap;
    }


    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Option to capture image using camera
                            dispatchTakePictureIntent();
                        } else {
                            // Option to choose image from gallery
                            dispatchPickImageIntent();
                        }
                    }
                })
                .show();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                imageUri = FileProvider.getUriForFile(this, "com.example.vibeverse.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private void dispatchPickImageIntent() {
        Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickIntent, REQUEST_PICK_IMAGE);
    }


    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void showPreviewDialog(final Bitmap bitmap, final Uri imageUri, final long fileSizeKB) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.image_preview_dialog, null);
        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        previewImageView.setImageBitmap(bitmap);

        builder.setView(dialogView)
                .setTitle("Preview Image")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Create a Photograph object when confirmed
                        Photograph photograph = new Photograph(imageUri, fileSizeKB, bitmap);
                        // You can now proceed to attach the photograph to your post
                        Toast.makeText(MainActivity.this, "Image confirmed!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Choose another", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog and allow user to pick a different image
                        dialog.dismiss();
                    }
                })
                .show();
    }





}
