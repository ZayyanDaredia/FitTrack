package com.example.fittrack;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.fittrack.model.DBHelper;
import java.util.List;
import java.util.Map;
import android.content.SharedPreferences;
import android.widget.CheckBox;

public class EditWorkoutActivity extends AppCompatActivity {

    private LinearLayout exerciseContainer;
    private DBHelper dbHelper;
    private String workoutId; // Unique ID for the session

    private String currentUser; // 🟢 ADD THIS LINE HERE

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_workout);

        dbHelper = new DBHelper(this);
        exerciseContainer = findViewById(R.id.exerciseContainer);

        // 1. Initialize user session
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "User");

        // 2. 🟢 THE FIX: Pre-check the muscle group boxes from the Intent
        String existingMuscles = getIntent().getStringExtra("muscle_group");
        checkMuscleGroup(existingMuscles);

        // 3. Load the specific exercises using the session_id fingerprint
        // Ensure you store the ID so handleUpdate() can use it later
        loadExistingExercises();

        // 4. Setup "Add More" logic
        Button btnAddMore = findViewById(R.id.btnAddMore);
        btnAddMore.setOnClickListener(v -> addExerciseRow("", ""));

        // 5. Setup "Update" logic
        Button btnUpdate = findViewById(R.id.btnUpdateWorkout);
        btnUpdate.setOnClickListener(v -> handleUpdate());
    }

    private String getSelectedMuscles() {
        StringBuilder muscles = new StringBuilder();
        int[] checkboxIds = {R.id.cbChest, R.id.cbBack, R.id.cbTriceps, R.id.cbBiceps,
                R.id.cbShoulders, R.id.cbLegs, R.id.cbCore, R.id.cbCardio};

        for (int id : checkboxIds) {
            CheckBox cb = findViewById(id);
            // 🟢 ADD THIS NULL CHECK
            if (cb != null && cb.isChecked()) {
                if (muscles.length() > 0) muscles.append(", ");
                muscles.append(cb.getText().toString().toUpperCase());
            }
        }
        return muscles.length() > 0 ? muscles.toString() : "REST DAY";
    }

    private void checkMuscleGroup(String muscleString) {
        if (muscleString == null) return;
        String m = muscleString.toUpperCase();

        // We find each checkbox individually and check if it's null before setting
        CheckBox cbChest = findViewById(R.id.cbChest);
        if (cbChest != null && m.contains("CHEST")) cbChest.setChecked(true);

        CheckBox cbBack = findViewById(R.id.cbBack);
        if (cbBack != null && m.contains("BACK")) cbBack.setChecked(true);

        CheckBox cbTriceps = findViewById(R.id.cbTriceps);
        if (cbTriceps != null && m.contains("TRICEPS")) cbTriceps.setChecked(true);

        CheckBox cbBiceps = findViewById(R.id.cbBiceps);
        if (cbBiceps != null && m.contains("BICEPS")) cbBiceps.setChecked(true);

        CheckBox cbShoulders = findViewById(R.id.cbShoulders);
        if (cbShoulders != null && m.contains("SHOULDERS")) cbShoulders.setChecked(true);

        CheckBox cbLegs = findViewById(R.id.cbLegs);
        if (cbLegs != null && m.contains("LEGS")) cbLegs.setChecked(true);

        CheckBox cbCore = findViewById(R.id.cbCore);
        if (cbCore != null && m.contains("CORE")) cbCore.setChecked(true);

        CheckBox cbCardio = findViewById(R.id.cbCardio);
        if (cbCardio != null && m.contains("CARDIO")) cbCardio.setChecked(true);
    }

    private void loadExistingExercises() {
        // 1. Get the session_id from the Intent (passed from HistoryActivity)
        // Use -1 as a default value
        long sessionId = getIntent().getLongExtra("session_id", -1);

        exerciseContainer.removeAllViews();

        // 2. 🟢 CHANGE THIS LINE: Call the ID-based method
        List<Map<String, String>> sessionExercises = dbHelper.getExercisesBySessionId(sessionId);

        if (sessionExercises != null && !sessionExercises.isEmpty()) {
            for (Map<String, String> ex : sessionExercises) {
                addExerciseRow(
                        ex.get("exercise_name"),
                        ex.get("sets")
                );
            }
        } else {
            addExerciseRow("", "");
        }
    }

    private void addExerciseRow(String name, String sets) {
        // 🟢 MAKE SURE this layout file name is correct!
        View row = getLayoutInflater().inflate(R.layout.item_exercise_row, null);

        // 🟢 CRITICAL: Check your item_exercise_row.xml.
        // Are the IDs actually etExerciseName and etSets?
        // If you used different names in the XML, change them here:
        EditText etName = row.findViewById(R.id.etExerciseName);
        EditText etSets = row.findViewById(R.id.etSets);
        ImageButton btnRemove = row.findViewById(R.id.btnRemoveRow);

        if (etName != null) {
            etName.setText(name);
        } else {
            android.util.Log.e("EDIT_DEBUG", "etExerciseName is NULL. Check your XML IDs!");
        }

        if (etSets != null) {
            etSets.setText(sets);
        }

        btnRemove.setOnClickListener(v -> exerciseContainer.removeView(row));
        exerciseContainer.addView(row);
    }

    private void handleUpdate() {
        // 1. Retrieve the session fingerprint
        long sessionId = getIntent().getLongExtra("session_id", -1L);

        // Safety Check: If sessionId is missing, we can't update safely
        if (sessionId == -1L) {
            Toast.makeText(this, "Error: Session ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        String date = getIntent().getStringExtra("date");
        String newMuscleGroup = getSelectedMuscles();

        // 🟢 STEP 1: Delete ONLY the exercises belonging to THIS ID
        dbHelper.deleteBySessionId(sessionId);

        // 🟢 STEP 2: Re-insert with the EXACT SAME sessionId
        for (int i = 0; i < exerciseContainer.getChildCount(); i++) {
            View row = exerciseContainer.getChildAt(i);
            EditText etName = row.findViewById(R.id.etExerciseName);
            EditText etSets = row.findViewById(R.id.etSets);

            String name = etName.getText().toString().trim();
            String sets = etSets.getText().toString().trim();

            if (!name.isEmpty()) {
                // This MUST use the sessionId passed from the Intent
                dbHelper.insertUpdatedExercise(sessionId, currentUser, name, sets, newMuscleGroup, date);
            }
        }

        Toast.makeText(this, "Workout Updated!", Toast.LENGTH_SHORT).show();
        finish();
    }
}