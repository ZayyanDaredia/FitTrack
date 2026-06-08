package com.example.fittrack;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.model.DBHelper;
import com.google.android.material.textfield.TextInputLayout;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etEmail, etAge, etHeight, etWeight, username, password;
    private TextInputLayout tlEmail; // 🟢 Added for red border support
    private Button btnsignup, btnsignin;
    private DBHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        db = new DBHelper(this);

        // Initialize Views
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        tlEmail = findViewById(R.id.tlEmail); // 🟢 Initialize the Layout
        etAge = findViewById(R.id.etAge);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        btnsignup = findViewById(R.id.btnsignup);
        btnsignin = findViewById(R.id.btnsignin);

        // 🟢 Real-time listener to remove red border when user fixes the email
        etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (Patterns.EMAIL_ADDRESS.matcher(s.toString().trim()).matches()) {
                    tlEmail.setError(null); // Removes red border
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnsignup.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String age = etAge.getText().toString().trim();
            String h = etHeight.getText().toString().trim();
            String w = etWeight.getText().toString().trim();
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            // Validation Logic
            if (name.isEmpty() || email.isEmpty() || age.isEmpty() || h.isEmpty() || w.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
            else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                // 🔴 Trigger the Red Border and Error Message
                tlEmail.setError("Enter a valid email address");
                etEmail.requestFocus();
            }
            else {
                // Clear error if valid
                tlEmail.setError(null);

                if (!db.checkusername(user)) {
                    String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    Boolean insert = db.insertData(
                            user,
                            pass,
                            name,
                            email,
                            age,
                            h,
                            w,
                            currentDate
                    );

                    if (insert) {
                        Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "User already exists", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnsignin.setOnClickListener(v -> finish());
    }
}