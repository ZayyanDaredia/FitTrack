package com.example.fittrack.model;

public class Workout {
    private String title;
    private String muscleGroup;
    private String sets;
    private String date;

    // 1. The Constructor
    public Workout(String title, String muscleGroup, String sets, String date) {
        this.title = title;
        this.muscleGroup = muscleGroup;
        this.sets = sets;
        this.date = date;
    }

    // 2. The missing "Getter" method causing your error
    public String getTitle() {
        return title;
    }

    // 3. Other necessary "Getter" methods for the Adapter
    public String getMuscleGroup() {
        return muscleGroup;
    }

    public String getSets() {
        return sets;
    }

    public String getDate() {
        return date;
    }
}