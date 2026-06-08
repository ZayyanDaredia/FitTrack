package com.example.fittrack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.util.Pair;

import com.example.fittrack.model.DBHelper;
import com.github.mikephil.charting.charts.RadarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.RadarData;
import com.github.mikephil.charting.data.RadarDataSet;
import com.github.mikephil.charting.data.RadarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportActivity extends AppCompatActivity {

    private RadarChart muscleRadarChart;
    private GridView heatmapGrid;
    private TextView tvTotalWorkouts, tvMostTrained, tvLeastTrained, tvViewAll;
    private Button btnMonthly, btnAllTime, btnCustomRange;
    private ImageButton btnBack;
    private FloatingActionButton fabShare;
    private LinearLayout historyContainer;
    private DBHelper dbHelper;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        dbHelper = new DBHelper(this);
        SharedPreferences pref = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        currentUser = pref.getString("username", "User");

        // UI Initialization
        muscleRadarChart = findViewById(R.id.reportPieChart);
        heatmapGrid = findViewById(R.id.heatmapGrid);
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvMostTrained = findViewById(R.id.tvMostTrained);
        tvLeastTrained = findViewById(R.id.tvLeastTrained);
        tvViewAll = findViewById(R.id.tvViewAll); // 🟢 Added
        btnMonthly = findViewById(R.id.btnMonthly);
        btnAllTime = findViewById(R.id.btnAllTime);
        btnCustomRange = findViewById(R.id.btnCustomRange);
        btnBack = findViewById(R.id.btnBackReport);
        fabShare = findViewById(R.id.fabShare);
        historyContainer = findViewById(R.id.historyContainer);

        // Click Listeners
        btnBack.setOnClickListener(v -> finish());
        btnMonthly.setOnClickListener(v -> { toggleFilterButtons(true); loadReportData(true); });
        btnAllTime.setOnClickListener(v -> { toggleFilterButtons(false); loadReportData(false); });
        btnCustomRange.setOnClickListener(v -> showDateRangePicker());
        fabShare.setOnClickListener(v -> shareWorkoutReport());

        // 🟢 View All redirect to HistoryActivity
        tvViewAll.setOnClickListener(v -> {
            Intent intent = new Intent(ReportActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // Initial Setup
        setupRadarChartBase();
        toggleFilterButtons(true);

        loadReportData(true);
        setupHeatmap();
    }

    private void loadWorkoutHistory(String startDate, String endDate) {
        historyContainer.removeAllViews();

        List<Map<String, String>> workouts;
        if (startDate != null && endDate != null) {
            workouts = dbHelper.getWorkoutsByDateRange(currentUser, startDate, endDate);
        } else {
            workouts = dbHelper.getUserWorkouts(currentUser);
        }

        if (workouts == null || workouts.isEmpty()) return;

        LayoutInflater inflater = LayoutInflater.from(this);

        String lastDate = "";
        String lastMuscleGroup = "";
        LinearLayout currentExerciseContainer = null;

        // 🟢 Tracker to limit sessions to 5
        int sessionCount = 0;

        for (Map<String, String> workout : workouts) {
            String currentDate = workout.get("date");
            String currentMuscleGroup = workout.get("muscle_group");

            // CHECK: Is this a different workout session?
            if (!currentDate.equals(lastDate) || !currentMuscleGroup.equals(lastMuscleGroup)) {

                // 🟢 LIMIT CHECK: Stop after 5 grouped cards (sessions)
                if (sessionCount >= 5) break;

                // Inflate a NEW CARD for this session
                View card = inflater.inflate(R.layout.item_workout_card, historyContainer, false);

                TextView tvDate = card.findViewById(R.id.tvDate);
                TextView tvMuscle = card.findViewById(R.id.tvMuscleGroup);
                currentExerciseContainer = card.findViewById(R.id.exercisesContainer);

                tvDate.setText(currentDate);
                tvMuscle.setText(currentMuscleGroup != null ? currentMuscleGroup.toUpperCase() : "");

                historyContainer.addView(card);

                lastDate = currentDate;
                lastMuscleGroup = currentMuscleGroup;

                sessionCount++; // 🟢 Increment card count
            }

            // ADD EXERCISE ROW: Inject the exercise into the current card
            if (currentExerciseContainer != null) {
                View row = inflater.inflate(R.layout.item_workout, currentExerciseContainer, false);

                TextView tvName = row.findViewById(R.id.tvRowExercise);
                TextView tvSets = row.findViewById(R.id.tvRowSets);

                String name = workout.get("exercise_name");
                String sets = workout.get("sets");
                String reps = workout.get("reps");

                tvName.setText(name);

                if (currentMuscleGroup != null && currentMuscleGroup.equalsIgnoreCase("CARDIO")) {
                    tvSets.setText("Set " + sets + " Mins");
                } else {
                    String repsText = (reps != null && !reps.equals("0") && !reps.isEmpty()) ? "x" + reps : "";
                    tvSets.setText("Set " + sets + repsText);
                }

                currentExerciseContainer.addView(row);
            }
        }
    }

    private void setupRadarChartBase() {
        muscleRadarChart.getDescription().setEnabled(false);
        muscleRadarChart.setWebLineWidth(1f);
        muscleRadarChart.setWebColor(Color.LTGRAY);
        muscleRadarChart.setWebAlpha(100);
        muscleRadarChart.getLegend().setEnabled(false);
    }

    private void loadReportData(boolean isMonthly) {
        Map<String, Integer> stats;
        int sessions;

        if (isMonthly) {
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String start = sdf.format(cal.getTime());
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
            String end = sdf.format(cal.getTime());

            stats = dbHelper.getStatsByDateRange(currentUser, start, end);
            sessions = dbHelper.getUniqueSessionCount(currentUser, start, end);
            loadWorkoutHistory(start, end);
        } else {
            stats = dbHelper.getStatsAllTime(currentUser);
            sessions = dbHelper.getUniqueSessionCountAllTime(currentUser);
            loadWorkoutHistory(null, null);
        }

        tvTotalWorkouts.setText(String.valueOf(sessions));
        updateRadarChart(stats);
        updateTextStats(stats);
    }

    private void updateTextStats(Map<String, Integer> stats) {
        if (stats == null || stats.isEmpty()) {
            tvMostTrained.setText("Most Trained: --");
            tvLeastTrained.setText("Least Trained: --");
            return;
        }
        String most = "", least = "";
        int max = -1, min = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > max) { max = entry.getValue(); most = entry.getKey(); }
            if (entry.getValue() < min) { min = entry.getValue(); least = entry.getKey(); }
        }
        tvMostTrained.setText("Most Trained - " + most);
        tvLeastTrained.setText("Least Trained - " + least);
    }

    private void updateRadarChart(Map<String, Integer> stats) {
        String[] labels = {"CHEST", "BACK", "LEGS", "SHOULDERS", "BICEPS", "TRICEPS", "CARDIO"};
        List<RadarEntry> entries = new ArrayList<>();

        for (String label : labels) {
            float val = 0;
            if (stats != null) {
                for (String key : stats.keySet()) {
                    if (key.equalsIgnoreCase(label)) {
                        val = stats.get(key);
                        break;
                    }
                }
            }
            entries.add(new RadarEntry(val));
        }

        RadarDataSet dataSet = new RadarDataSet(entries, "Performance Balance");
        dataSet.setColor(Color.parseColor("#A4FF33"));
        dataSet.setFillColor(Color.parseColor("#A4FF33"));
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(120);
        dataSet.setLineWidth(2f);

        RadarData data = new RadarData(dataSet);
        data.setValueTextColor(Color.TRANSPARENT);

        XAxis xAxis = muscleRadarChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setTextColor(Color.WHITE);
        xAxis.setTextSize(9f);

        muscleRadarChart.getYAxis().setAxisMinimum(0f);
        muscleRadarChart.getYAxis().setDrawLabels(false);
        muscleRadarChart.setExtraOffsets(10, 10, 10, 10);
        muscleRadarChart.setData(data);
        muscleRadarChart.animateXY(1000, 1000);
        muscleRadarChart.invalidate();
    }

    private void setupHeatmap() {
        new Thread(() -> {
            List<Boolean> workoutDays = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -167);
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            for (int i = 0; i < 168; i++) {
                workoutDays.add(dbHelper.hasWorkoutOnDate(currentUser, sdf.format(cal.getTime())));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            runOnUiThread(() -> heatmapGrid.setAdapter(new HeatmapAdapter(workoutDays)));
        }).start();
    }

    private void showDateRangePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Training Period")
                .build();
        picker.show(getSupportFragmentManager(), "range_picker");
        picker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            String start = sdf.format(new Date(selection.first));
            String end = sdf.format(new Date(selection.second));
            loadCustomRangeData(start, end);
        });
    }

    private void loadCustomRangeData(String start, String end) {
        Map<String, Integer> stats = dbHelper.getStatsByDateRange(currentUser, start, end);
        int sessions = dbHelper.getUniqueSessionCount(currentUser, start, end);
        tvTotalWorkouts.setText(String.valueOf(sessions));
        updateRadarChart(stats);
        updateTextStats(stats);
        loadWorkoutHistory(start, end);
        toggleFilterButtonsToCustom();
    }

    private void toggleFilterButtonsToCustom() {
        btnMonthly.setBackgroundColor(Color.TRANSPARENT);
        btnMonthly.setTextColor(Color.parseColor("#9E9E9E"));
        btnAllTime.setBackgroundColor(Color.TRANSPARENT);
        btnAllTime.setTextColor(Color.parseColor("#9E9E9E"));
        btnCustomRange.setTextColor(Color.parseColor("#A4FF33"));
    }

    private void shareWorkoutReport() {
        // 1. Target the LinearLayout that holds all the data
        View content = findViewById(R.id.llReportContent);

        if (content == null) {
            Toast.makeText(this, "Report content not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🟢 THE TRICK: Force the view to calculate its FULL scrollable height
        // We tell it: "Use the current width, but take as much height as you need."
        content.measure(
                View.MeasureSpec.makeMeasureSpec(content.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        content.layout(0, 0, content.getMeasuredWidth(), content.getMeasuredHeight());

        int totalHeight = content.getMeasuredHeight();
        int totalWidth = content.getMeasuredWidth();

        // Safety check to prevent crashes if height is 0
        if (totalHeight <= 0 || totalWidth <= 0) return;

        // 2. Create the PDF Document
        android.graphics.pdf.PdfDocument document = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo =
                new android.graphics.pdf.PdfDocument.PageInfo.Builder(totalWidth, totalHeight, 1).create();
        android.graphics.pdf.PdfDocument.Page page = document.startPage(pageInfo);

        // 3. Draw the View onto the PDF Canvas
        Canvas canvas = page.getCanvas();
        content.draw(canvas);
        document.finishPage(page);

        // 4. Save the PDF to Cache
        try {
            File cachePath = new File(getCacheDir(), "reports");
            if (!cachePath.exists()) cachePath.mkdirs();

            File file = new File(cachePath, "FitTrack_Report.pdf");
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            // 5. Trigger the Share Intent
            sharePdfFile(file);

        } catch (IOException e) {
            Log.e("PDF_ERROR", "Error: " + e.getMessage());
            Toast.makeText(this, "Failed to generate PDF report", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFilterButtons(boolean isMonthlyActive) {
        if (isMonthlyActive) {
            btnMonthly.setBackgroundColor(Color.parseColor("#A4FF33"));
            btnMonthly.setTextColor(Color.parseColor("#121212"));
            btnAllTime.setBackgroundColor(Color.TRANSPARENT);
            btnAllTime.setTextColor(Color.parseColor("#9E9E9E"));
        } else {
            btnAllTime.setBackgroundColor(Color.parseColor("#A4FF33"));
            btnAllTime.setTextColor(Color.parseColor("#121212"));
            btnMonthly.setBackgroundColor(Color.TRANSPARENT);
            btnMonthly.setTextColor(Color.parseColor("#9E9E9E"));
        }
        btnCustomRange.setTextColor(Color.parseColor("#9E9E9E"));
    }

    private class HeatmapAdapter extends BaseAdapter {
        private final List<Boolean> data;
        public HeatmapAdapter(List<Boolean> data) { this.data = data; }
        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int pos) { return data.get(pos); }
        @Override public long getItemId(int pos) { return pos; }
        @Override public View getView(int pos, View v, ViewGroup p) {
            if (v == null) v = LayoutInflater.from(ReportActivity.this).inflate(R.layout.item_heatmap_square, p, false);
            v.findViewById(R.id.viewSquare).setBackgroundResource(data.get(pos) ? R.drawable.bg_square_green : R.drawable.bg_square_grey);
            return v;
        }
    }
    // 🟢 Add this helper method to handle the actual sharing of the PDF file
    private void sharePdfFile(File file) {
        // Using getPackageName() ensures it matches your Manifest's ${applicationId}
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);

        // Create the Intent for sharing a PDF
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        // Grant temporary read permission to the app receiving the PDF
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Start the chooser
        startActivity(Intent.createChooser(intent, "Share Workout Report PDF"));
    }
}