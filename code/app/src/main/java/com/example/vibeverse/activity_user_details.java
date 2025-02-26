package com.example.vibeverse;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class activity_user_details extends AppCompatActivity {

    private EditText fullName, username, bio, age, dob;
    private Spinner genderSpinner;
    private Button continueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_details);

        // Initialize input fields
        fullName = findViewById(R.id.fullName);
        username = findViewById(R.id.username);
        bio = findViewById(R.id.bio);
        age = findViewById(R.id.age);
        dob = findViewById(R.id.dob);
        genderSpinner = findViewById(R.id.genderSpinner);
        continueButton = findViewById(R.id.continueButton);

        // Set up the gender spinner
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
//                R.array.gender_options, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.gender_options)) {
            @Override
            public boolean isEnabled(int position) {
                return position != 0; // Disable first item (placeholder)
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                if (position == 0) {
                    textView.setTextColor(getResources().getColor(R.color.white)); // Placeholder color
                } else {
                    textView.setTextColor(Color.WHITE);
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(Color.BLACK);
                return view;
            }
        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);



        // Set onClickListener for the continue button
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle button click
                handleContinueButtonClick();
            }
        });

        // Set onClickListener for the date of birth field
        dob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });
    }

    private void handleContinueButtonClick() {
        // Handle the logic for the continue button click
        String fullNameText = fullName.getText().toString();
        String usernameText = username.getText().toString();
        String bioText = bio.getText().toString();
        String ageText = age.getText().toString();
        String dobText = dob.getText().toString();
        String gender = genderSpinner.getSelectedItem().toString();

        // Add your logic here
    }

    private void showDatePicker() {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Set the selected date to the dob EditText
                String date = dayOfMonth + "/" + (month + 1) + "/" + year;
                dob.setText(date);

            }
        }, year, month, day);

        // Show the date picker dialog
        datePickerDialog.show();
    }
}