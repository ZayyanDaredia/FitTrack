package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.fittrack.model.DBHelper;

import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    private View layoutEmptyState;
    private DBHelper dbHelper;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DBHelper(this);
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "");

        historyContainer = findViewById(R.id.historyContainer);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        View btnBack = findViewById(R.id.btnBackHistory);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHistory();
    }

    private void loadHistory() {
        historyContainer.removeAllViews();
        List<Map<String, String>> workouts = dbHelper.getUserWorkouts(currentUser);

        if (workouts == null || workouts.isEmpty()) {
            historyContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        historyContainer.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);

        // 🟢 THE FIX: Track the last ID, not the last muscle name
        long lastSessionId = -1;
        LinearLayout currentExerciseContainer = null;

        for (Map<String, String> workout : workouts) {
            String currentDate = workout.get("date");
            String currentMuscleGroup = workout.get("muscle_group");
            final String workoutId = workout.get("id");

            // 🟢 THE FIX: Get the session_id from the map
            String sidString = workout.get("session_id");
            long currentSessionId = (sidString != null) ? Long.parseLong(sidString) : -1;

            // 🟢 1. Create a NEW CARD only if the session_id is different
            if (currentSessionId != lastSessionId) {
                View card = inflater.inflate(R.layout.item_workout_card, historyContainer, false);

                TextView tvDate = card.findViewById(R.id.tvDate);
                TextView tvMuscle = card.findViewById(R.id.tvMuscleGroup);
                currentExerciseContainer = card.findViewById(R.id.exercisesContainer);

                tvDate.setText(currentDate);
                tvMuscle.setText(currentMuscleGroup != null ? currentMuscleGroup.toUpperCase() : "WORKOUT");

                historyContainer.addView(card);

                // 🟢 Update our marker
                lastSessionId = currentSessionId;
            }

            // 2. Add individual exercise rows inside the current card
            if (currentExerciseContainer != null) {
                View row = inflater.inflate(R.layout.item_workout, currentExerciseContainer, false);

                TextView tvName = row.findViewById(R.id.tvRowExercise);
                TextView tvSets = row.findViewById(R.id.tvRowSets);

                tvName.setText(workout.get("exercise_name"));

                String sets = workout.get("sets");
                String reps = workout.get("reps");
                String weight = workout.get("weight");

                String repsText = (reps != null && !reps.equals("0") && !reps.isEmpty()) ? " x " + reps : "";
                String weightText = (weight != null && !weight.equals("0") && !weight.isEmpty()) ? " (" + weight + "kg)" : "";

                tvSets.setText("Set " + sets + repsText + weightText);

                // 3. CLICK TO EDIT
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(HistoryActivity.this, EditWorkoutActivity.class);
                    intent.putExtra("workout_id", workoutId);
                    intent.putExtra("username", currentUser);
                    intent.putExtra("date", currentDate);
                    intent.putExtra("muscle_group", currentMuscleGroup);
                    // 🟢 Pass the session_id as a Long
                    intent.putExtra("session_id", currentSessionId);
                    startActivity(intent);
                });

                // 4. LONG PRESS TO DELETE
                row.setOnLongClickListener(v -> {
                    showDeleteDialog(workoutId, workout.get("exercise_name"));
                    return true;
                });

                currentExerciseContainer.addView(row);
            }
        }
    }

    private void showDeleteDialog(String id, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Delete this set for " + name + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (dbHelper.deleteWorkout(id)) {
                        Toast.makeText(this, "Workout deleted", Toast.LENGTH_SHORT).show();
                        loadHistory(); // Refresh the list
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}