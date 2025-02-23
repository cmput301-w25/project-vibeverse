package com.example.vibeverse;



import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;
import java.util.Calendar;

public class UserDetailsActivity extends AppCompatActivity {

    private EditText fullName, username, age, dob, bio;
    private Spinner genderSpinner;
    private ImageView profilePicture;
    private Uri imageUri;
    private SharedPreferences sharedPreferences;

    private static final int PICK_IMAGE = 1;
    private static final int CAPTURE_IMAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        fullName = findViewById(R.id.fullName);
        username = findViewById(R.id.username);
        age = findViewById(R.id.age);
        dob = findViewById(R.id.dob);
        bio = findViewById(R.id.bio);
        genderSpinner = findViewById(R.id.genderSpinner);
        profilePicture = findViewById(R.id.profilePicture);
        Button selectProfilePicture = findViewById(R.id.selectProfilePicture);
        Button continueButton = findViewById(R.id.continueButton);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);

        // Gender dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"Select Gender", "Male", "Female", "Other"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        // Date Picker for DOB
        dob.setOnClickListener(v -> showDatePicker());

        // Profile Picture selection
        selectProfilePicture.setOnClickListener(v -> chooseProfilePicture());

        // Continue Button
        continueButton.setOnClickListener(v -> saveUserData());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> dob.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void chooseProfilePicture() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE) {
                imageUri = data.getData();
                profilePicture.setImageURI(imageUri);
            }
        }
    }

    private void saveUserData() {
        if (fullName.getText().toString().isEmpty() || username.getText().toString().isEmpty() || age.getText().toString().isEmpty() || dob.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("FullName", fullName.getText().toString());
        editor.putString("Username", username.getText().toString());
        editor.putString("Age", age.getText().toString());
        editor.putString("Gender", genderSpinner.getSelectedItem().toString());
        editor.putString("DOB", dob.getText().toString());
        editor.putString("Bio", bio.getText().toString());
        editor.apply();

        Toast.makeText(this, "Data Saved!", Toast.LENGTH_SHORT).show();
    }
}

