package com.example.fittrack.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DBNAME = "FitTrack.db";
    public static final int DB_VERSION = 9;

    public DBHelper(Context context) {
        super(context, DBNAME, null, DB_VERSION);
    }

    @Override
    public  void onCreate(SQLiteDatabase MyDB) {
        MyDB.execSQL("create Table users(" +
                "username TEXT PRIMARY KEY, " +
                "password TEXT, " +
                "fullname TEXT, " +
                "email TEXT, " +
                "age TEXT, " +
                "height TEXT, " +
                "weight TEXT, " +
                "registration_date TEXT)");

        // Inside onCreate in DBHelper.java
        MyDB.execSQL("create Table workouts(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "session_id INTEGER, " + // 🟢 ADD THIS COLUMN
                "username TEXT, " +
                "workoutname TEXT, " +
                "sets TEXT, " +
                "reps TEXT, " +
                "weight TEXT, " +
                "muscle_group TEXT, " +
                "date TEXT)");

        MyDB.execSQL("create Table bug_reports(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category TEXT, " +
                "description TEXT)");

        MyDB.execSQL("create Table workout_schedule(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT, " +
                "day_of_week INTEGER, " +
                "muscle_group TEXT, " +
                "UNIQUE(username, day_of_week) ON CONFLICT REPLACE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase MyDB, int oldVersion, int newVersion) {
        MyDB.execSQL("drop Table if exists users");
        MyDB.execSQL("drop Table if exists workouts");
        MyDB.execSQL("drop Table if exists bug_reports");
        MyDB.execSQL("drop Table if exists workout_schedule");
        onCreate(MyDB);
    }

    // --- 🟢 NEW: SESSION-BASED EDITING METHODS ---

    public List<Map<String, String>> getExercisesForWorkout(String workoutId) {
        List<Map<String, String>> exerciseList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT workoutname, sets FROM workouts WHERE id = ?", new String[]{workoutId});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();
                map.put("exercise_name", cursor.getString(0));
                map.put("sets", cursor.getString(1));
                exerciseList.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return exerciseList;
    }

    public void deleteWorkoutSession(String username, String date, String muscleGroup) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("workouts", "username = ? AND date = ? AND muscle_group = ?",
                new String[]{username, date, muscleGroup});
    }

    // 🟢 UPDATE: Include session_id in your insertion
    public void insertUpdatedExercise(long sessionId, String user, String name, String sets, String muscle, String date) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", sessionId); // 🟢 The "Identity"
        cv.put("username", user);
        cv.put("workoutname", name);
        cv.put("sets", sets);
        cv.put("muscle_group", muscle);
        cv.put("date", date);
        db.insert("workouts", null, cv);
    }

    // --- BUG REPORTING ---
    public boolean insertBugReport(String category, String description) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("category", category);
        cv.put("description", description);
        long result = db.insert("bug_reports", null, cv);
        return result != -1;
    }

    // --- USER AUTH & PROFILE ---
    public Boolean insertData(String user, String pass, String name, String email, String age, String height, String weight, String regDate) {
        SQLiteDatabase MyDB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("username", user);
        contentValues.put("password", pass);
        contentValues.put("fullname", name);
        contentValues.put("email", email);
        contentValues.put("age", age);
        contentValues.put("height", height);
        contentValues.put("weight", weight);
        contentValues.put("registration_date", regDate);
        long result = MyDB.insert("users", null, contentValues);
        return result != -1;
    }

    public boolean updateProfile(String username, String name, String email, String age, String height, String weight) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("fullname", name);
        cv.put("email", email);
        cv.put("age", age);
        cv.put("height", height);
        cv.put("weight", weight);
        int result = db.update("users", cv, "username = ?", new String[]{username});
        return result > 0;
    }

    public Map<String, String> getFullUserDetails(String identifier) {
        Map<String, String> details = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();

        android.util.Log.d("APP_DEBUG", "Checking DB for identifier: [" + identifier + "]");

        // 🟢 SMART QUERY: Try to match the ID against username OR fullname OR email
        // This covers all bases in case you registered differently
        String query = "SELECT * FROM users WHERE LOWER(username) = LOWER(?) " +
                "OR LOWER(fullname) = LOWER(?) " +
                "OR LOWER(email) = LOWER(?) LIMIT 1";

        Cursor cursor = db.rawQuery(query, new String[]{identifier, identifier, identifier});

        if (cursor != null && cursor.moveToFirst()) {
            details.put("fullname", cursor.getString(cursor.getColumnIndexOrThrow("fullname")));
            details.put("email", cursor.getString(cursor.getColumnIndexOrThrow("email")));
            details.put("age", cursor.getString(cursor.getColumnIndexOrThrow("age")));
            details.put("height", cursor.getString(cursor.getColumnIndexOrThrow("height")));
            details.put("weight", cursor.getString(cursor.getColumnIndexOrThrow("weight")));

            android.util.Log.d("APP_DEBUG", "🟢 MATCH FOUND! Correct Email: " + details.get("email"));
        } else {
            android.util.Log.e("APP_DEBUG", "🔴 TOTAL FAILURE: No record found for " + identifier);
        }

        if (cursor != null) cursor.close();
        return details;
    }

    // --- WORKOUT MANAGEMENT ---

    // 🟢 Update your insert logic to include sessionId
    public boolean insertWorkout(long sessionId, String name, String sets, String reps, String weight, String muscle, String date, String user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("session_id", sessionId); // 🟢 The "Fingerprint"
        cv.put("workoutname", name);
        cv.put("sets", sets);
        cv.put("reps", reps);
        cv.put("weight", weight);
        cv.put("muscle_group", muscle);
        cv.put("date", date);
        cv.put("username", user);
        return db.insert("workouts", null, cv) != -1;
    }


    public boolean updateWorkout(String id, String name, String sets, String muscles) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("workoutname", name);
        cv.put("sets", sets);
        cv.put("muscle_group", muscles);
        int result = db.update("workouts", cv, "id = ?", new String[]{id});
        return result > 0;
    }

    public boolean deleteWorkout(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("workouts", "id = ?", new String[]{id}) > 0;
    }

    // 🟢 NEW: Delete all exercises belonging to a specific unique session
    // 🟢 Add this to DBHelper.java
    public void deleteBySessionId(long sessionId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // 🟢 ONLY delete by session_id!
        db.delete("workouts", "session_id = ?", new String[]{String.valueOf(sessionId)});
    }

    public boolean hasWorkoutOnDate(String username, String date) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT id FROM workouts WHERE username = ? AND TRIM(SUBSTR(date, 1, 10)) = ? LIMIT 1";
        Cursor cursor = db.rawQuery(query, new String[]{username, date.trim()});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    public int getUniqueSessionCountAllTime(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(DISTINCT SUBSTR(date, 1, 10)) FROM workouts WHERE username = ?", new String[]{username});
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // --- STATISTICS HELPER ---
    public Map<String, Integer> getStatsAllTime(String username) {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT muscle_group FROM workouts WHERE username = ?", new String[]{username});
        return processMuscleStats(cursor, stats);
    }

    private Map<String, Integer> processMuscleStats(Cursor cursor, Map<String, Integer> stats) {
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String muscleGroupString = cursor.getString(0);
                if (muscleGroupString != null) {
                    String[] muscles = muscleGroupString.split("[\\s,]+");
                    for (String m : muscles) {
                        String cleaned = m.trim().toUpperCase();
                        if (!cleaned.isEmpty()) {
                            stats.put(cleaned, stats.getOrDefault(cleaned, 0) + 1);
                        }
                    }
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        return stats;
    }

    public Boolean checkusernamepassword(String username, String password) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ? and password = ?", new String[]{username, password});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public Map<String, Integer> getStatsByDateRange(String username, String startDate, String endDate) {
        Map<String, Integer> stats = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT muscle_group FROM workouts WHERE username = ? AND date BETWEEN ? AND ?",
                new String[]{username, startDate, endDate}
        );
        return processMuscleStats(cursor, stats);
    }

    public int getUniqueSessionCount(String username, String startDate, String endDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT date) FROM workouts WHERE username = ? AND date BETWEEN ? AND ?",
                new String[]{username, startDate, endDate}
        );
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        return count;
    }

    public Boolean checkusername(String username) {
        SQLiteDatabase MyDB = this.getReadableDatabase();
        Cursor cursor = MyDB.rawQuery("Select * from users where username = ?", new String[]{username});
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    public List<Map<String, String>> getUserWorkouts(String username) {
        List<Map<String, String>> workoutList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        // 🟢 Change the ORDER BY to session_id DESC
        Cursor cursor = db.rawQuery("SELECT * FROM workouts WHERE username = ? ORDER BY session_id DESC, id ASC", new String[]{username});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> workout = new HashMap<>();
                workout.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")));

                // 🟢 ADD THIS LINE so HistoryActivity can group by Session ID
                workout.put("session_id", cursor.getString(cursor.getColumnIndexOrThrow("session_id")));

                workout.put("date", cursor.getString(cursor.getColumnIndexOrThrow("date")));
                workout.put("muscle_group", cursor.getString(cursor.getColumnIndexOrThrow("muscle_group")));
                workout.put("exercise_name", cursor.getString(cursor.getColumnIndexOrThrow("workoutname")));
                workout.put("sets", cursor.getString(cursor.getColumnIndexOrThrow("sets")));
                workout.put("reps", cursor.getString(cursor.getColumnIndexOrThrow("reps")));
                workout.put("weight", cursor.getString(cursor.getColumnIndexOrThrow("weight")));
                workoutList.add(workout);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return workoutList;
    }

    public List<Map<String, String>> getWorkoutsByDateRange(String username, String startDate, String endDate) {
        List<Map<String, String>> workoutList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM workouts WHERE username = ? ORDER BY id DESC";
        Cursor cursor = db.rawQuery(query, new String[]{username});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> workout = new HashMap<>();
                workout.put("id", cursor.getString(cursor.getColumnIndexOrThrow("id")));
                workout.put("date", cursor.getString(cursor.getColumnIndexOrThrow("date")));
                workout.put("muscle_group", cursor.getString(cursor.getColumnIndexOrThrow("muscle_group")));
                workout.put("exercise_name", cursor.getString(cursor.getColumnIndexOrThrow("workoutname")));
                workout.put("sets", cursor.getString(cursor.getColumnIndexOrThrow("sets")));
                workout.put("reps", cursor.getString(cursor.getColumnIndexOrThrow("reps")));
                workout.put("weight", cursor.getString(cursor.getColumnIndexOrThrow("weight")));
                workoutList.add(workout);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return workoutList;
    }

    // 🟢 RECOVERY LOGIC ENGINE
    public long getHoursSinceLastWorkout(String username, String muscleGroup) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT date FROM workouts WHERE username = ? AND muscle_group LIKE ? ORDER BY id DESC LIMIT 1",
                new String[]{username, "%" + muscleGroup + "%"}
        );

        if (cursor.moveToFirst()) {
            String dateString = cursor.getString(0);
            cursor.close();
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                java.util.Date lastDate = sdf.parse(dateString);
                if (lastDate != null) {
                    long diffInMillis = System.currentTimeMillis() - lastDate.getTime();
                    return diffInMillis / (1000 * 60 * 60);
                }
            } catch (Exception e) {
                return 999;
            }
        }
        if (cursor != null) cursor.close();
        return 999;
    }

    // --- SCHEDULE MANAGEMENT ---
    public void updateSchedule(String username, int dayId, String muscleGroup) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("username", username);
        values.put("day_of_week", dayId);
        values.put("muscle_group", muscleGroup);
        db.replace("workout_schedule", null, values);
    }

    public String getScheduledMuscleForToday(String username, int dayOfWeek) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT muscle_group FROM workout_schedule WHERE username = ? AND day_of_week = ?",
                new String[]{username, String.valueOf(dayOfWeek)});

        String muscle = "Rest Day";
        if (cursor.moveToFirst()) {
            muscle = cursor.getString(0);
        }
        cursor.close();
        return muscle;
    }

    public String getLastTrainedDate(String username, String muscleGroup) {
        SQLiteDatabase db = this.getReadableDatabase();
        String lastDate = null;
        Cursor cursor = db.rawQuery("SELECT date FROM workouts WHERE username=? AND muscle_group=? ORDER BY date DESC LIMIT 1",
                new String[]{username, muscleGroup});

        if (cursor.moveToFirst()) {
            lastDate = cursor.getString(0);
        }
        cursor.close();
        return lastDate;
    }
    public List<Map<String, String>> getExercisesBySessionId(long sessionId) {
        List<Map<String, String>> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 🟢 The "Fingerprint" Query: This ensures NO merging happens
        Cursor cursor = db.rawQuery("SELECT * FROM workouts WHERE session_id=?",
                new String[]{String.valueOf(sessionId)});

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> map = new HashMap<>();

                // Map 'workoutname' from DB to 'exercise_name' for the Activity
                map.put("exercise_name", cursor.getString(cursor.getColumnIndexOrThrow("workoutname")));
                map.put("sets", cursor.getString(cursor.getColumnIndexOrThrow("sets")));
                map.put("reps", cursor.getString(cursor.getColumnIndexOrThrow("reps")));
                map.put("weight", cursor.getString(cursor.getColumnIndexOrThrow("weight")));

                list.add(map);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }
}