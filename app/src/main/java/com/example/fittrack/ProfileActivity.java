package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.fittrack.model.DBHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvName, tvEmail, tvAge, tvHeight, tvWeight, tvUsername;
    private ShapeableImageView ivProfilePic;
    private FloatingActionButton btnChangePhoto;
    private MaterialButton btnEditProfile; // 🟢 Updated to MaterialButton

    private DBHelper dbHelper;
    private String currentUser;

    // Photo Picker Logic
    // Inside your ProfileActivity class declaration
    private final ActivityResultLauncher<Intent> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            // 1. 🟢 CRASH PROTECTION: Request permanent access
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                        } catch (SecurityException e) {
                            // If it fails, we still show the photo for this session
                            android.util.Log.e("FITTRACK", "Could not persist permission: " + e.getMessage());
                        }

                        // 2. Show the photo
                        ivProfilePic.setImageURI(null); // Clear first
                        ivProfilePic.setImageURI(imageUri);

                        // 3. Save the path
                        getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                                .edit()
                                .putString("profile_uri_" + currentUser, imageUri.toString())
                                .apply();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // 1. Initialize DB and User
        dbHelper = new DBHelper(this);
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "");

        // 2. Initialize UI Elements
        initViews();

        // 3. Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // 4. Set Username and Load Profile Data
        tvUsername.setText("@" + currentUser);

        // 🟢 LOAD TEXT DATA AND SAVED PHOTO
        loadUserProfile();

        // 5. Button Listeners
        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); // 🟢 Use OPEN_DOCUMENT for better persistence
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");

            // Add these flags to the intent itself
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            photoPickerLauncher.launch(intent);
        });

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, EditProfileActivity.class);

            // 🟢 Ensure we send ONLY the clean ID (e.g., "Zayyan", not "@Zayyan")
            String cleanId = currentUser.replace("@", "").trim();

            intent.putExtra("username", cleanId);
            startActivity(intent);
        });
    }

    private void initViews() {
        // Text Fields
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvAge = findViewById(R.id.tvProfileAge);
        tvHeight = findViewById(R.id.tvProfileHeight);
        tvWeight = findViewById(R.id.tvProfileWeight);
        tvUsername = findViewById(R.id.tvProfileUsername);

        // Material Components (Must match XML types exactly to prevent "Process Ended")
        ivProfilePic = findViewById(R.id.ivProfilePic);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnEditProfile = findViewById(R.id.btnEditProfile);
    }

    private void loadUserProfile() {
        // --- PART A: Load Text Data from Database ---
        Map<String, String> userDetails = dbHelper.getFullUserDetails(currentUser);

        if (userDetails != null && !userDetails.isEmpty()) {
            tvName.setText(userDetails.get("fullname"));
            tvEmail.setText(userDetails.get("email"));
            tvAge.setText(userDetails.get("age") + " Years");
            tvHeight.setText(userDetails.get("height") + " cm");
            tvWeight.setText(userDetails.get("weight") + " kg");
        }

        // --- PART B: 🟢 Load the Persistent Photo ---
        String savedUriString = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                .getString("profile_uri_" + currentUser, null);

        if (savedUriString != null) {
            try {
                Uri savedUri = Uri.parse(savedUriString);

                // 🟢 Force the ImageView to refresh by clearing it first
                ivProfilePic.setImageURI(null);
                ivProfilePic.setImageURI(savedUri);

            } catch (SecurityException e) {
                // This happens if the permission wasn't "Persisted" correctly
                ivProfilePic.setImageResource(R.drawable.ic_placeholder_user);
                android.util.Log.e("FITTRACK", "Permission denied for URI: " + savedUriString);
            } catch (Exception e) {
                ivProfilePic.setImageResource(R.drawable.ic_placeholder_user);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 🟢 Crucial: Refreshes the data if you return from the Edit screen
        loadUserProfile();
    }
}