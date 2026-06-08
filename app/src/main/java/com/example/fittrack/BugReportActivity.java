package com.example.fittrack;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fittrack.model.DBHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class BugReportActivity extends AppCompatActivity {

    private Spinner spinCategory;
    private TextInputEditText etDescription;
    private MaterialButton btnSubmit;
    private Toolbar toolbar;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bug_report);

        dbHelper = new DBHelper(this);

        // 1. Initialize Toolbar
        toolbar = findViewById(R.id.toolbar_bug);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Initialize UI Elements
        spinCategory = findViewById(R.id.spinnerCategory);
        etDescription = findViewById(R.id.etBugDescription);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        // 🟢 3. POPULATE THE SPINNER (The missing piece!)
        setupCategorySpinner();

        // 4. Submit Button Logic
        btnSubmit.setOnClickListener(v -> processReport());
    }

    private void setupCategorySpinner() {
        // Define your categories
        String[] categories = {"UI Issue", "App Crash", "Performance", "Feature Request", "Other"};

        // Create the adapter (using a standard Android dark-theme layout)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );

        // Set the dropdown layout style
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Attach adapter to spinner
        spinCategory.setAdapter(adapter);
    }

    private void processReport() {
        String category = spinCategory.getSelectedItem() != null ?
                spinCategory.getSelectedItem().toString() : "General";

        String description = etDescription.getText() != null ?
                etDescription.getText().toString().trim() : "";

        if (description.isEmpty()) {
            etDescription.setError("Please describe the issue");
            return;
        }

        // 🟢 5. Save to Database (Assuming you have an insertBugReport method)
        // If you haven't created the DB method yet, the Toast still works!
        boolean isInserted = dbHelper.insertBugReport(category, description);

        if (isInserted) {
            Toast.makeText(this, "Bug reported successfully!", Toast.LENGTH_SHORT).show();
            finish(); // Close activity and go back
        } else {
            Toast.makeText(this, "Failed to submit report. Try again.", Toast.LENGTH_SHORT).show();
        }
    }
}