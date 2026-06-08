package com.example.fittrack;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class SuggestedActivity extends AppCompatActivity {

    TabLayout tabLayout;
    LinearLayout exerciseContainer;
    Button btnShuffle, btnAddSelected;
    HashMap<String, List<String>> exerciseBank;
    String currentMuscle = "CHEST";

    // Stores strings formatted as "Exercise Name|Sets|Muscle"
    ArrayList<String> selectedExercises = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested);

        // Initialize UI
        tabLayout = findViewById(R.id.tabLayoutMuscles);
        exerciseContainer = findViewById(R.id.exerciseContainer);
        btnAddSelected = findViewById(R.id.btnBackSuggest);
        btnShuffle = findViewById(R.id.btnShuffle);

        loadExerciseData();

        // --- UPDATED BUTTON LOGIC (FIXED TRICEPS BUG) ---
        btnAddSelected.setOnClickListener(v -> {
            if (selectedExercises.isEmpty()) {
                finish();
            } else {
                HashSet<String> muscleSet = new HashSet<>();
                ArrayList<String> exercisesForIntent = new ArrayList<>();

                for (String item : selectedExercises) {
                    // Split "Name|Sets|Muscle"
                    String[] parts = item.split("\\|");
                    if (parts.length >= 3) {
                        String name = parts[0];
                        String sets = parts[1];
                        String muscleGroup = parts[2];

                        // Add only the specific muscle from the tab where it was picked
                        muscleSet.add(muscleGroup.toUpperCase());

                        // Re-format to "Name|Sets" for AddWorkoutActivity
                        exercisesForIntent.add(name + "|" + sets);
                    }
                }

                StringBuilder combinedMuscles = new StringBuilder();
                for (String m : muscleSet) {
                    combinedMuscles.append(m).append(" ");
                }

                Intent intent = new Intent(SuggestedActivity.this, AddWorkoutActivity.class);
                intent.putStringArrayListExtra("selected_list", exercisesForIntent);
                intent.putExtra("suggested_muscle", combinedMuscles.toString().trim());

                startActivity(intent);
                finish();
            }
        });

        // Setup Tabs
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.addTab(tabLayout.newTab().setText("CHEST"));
        tabLayout.addTab(tabLayout.newTab().setText("TRICEPS"));
        tabLayout.addTab(tabLayout.newTab().setText("BACK"));
        tabLayout.addTab(tabLayout.newTab().setText("BICEPS"));
        tabLayout.addTab(tabLayout.newTab().setText("SHOULDERS"));
        tabLayout.addTab(tabLayout.newTab().setText("LEGS"));
        tabLayout.addTab(tabLayout.newTab().setText("CORE"));
        tabLayout.addTab(tabLayout.newTab().setText("CARDIO"));

        displayRandomExercises("CHEST");

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentMuscle = tab.getText().toString();
                displayRandomExercises(currentMuscle);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        btnShuffle.setOnClickListener(v -> displayRandomExercises(currentMuscle));
        updateButtonText();
    }

    private void loadExerciseData() {
        exerciseBank = new HashMap<>();
        addCategory("CHEST", new String[]{"Pushups: 3x15", "Bench Press: 3x10", "Incline Press: 3x8", "Dumbbell Flys: 3x12", "Dips: 3x12"});
        addCategory("BACK", new String[]{"Pull-ups: 3xMax", "Lat Pulldowns: 3x10", "Deadlifts: 3x5", "Bent Rows: 3x12", "Single Arm Rows: 3x10"});
        addCategory("LEGS", new String[]{"Squats: 4x12", "Lunges: 3x15", "Leg Press: 3x15", "Leg Curls: 3x12", "Calf Raises: 4x20"});
        addCategory("TRICEPS", new String[]{"Pushdowns: 3x15", "Skull Crushers: 3x10", "Extensions: 3x12", "Diamond Pushups: 3x10", "Bench Dips: 3x15"});
        addCategory("BICEPS", new String[]{"Dumbbell Curls: 3x12", "Hammer Curls: 3x12", "Preacher Curls: 3x10", "Concentration Curls: 3x12", "Barbell Curls: 3x10"});
        addCategory("SHOULDERS", new String[]{"Shoulder Press: 3x10", "Lateral Raises: 3x15", "Front Raises: 3x12", "Rear Delt Flys: 3x15", "Arnold Press: 3x10"});
        addCategory("CORE", new String[]{"Planks: 3x1Min", "Crunches: 3x20", "Leg Raises: 3x15", "Russian Twists: 3x20", "Mountain Climbers: 3x30s"});
        addCategory("CARDIO", new String[]{"Treadmill Run: 20 Mins", "Cycling: 30 Mins", "Jump Rope: 5x2 Mins", "Stair Climber: 15 Mins", "Rowing Machine: 10 Mins", "Burpees: 3x15", "High Knees: 3x1 Min", "Elliptical: 20 Mins"});
    }

    private void addCategory(String key, String[] exercises) {
        List<String> list = new ArrayList<>();
        Collections.addAll(list, exercises);
        exerciseBank.put(key, list);
    }

    private void displayRandomExercises(String muscle) {
        exerciseContainer.removeAllViews();
        List<String> list = exerciseBank.get(muscle);

        if (list != null) {
            List<String> shuffledList = new ArrayList<>(list);
            Collections.shuffle(shuffledList);
            for (int i = 0; i < 5 && i < shuffledList.size(); i++) {
                inflateExerciseCard(shuffledList.get(i), muscle);
            }
        }
    }

    private void inflateExerciseCard(String details, String muscle) {
        View itemView = getLayoutInflater().inflate(R.layout.item_suggestion, exerciseContainer, false);

        TextView tvMuscleTag = itemView.findViewById(R.id.tvMuscleTag);
        TextView tvExerciseName = itemView.findViewById(R.id.tvExerciseName);
        TextView tvTargetDetails = itemView.findViewById(R.id.tvTargetDetails);
        CheckBox cbSelect = itemView.findViewById(R.id.cbSelectExercise);

        tvMuscleTag.setText(muscle);

        final String exerciseName;
        final String exerciseSets;

        if (details.contains(":")) {
            String[] parts = details.split(":");
            exerciseName = parts[0].trim();
            exerciseSets = parts[1].trim();
        } else {
            exerciseName = details;
            exerciseSets = "3 Sets";
        }

        tvExerciseName.setText(exerciseName);
        tvTargetDetails.setText("Target: " + exerciseSets);

        // 🟢 Includes muscle group in the saved string
        String combinedData = exerciseName + "|" + exerciseSets + "|" + muscle;

        if (selectedExercises.contains(combinedData)) {
            cbSelect.setChecked(true);
        }

        cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedExercises.contains(combinedData)) {
                    selectedExercises.add(combinedData);
                }
            } else {
                selectedExercises.remove(combinedData);
            }
            updateButtonText();
        });

        exerciseContainer.addView(itemView);
    }

    private void updateButtonText() {
        if (selectedExercises.isEmpty()) {
            btnAddSelected.setText("BACK TO DASHBOARD");
        } else {
            btnAddSelected.setText("ADD SELECTED (" + selectedExercises.size() + ")");
        }
    }
}