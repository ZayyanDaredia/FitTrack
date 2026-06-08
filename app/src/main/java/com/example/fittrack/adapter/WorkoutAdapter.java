package com.example.fittrack.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.fittrack.R;
import com.example.fittrack.model.WorkoutModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private List<WorkoutModel> workoutList;

    public WorkoutAdapter(List<WorkoutModel> workoutList) {
        this.workoutList = workoutList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 🟢 LOOK AT THIS LINE
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    // 1. The Method that puts data into the rows
    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutModel current = workoutList.get(position);

        // 🟢 DATE SAFETY: Fixes the 0007 bug on the fly
        String displayDate = current.getDate();
        if (displayDate == null || displayDate.contains("0007") || displayDate.length() < 8) {
            displayDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        }

        // 🟢 GROUPING LOGIC: Hides headers for exercises in the same session
        boolean isFirst = true;
        if (position > 0) {
            WorkoutModel prev = workoutList.get(position - 1);
            if (current.getDate().equals(prev.getDate()) &&
                    current.getMuscleGroup().equals(prev.getMuscleGroup())) {
                isFirst = false;
            }
        }

        // 🟢 UI UPDATES: Using the EXACT IDs from your item_workout.xml
        if (isFirst) {
            holder.tvDate.setVisibility(View.VISIBLE);
            holder.tvDate.setText(displayDate);
            holder.tvMuscleGroup.setVisibility(View.VISIBLE);
            holder.tvMuscleGroup.setText(current.getMuscleGroup());
        } else {
            holder.tvDate.setVisibility(View.GONE);
            holder.tvMuscleGroup.setVisibility(View.GONE);
        }

        // Use tvRowExercise and tvRowSets to match your XML exactly
        holder.tvRowExercise.setText(current.getWorkoutName());

        String sets = current.getSets();
        if (current.getMuscleGroup().equalsIgnoreCase("Cardio")) {
            holder.tvRowSets.setText("Set " + sets + " Mins");
        } else {
            holder.tvRowSets.setText("Set " + sets);
        }
    }

    // 2. The Bridge between Java and XML
    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        // 🟢 These variable names must match what we used above
        TextView tvDate, tvMuscleGroup, tvRowExercise, tvRowSets;
        LinearLayout cardContainer;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);

            // 🟢 These R.id names MUST match your item_workout.xml exactly
            tvDate = itemView.findViewById(R.id.tvDate);
            tvMuscleGroup = itemView.findViewById(R.id.tvMuscleGroup);
            tvRowExercise = itemView.findViewById(R.id.tvRowExercise);
            tvRowSets = itemView.findViewById(R.id.tvRowSets);
            cardContainer = itemView.findViewById(R.id.cardContainer);
        }
    }

    private void updateCardBackground(WorkoutViewHolder holder, boolean isFirst, boolean isLast) {
        if (isFirst && isLast) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_full_rounded);
        } else if (isFirst) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_group_top);
        } else if (isLast) {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_group_bottom);
        } else {
            holder.cardContainer.setBackgroundResource(R.drawable.bg_group_middle);
        }
    }

    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat output = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
            Date date = input.parse(dateStr);
            return output.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public int getItemCount() {
        return workoutList.size();
    }
}