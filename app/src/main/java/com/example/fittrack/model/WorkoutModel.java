package com.example.fittrack.model;

public class WorkoutModel {
    private String name;
    private String sets;
    private String muscleGroup;
    private String date;

    // 🟢 Constructor: Ensure this matches the order in DBHelper.java
    public WorkoutModel(String name, String sets, String muscleGroup, String date) {
        this.name = name;
        this.sets = sets;
        this.muscleGroup = muscleGroup;
        this.date = date;
    }

    // 🟢 Getters: These are what your WorkoutAdapter uses to fill the screen
    public String getWorkoutName() {
        return name != null ? name : "";
    }

    public String getSets() {
        return sets != null ? sets : "0";
    }

    public String getMuscleGroup() {
        return muscleGroup != null ? muscleGroup : "General";
    }

    public String getDate() {
        return date != null ? date : "";
    }
}