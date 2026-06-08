package com.example.fittrack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fittrack.model.DBHelper;
import java.util.ArrayList;

public class WeeklyPlannerActivity extends AppCompatActivity {

    // UI Elements (Changed from Spinner to TextView to match your green boxes)
    private TextView tvMon, tvTue, tvWed, tvThu, tvFri, tvSat, tvSun;
    private Button btnSave;
    private DBHelper dbHelper;
    private String currentUser;

    // Data for Multi-Select
    private final String[] muscleGroups = {"Chest", "Back", "Legs", "Shoulders", "Biceps", "Triceps", "Abs", "Cardio", "Rest Day"};

    // Arrays to keep track of checked items for each of the 7 days
    private boolean[][] selections = new boolean[8][muscleGroups.length]; // Index 1-7 for days

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_planner);

        dbHelper = new DBHelper(this);
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "User");

        // 1. Initialize TextViews (Ensure these IDs match your activity_weekly_planner.xml)
        initViews();

        // 2. Load Existing Data from DB
        loadCurrentSchedule();

        // 3. Set Click Listeners to open the Multi-Select Dialog
        setupClickListeners();

        btnSave = findViewById(R.id.btnSaveSchedule);
        btnSave.setOnClickListener(v -> saveSchedule());
    }

    private void initViews() {
        tvSun = findViewById(R.id.spinnerSun); // Keeping your IDs if you didn't change them in XML
        tvMon = findViewById(R.id.spinnerMon);
        tvTue = findViewById(R.id.spinnerTue);
        tvWed = findViewById(R.id.spinnerWed);
        tvThu = findViewById(R.id.spinnerThu);
        tvFri = findViewById(R.id.spinnerFri);
        tvSat = findViewById(R.id.spinnerSat);
    }

    private void setupClickListeners() {
        tvSun.setOnClickListener(v -> showMultiSelectDialog(1, tvSun));
        tvMon.setOnClickListener(v -> showMultiSelectDialog(2, tvMon));
        tvTue.setOnClickListener(v -> showMultiSelectDialog(3, tvTue));
        tvWed.setOnClickListener(v -> showMultiSelectDialog(4, tvWed));
        tvThu.setOnClickListener(v -> showMultiSelectDialog(5, tvThu));
        tvFri.setOnClickListener(v -> showMultiSelectDialog(6, tvFri));
        tvSat.setOnClickListener(v -> showMultiSelectDialog(7, tvSat));
    }

    private void showMultiSelectDialog(int dayIndex, TextView targetView) {
        // Refresh the selections array from the current text in the box
        // This ensures that if they edited but didn't save yet, the dialog is accurate
        parseSavedStringIntoArray(dayIndex, targetView.getText().toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Focus");

        builder.setMultiChoiceItems(muscleGroups, selections[dayIndex], (dialog, which, isChecked) -> {
            selections[dayIndex][which] = isChecked;
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            ArrayList<String> selectedList = new ArrayList<>();
            for (int i = 0; i < muscleGroups.length; i++) {
                if (selections[dayIndex][i]) {
                    selectedList.add(muscleGroups[i]);
                }
            }

            // 🟢 Logic: If "Rest Day" is selected with others, or nothing is selected
            String result;
            if (selectedList.isEmpty() || (selectedList.size() > 1 && selectedList.contains("Rest Day"))) {
                // If they picked "Rest Day" + something else, default to the something else
                selectedList.remove("Rest Day");
                if (selectedList.isEmpty()) result = "Rest Day";
                else result = android.text.TextUtils.join(", ", selectedList);
            } else {
                result = android.text.TextUtils.join(", ", selectedList);
            }

            targetView.setText(result);
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadCurrentSchedule() {
        TextView[] textViews = {null, tvSun, tvMon, tvTue, tvWed, tvThu, tvFri, tvSat};
        for (int i = 1; i <= 7; i++) {
            String savedMuscle = dbHelper.getScheduledMuscleForToday(currentUser, i);
            if (savedMuscle != null && !savedMuscle.isEmpty()) {
                textViews[i].setText(savedMuscle);
                // Sync the boolean array so the checkboxes are pre-checked
                parseSavedStringIntoArray(i, savedMuscle);
            } else {
                textViews[i].setText("Rest Day");
            }
        }
    }

    private void parseSavedStringIntoArray(int dayIndex, String savedString) {
        // 🟢 Step 1: Clear existing selections for this day first
        for (int i = 0; i < muscleGroups.length; i++) {
            selections[dayIndex][i] = false;
        }

        if (savedString == null || savedString.isEmpty() || savedString.equalsIgnoreCase("Rest Day")) {
            return;
        }

        // 🟢 Step 2: Split by comma and trim spaces
        String[] parts = savedString.split(",");
        for (String part : parts) {
            String trimmedPart = part.trim();
            for (int i = 0; i < muscleGroups.length; i++) {
                if (muscleGroups[i].equalsIgnoreCase(trimmedPart)) {
                    selections[dayIndex][i] = true;
                }
            }
        }
    }

    private void saveSchedule() {
        // Save each day (1=Sun, 2=Mon... 7=Sat)
        dbHelper.updateSchedule(currentUser, 1, tvSun.getText().toString());
        dbHelper.updateSchedule(currentUser, 2, tvMon.getText().toString());
        dbHelper.updateSchedule(currentUser, 3, tvTue.getText().toString());
        dbHelper.updateSchedule(currentUser, 4, tvWed.getText().toString());
        dbHelper.updateSchedule(currentUser, 5, tvThu.getText().toString());
        dbHelper.updateSchedule(currentUser, 6, tvFri.getText().toString());
        dbHelper.updateSchedule(currentUser, 7, tvSat.getText().toString());

        Toast.makeText(this, "Weekly Plan Updated!", Toast.LENGTH_SHORT).show();
        finish();
    }
}