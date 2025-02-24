package com.example.vibeverse;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
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

    private static final int REQUEST_IMAGE_CAPTURE = 1; // for images captured directly from the camera
    private static final int REQUEST_PICK_IMAGE = 2; // for images taken from the gallery
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Uri imageUri;
    private Bitmap currentBitmap;
    private ImageView imgPlaceholder;
    private ImageView imgSelected;
    private TextView txtImageDetails;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.create_mood);

        imgPlaceholder = findViewById(R.id.imgPlaceholder);
        imgSelected = findViewById(R.id.imgSelected);
        txtImageDetails = findViewById(R.id.txtImageDetails);

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
        Toast.makeText(this, "Activity result: requestCode=" + requestCode +
                ", resultCode=" + resultCode, Toast.LENGTH_SHORT).show();
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

            Date dateTaken = new Date(); // For testing, using current date
            String location = "Test Location"; // In a real app, you'd get this from GPS or EXIF data

            Photograph photograph = new Photograph(imageUri, sizeKB, bitmap, dateTaken, location);
            txtImageDetails.setText(photograph.getFormattedDetails());
            // Display a preview dialog of the (possibly compressed) image
            showPreviewDialog(bitmap, imageUri, sizeKB, dateTaken, location);

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
        requestPermissions();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Add this debug Toast
        Toast.makeText(this, "Starting camera intent", Toast.LENGTH_SHORT).show();

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Add error handling
                Toast.makeText(this, "Error creating image file: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                ex.printStackTrace();
                return;
            }

            if (photoFile != null) {
                try {
                    imageUri = FileProvider.getUriForFile(this,
                            "com.example.vibeverse.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                    // Add success Toast
                    Toast.makeText(this, "Camera intent started", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    // Add error handling
                    Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            } else {
                Toast.makeText(this, "Could not create photo file", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No camera app available", Toast.LENGTH_SHORT).show();
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

    private void showPreviewDialog(final Bitmap bitmap, final Uri imageUri, final long fileSizeKB, final Date dateTaken, final String location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.image_preview_dialog, null);
        ImageView previewImageView = dialogView.findViewById(R.id.previewImageView);
        previewImageView.setImageBitmap(bitmap);

        builder.setView(dialogView)
                .setTitle("Preview Image")
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        currentBitmap = bitmap;
                        imgPlaceholder.setVisibility(View.GONE);
                        imgSelected.setVisibility(View.VISIBLE);
                        imgSelected.setImageBitmap(bitmap);
                        txtImageDetails.setVisibility(View.VISIBLE);

                        // Create a Photograph object when confirmed
                        Photograph photograph = new Photograph(imageUri, fileSizeKB, bitmap, dateTaken, location);
                        // You can now proceed to attach the photograph to your post
                        Toast.makeText(MainActivity.this, "Image selected!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Dismiss the dialog and allow user to pick a different image
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void requestPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }





}
