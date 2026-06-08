package com.example.fittrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fittrack.model.DBHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etAge, etHeight, etWeight;
    private MaterialButton btnSave;
    private DBHelper dbHelper;
    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        dbHelper = new DBHelper(this);

        // 🟢 1. Get Username (with Backup from SharedPreferences)
        username = getIntent().getStringExtra("username");

        if (username == null || username.isEmpty()) {
            SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            username = pref.getString("username", "");
            Log.d("EDIT_DEBUG", "Intent was empty, pulled from Prefs: " + username);
        }

        initViews();

        // 2. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbarEdit);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setTitle("");
                toolbar.setNavigationOnClickListener(v -> finish());
            }
        }

        // 3. Load Data immediately
        loadCurrentData();

        // 4. Save Logic
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void initViews() {
        etName = findViewById(R.id.etEditName);
        etEmail = findViewById(R.id.etEditEmail);
        etAge = findViewById(R.id.etEditAge);
        etHeight = findViewById(R.id.etEditHeight);
        etWeight = findViewById(R.id.etEditWeight);
        btnSave = findViewById(R.id.btnSaveProfile);
    }

    private void loadCurrentData() {
        android.util.Log.d("APP_DEBUG", "---> EditProfileActivity: Starting loadCurrentData for " + username);

        Map<String, String> data = dbHelper.getFullUserDetails(username);

        if (data != null && !data.isEmpty()) {
            etName.setText(data.get("fullname"));
            etEmail.setText(data.get("email"));
            etAge.setText(data.get("age"));
            etHeight.setText(data.get("height"));
            etWeight.setText(data.get("weight"));
            android.util.Log.d("APP_DEBUG", "UI Updated with DB data.");
        } else {
            android.util.Log.e("APP_DEBUG", "UI NOT Updated: Data map is empty.");
            Toast.makeText(this, "Profile data not found", Toast.LENGTH_SHORT).show();
        }
    }

    // 🟢 Helper to ensure UI stays clean
    private String checkNull(String value) {
        if (value == null || value.equalsIgnoreCase("null") || value.isEmpty()) {
            return "";
        }
        return value;
    }

    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String height = etHeight.getText().toString().trim();
        String weight = etWeight.getText().toString().trim();

        // Validation
        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Name and Email are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🟢 Perform Update
        boolean updated = dbHelper.updateProfile(username, name, email, age, height, weight);

        if (updated) {
            Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
            // 🟢 This will trigger onResume() in your ProfileActivity to refresh the UI
            setResult(RESULT_OK);
            finish();
        } else {
            Log.e("EDIT_DEBUG", "Update failed for username: " + username);
            Toast.makeText(this, "Failed to save changes", Toast.LENGTH_SHORT).show();
        }
    }
}