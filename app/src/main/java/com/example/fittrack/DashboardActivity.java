package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.fittrack.model.DBHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class DashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private TextView tvActiveDaysText;
    private TextView tvMon, tvTue, tvWed, tvThu, tvFri, tvSat, tvSun;

    // Tomorrow's Card
    private TextView tvTomorrowDay, tvTomorrowMuscle;
    private ImageView ivTomorrowIcon;

    // Hydration UI
    private LinearProgressIndicator waterProgress;
    private TextView tvWaterPercent, tvWaterStatus;
    private boolean isLiterMode = false;

    // Health Tip UI
    private TextView tvHealthTip;
    private final String[] healthTips = {
            "Drinking water during your workout can prevent a 10% drop in performance.",
            "Muscles grow during rest, not during the workout. Don't skip sleep!",
            "Consistency beats intensity. 30 minutes every day is better than 3 hours once a week.",
            "Creatine is one of the most researched supplements for increasing strength and muscle mass.",
            "Progressive overload (adding weight or reps) is the key to long-term muscle growth.",
            "Post-workout protein helps repair muscle fibers torn during exercise.",
            "Proper form reduces injury risk and ensures the target muscle is doing the work.",
            "A 10-minute dynamic warm-up increases blood flow and joint mobility.",
            "Black coffee can act as a natural pre-workout by increasing focus and energy."
    };

    private DBHelper dbHelper;
    private String currentUser;
    private String scheduledMuscle = "Rest Day";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        dbHelper = new DBHelper(this);
        SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = loginPrefs.getString("username", "User");

        SharedPreferences healthPrefs = getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE);
        isLiterMode = healthPrefs.getBoolean("is_liter_mode", false);

        // 1. Initialize Drawer and Menu
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            setupNavigationHeader(navigationView);
        }

        ImageButton btnMenu = findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        }

        // 2. Initialize UI Elements
        tvActiveDaysText = findViewById(R.id.tvActiveDaysText);
        initWeeklyTrackerViews();

        tvTomorrowDay = findViewById(R.id.tvTomorrowDay);
        tvTomorrowMuscle = findViewById(R.id.tvTomorrowMuscle);
        ivTomorrowIcon = findViewById(R.id.ivTomorrowIcon);

        waterProgress = findViewById(R.id.waterProgress);
        tvWaterPercent = findViewById(R.id.tvWaterPercent);
        tvWaterStatus = findViewById(R.id.tvWaterStatus);

        // Health Tip Initialization
        tvHealthTip = findViewById(R.id.tvHealthTip);

        // 3. Setup Hydration
        setupHydrationListeners();

        // 4. Setup Edit Schedule Button
        ImageButton btnEditSchedule = findViewById(R.id.btnEditSchedule);
        if (btnEditSchedule != null) {
            btnEditSchedule.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, WeeklyPlannerActivity.class));
            });
        }

        // 5. Setup "Start Workout" Button
        Button btnStartWorkout = findViewById(R.id.btnStartWorkout);
        if (btnStartWorkout != null) {
            btnStartWorkout.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, AddWorkoutActivity.class);
                intent.putExtra("suggested_muscle", scheduledMuscle);
                startActivity(intent);
            });
        }

        // Display Initial Health Tip
        displayRandomTip();
    }

    private void displayRandomTip() {
        if (tvHealthTip != null && healthTips.length > 0) {
            int randomIndex = new Random().nextInt(healthTips.length);
            tvHealthTip.setText(healthTips[randomIndex]);
        }
    }

    private void setupHydrationListeners() {
        // Toggle units
        tvWaterStatus.setOnClickListener(v -> {
            isLiterMode = !isLiterMode;
            getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE)
                    .edit().putBoolean("is_liter_mode", isLiterMode).apply();
            updateWaterUI();
        });

        // Long press goal text to MODIFY GOAL
        tvWaterStatus.setOnLongClickListener(v -> {
            showSetWaterGoalDialog();
            return true;
        });

        // Long press progress bar to RESET
        waterProgress.setOnLongClickListener(v -> {
            showResetHydrationConfirm();
            return true;
        });

        // Quick Add Buttons
        setupQuickAddButton(R.id.btnAdd250, 250);
        setupQuickAddButton(R.id.btnAdd500, 500);
        setupQuickAddButton(R.id.btnAdd750, 750);
        setupQuickAddButton(R.id.btnAdd1000, 1000);
    }

    private void setupQuickAddButton(int id, int amount) {
        View btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> addWaterAmount(amount));
            // Long press to UNDO (subtract)
            btn.setOnLongClickListener(v -> {
                subtractWaterAmount(amount);
                return true;
            });
        }
    }

    private void addWaterAmount(int amountMl) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE);
        int current = prefs.getInt("water_current", 0);
        prefs.edit().putInt("water_current", current + amountMl).apply();
        updateWaterUI();
        showWaterToast("Added", amountMl);
    }

    private void subtractWaterAmount(int amountMl) {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE);
        int current = prefs.getInt("water_current", 0);
        int updated = Math.max(0, current - amountMl);
        prefs.edit().putInt("water_current", updated).apply();
        updateWaterUI();
        showWaterToast("Removed", amountMl);
    }

    private void showWaterToast(String action, int amount) {
        String unitLabel = isLiterMode ? String.format(Locale.getDefault(), "%.2fL", amount / 1000.0) : amount + "ml";
        Toast.makeText(this, action + " " + unitLabel, Toast.LENGTH_SHORT).show();
    }

    private void showResetHydrationConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Progress?")
                .setMessage("Do you want to clear your water intake for today?")
                .setPositiveButton("Reset", (d, w) -> {
                    getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE)
                            .edit().putInt("water_current", 0).apply();
                    updateWaterUI();
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void showSetWaterGoalDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("e.g. 3000");

        new AlertDialog.Builder(this)
                .setTitle("Set Daily Goal (ml)")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    String val = input.getText().toString();
                    if (!val.isEmpty()) {
                        int newGoal = Integer.parseInt(val);
                        getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE)
                                .edit().putInt("water_goal", newGoal).apply();
                        updateWaterUI();
                    }
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void updateWaterUI() {
        SharedPreferences prefs = getSharedPreferences("HealthPrefs_" + currentUser, MODE_PRIVATE);
        int goal = prefs.getInt("water_goal", 2500);
        int current = prefs.getInt("water_current", 0);

        int percent = (goal > 0) ? (int) (((float) current / goal) * 100) : 0;
        if (percent > 100) percent = 100;

        waterProgress.setProgress(percent);
        tvWaterPercent.setText(percent + "%");

        if (isLiterMode) {
            double currentL = current / 1000.0;
            double goalL = goal / 1000.0;
            tvWaterStatus.setText(String.format(Locale.getDefault(), "%.2f / %.2f L", currentL, goalL));
        } else {
            tvWaterStatus.setText(current + " / " + goal + " ml");
        }
    }

    private void showWeightUpdateDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(this)
                .setTitle("Update Body Weight")
                .setMessage("Enter current weight (kg):")
                .setView(input)
                .setPositiveButton("Update", (d, w) -> {
                    String newWeight = input.getText().toString();
                    if (!newWeight.isEmpty()) {
                        getSharedPreferences("UserPrefs_" + currentUser, MODE_PRIVATE)
                                .edit().putString("current_body_weight", newWeight).apply();
                        Toast.makeText(this, "Weight Updated to " + newWeight + "kg", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    private void setupNavigationHeader(NavigationView navigationView) {
        if (navigationView == null || navigationView.getHeaderCount() == 0) return;

        View headerView = navigationView.getHeaderView(0);
        ImageView ivFullHeader = headerView.findViewById(R.id.ivNavFullHeader);
        TextView tvNavUser = headerView.findViewById(R.id.tvNavUsername);
        TextView tvNavEmail = headerView.findViewById(R.id.tvNavEmail);

        if (tvNavUser != null) tvNavUser.setText("@" + currentUser);

        Map<String, String> userDetails = dbHelper.getFullUserDetails(currentUser);
        if (userDetails != null && tvNavEmail != null) {
            tvNavEmail.setText(userDetails.get("email") != null ? userDetails.get("email") : "No email set");
        }

        String savedUriString = getSharedPreferences("ProfilePrefs", MODE_PRIVATE)
                .getString("profile_uri_" + currentUser, null);

        if (savedUriString != null && ivFullHeader != null) {
            ivFullHeader.setImageURI(Uri.parse(savedUriString));
        }
    }

    private void initWeeklyTrackerViews() {
        tvMon = findViewById(R.id.tvMon);
        tvTue = findViewById(R.id.tvTue);
        tvWed = findViewById(R.id.tvWed);
        tvThu = findViewById(R.id.tvThu);
        tvFri = findViewById(R.id.tvFri);
        tvSat = findViewById(R.id.tvSat);
        tvSun = findViewById(R.id.tvSun);
    }

    private void updateDashboardStats() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        TextView[] dayViews = {tvMon, tvTue, tvWed, tvThu, tvFri, tvSat, tvSun};

        int activeDaysThisWeek = 0;

        for (int i = 0; i < 7; i++) {
            String dateToCheck = sdf.format(cal.getTime());
            boolean hasWorkout = dbHelper.hasWorkoutOnDate(currentUser, dateToCheck);

            if (dayViews[i] != null) {
                if (hasWorkout) {
                    dayViews[i].setBackgroundResource(R.drawable.bg_day_active);
                    dayViews[i].setTextColor(Color.parseColor("#121212"));
                    activeDaysThisWeek++;
                } else {
                    dayViews[i].setBackgroundResource(R.drawable.bg_day_inactive);
                    dayViews[i].setTextColor(Color.parseColor("#9E9E9E"));
                }
            }
            cal.add(Calendar.DATE, 1);
        }

        if (tvActiveDaysText != null) {
            tvActiveDaysText.setText(activeDaysThisWeek + "/7 Days Active");
        }
    }

    private void loadScheduleCard() {
        Calendar cal = Calendar.getInstance();
        String todayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        int todayInt = cal.get(Calendar.DAY_OF_WEEK);

        scheduledMuscle = dbHelper.getScheduledMuscleForToday(currentUser, todayInt);

        TextView tvDay = findViewById(R.id.tvScheduleDay);
        TextView tvMuscle = findViewById(R.id.tvScheduleMuscle);
        ImageView ivIcon = findViewById(R.id.ivScheduleIcon);

        if (tvDay != null) tvDay.setText(todayName + " Focus");
        if (tvMuscle != null) {
            tvMuscle.setText(scheduledMuscle != null ? scheduledMuscle.toUpperCase() : "REST DAY");
            tvMuscle.setTextColor(scheduledMuscle.equalsIgnoreCase("Rest Day") ? Color.parseColor("#9E9E9E") : Color.parseColor("#A4FF33"));
        }

        if (ivIcon != null) {
            updateScheduleIcon(scheduledMuscle, ivIcon);
        }

        cal.add(Calendar.DATE, 1);
        int tomorrowInt = cal.get(Calendar.DAY_OF_WEEK);
        String tomorrowName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        String tomorrowMuscle = dbHelper.getScheduledMuscleForToday(currentUser, tomorrowInt);

        if (tvTomorrowDay != null) tvTomorrowDay.setText(tomorrowName + " Focus");
        if (tvTomorrowMuscle != null) {
            tvTomorrowMuscle.setText(tomorrowMuscle != null ? tomorrowMuscle.toUpperCase() : "REST DAY");
        }
    }

    private void updateScheduleIcon(String muscle, ImageView iconView) {
        if (muscle == null) return;
        if (muscle.equalsIgnoreCase("Legs")) {
            iconView.setImageResource(R.drawable.ic_legs);
        } else if (muscle.toLowerCase().contains("chest")) {
            iconView.setImageResource(R.drawable.ic_chest);
        } else if (muscle.equalsIgnoreCase("Rest Day")) {
            iconView.setImageResource(R.drawable.ic_rest);
        } else {
            iconView.setImageResource(R.drawable.ic_body_base);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_add_workout) {
            startActivity(new Intent(this, AddWorkoutActivity.class));
        } else if (id == R.id.nav_suggested) {
            startActivity(new Intent(this, SuggestedActivity.class));
        } else if (id == R.id.nav_history) {
            startActivity(new Intent(this, HistoryActivity.class));
        } else if (id == R.id.nav_report) {
            startActivity(new Intent(this, ReportActivity.class));
        } else if (id == R.id.nav_calories || id == R.id.nav_supplements) {
            Toast.makeText(this, "Coming Soon!", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.nav_issue) {
            startActivity(new Intent(this, BugReportActivity.class));
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE).edit().clear().apply();
        Intent intent = new Intent(DashboardActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardStats();
        loadScheduleCard();
        updateWaterUI();
        displayRandomTip(); // Refresh the tip when returning to the dashboard

        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupNavigationHeader(navigationView);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}