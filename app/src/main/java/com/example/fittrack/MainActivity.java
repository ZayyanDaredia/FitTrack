package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.model.DBHelper;

public class MainActivity extends AppCompatActivity {

    private EditText username, password;
    private Button btnLogin, btnSignup;
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- SESSION CHECK: Auto-Login logic ---
        // 🟢 SYNCED: Using "LoginPrefs" to match DashboardActivity's logout logic
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        if (pref.getBoolean("isLoggedIn", false)) {
            // If already logged in, skip this screen and go to Dashboard
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);

        // LOGIN BUTTON LOGIC
        btnLogin.setOnClickListener(v -> {
            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            } else {
                Boolean checkuserpass = dbHelper.checkusernamepassword(user, pass);
                if (checkuserpass) {
                    // 🟢 SYNCED: Save session in "LoginPrefs"
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.putString("username", user);
                    editor.apply();

                    Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // SIGNUP BUTTON LOGIC
        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}