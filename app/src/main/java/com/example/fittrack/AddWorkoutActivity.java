package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.model.DBHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AddWorkoutActivity extends AppCompatActivity {

    private LinearLayout containerExercises;
    private Button btnAddMore, btnSaveWorkout;
    private TextView tvTitle;
    private DBHelper db;
    private String currentUser;

    // Edit Mode Variables
    private boolean isEditMode = false;
    private String workoutIdToEdit = "";

    private CheckBox cbChest, cbTriceps, cbBack, cbBiceps, cbShoulders, cbLegs, cbCore, cbCardio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_workout);

        db = new DBHelper(this);
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "User");

        // Initialize UI Elements
        containerExercises = findViewById(R.id.container_exercises);
        btnAddMore = findViewById(R.id.btnAddMore);
        btnSaveWorkout = findViewById(R.id.btnSaveWorkout);
        tvTitle = findViewById(R.id.tvAddWorkoutTitle); // Ensure this ID exists in your XML for the header

        initCheckBoxes();

        Intent intent = getIntent();

        // 🟢 CHECK FOR EDIT MODE
        if (intent != null && intent.getBooleanExtra("is_edit_mode", false)) {
            setupEditMode(intent);
        } else {
            setupNormalMode(intent);
        }

        btnAddMore.setOnClickListener(v -> addNewExerciseRow("", ""));
        btnSaveWorkout.setOnClickListener(v -> handleSaveOrUpdate());
    }

    private void initCheckBoxes() {
        cbChest = findViewById(R.id.cbChest);
        cbTriceps = findViewById(R.id.cbTriceps);
        cbBack = findViewById(R.id.cbBack);
        cbBiceps = findViewById(R.id.cbBiceps);
        cbShoulders = findViewById(R.id.cbShoulders);
        cbLegs = findViewById(R.id.cbLegs);
        cbCore = findViewById(R.id.cbCore);
        cbCardio = findViewById(R.id.cbCardio);
    }

    private void setupEditMode(Intent intent) {
        isEditMode = true;
        workoutIdToEdit = intent.getStringExtra("workout_id");

        if (tvTitle != null) tvTitle.setText("Edit Workout");
        btnSaveWorkout.setText("UPDATE WORKOUT");
        btnAddMore.setVisibility(View.GONE); // Hide "Add More" when editing a specific record

        String name = intent.getStringExtra("exercise_name");
        String sets = intent.getStringExtra("sets");
        String muscleGroup = intent.getStringExtra("muscle_group");

        checkMuscleGroup(muscleGroup);
        addNewExerciseRow(name, sets);
    }

    private void setupNormalMode(Intent intent) {
        if (intent != null && intent.hasExtra("suggested_muscle")) {
            checkMuscleGroup(intent.getStringExtra("suggested_muscle"));
        }

        if (intent != null && intent.hasExtra("selected_list")) {
            ArrayList<String> selectedList = intent.getStringArrayListExtra("selected_list");
            if (selectedList != null && !selectedList.isEmpty()) {
                containerExercises.removeAllViews();
                for (String item : selectedList) {
                    String[] parts = item.split("\\|");
                    if (parts.length == 2) addNewExerciseRow(parts[0], parts[1]);
                }
            }
        } else if (intent != null && intent.hasExtra("exercise_name")) {
            addNewExerciseRow(intent.getStringExtra("exercise_name"), intent.getStringExtra("sets"));
        } else {
            addNewExerciseRow("", "");
        }
    }

    private void addNewExerciseRow(String name, String sets) {
        View row = getLayoutInflater().inflate(R.layout.item_add_exercise_row, null);

        EditText etName = row.findViewById(R.id.etExerciseName);
        EditText etSets = row.findViewById(R.id.etSets);
        ImageButton btnDelete = row.findViewById(R.id.btnDeleteRow);

        etName.setText(name);
        etSets.setText(sets);

        if (btnDelete != null) {
            // Hide delete button in edit mode to prevent confusion
            if (isEditMode) btnDelete.setVisibility(View.GONE);

            btnDelete.setOnClickListener(v -> {
                if (containerExercises.getChildCount() > 1) {
                    containerExercises.removeView(row);
                } else {
                    Toast.makeText(this, "Keep at least one exercise", Toast.LENGTH_SHORT).show();
                }
            });
        }
        containerExercises.addView(row);
    }

    private void handleSaveOrUpdate() {
        if (isEditMode) {
            updateExistingWorkout();
        } else {
            saveNewWorkouts();
        }
    }

    private void updateExistingWorkout() {
        View row = containerExercises.getChildAt(0);
        EditText etName = row.findViewById(R.id.etExerciseName);
        EditText etSets = row.findViewById(R.id.etSets);

        String name = etName.getText().toString().trim();
        String sets = etSets.getText().toString().trim();

        if (!name.isEmpty() && !sets.isEmpty()) {
            // 🟢 You need to add updateWorkout() to your DBHelper
            boolean success = db.updateWorkout(workoutIdToEdit, name, sets, getSelectedMuscles());
            if (success) {
                Toast.makeText(this, "Workout Updated!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveNewWorkouts() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String cleanDate = sdf.format(new Date());

        // 🟢 THE FIX: Generate ONE unique ID for this entire group/card
        long sessionId = System.currentTimeMillis();
        String selectedMuscles = getSelectedMuscles();

        int savedCount = 0;

        // Loop through the exercises you added on the screen
        for (int i = 0; i < containerExercises.getChildCount(); i++) {
            View row = containerExercises.getChildAt(i);
            EditText etName = row.findViewById(R.id.etExerciseName);
            EditText etSets = row.findViewById(R.id.etSets);

            if (etName != null && etSets != null) {
                String workoutName = etName.getText().toString().trim();
                String setsValue = etSets.getText().toString().trim();

                if (!workoutName.isEmpty()) {
                    // 🟢 ALL exercises in this loop get the SAME sessionId
                    db.insertWorkout(
                            sessionId,
                            workoutName,
                            setsValue,
                            "0", "0",       // Default reps/weight
                            selectedMuscles,
                            cleanDate,
                            currentUser
                    );
                    savedCount++;
                }
            }
        }

        if (savedCount > 0) {
            Toast.makeText(this, "Saved " + savedCount + " exercises!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Please fill in details", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkMuscleGroup(String muscleString) {
        if (muscleString == null) return;
        String m = muscleString.toUpperCase();
        if (m.contains("CHEST")) cbChest.setChecked(true);
        if (m.contains("TRICEPS")) cbTriceps.setChecked(true);
        if (m.contains("BACK")) cbBack.setChecked(true);
        if (m.contains("BICEPS")) cbBiceps.setChecked(true);
        if (m.contains("SHOULDERS")) cbShoulders.setChecked(true);
        if (m.contains("LEGS")) cbLegs.setChecked(true);
        if (m.contains("CORE")) cbCore.setChecked(true);
        if (m.contains("CARDIO")) cbCardio.setChecked(true);
    }

    private String getSelectedMuscles() {
        StringBuilder sb = new StringBuilder();
        if (cbChest.isChecked()) sb.append("Chest ");
        if (cbTriceps.isChecked()) sb.append("Triceps ");
        if (cbBack.isChecked()) sb.append("Back ");
        if (cbBiceps.isChecked()) sb.append("Biceps ");
        if (cbShoulders.isChecked()) sb.append("Shoulders ");
        if (cbLegs.isChecked()) sb.append("Legs ");
        if (cbCore.isChecked()) sb.append("Core ");
        if (cbCardio.isChecked()) sb.append("Cardio ");
        return sb.toString().trim();
    }
}