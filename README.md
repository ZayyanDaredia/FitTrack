# 🏋️‍♂️ FitTrack — Android Fitness Management System

FitTrack is a localized, offline-first personal health and fitness tracking application developed for Android. It eliminates the disorganization of manual workout tracking by offering users a centralized, privacy-focused platform to log workouts, plan schedules, and visualize physical progress through data-driven insights.

---

## 🚀 Tech Stack

FitTrack leverages a modern, efficient, and completely local architecture to ensure zero latency and absolute user privacy:

* **Language:** Java (JDK 17+)
* **IDE & Environment:** Android Studio / Visual Studio Code (VS Code)
* **User Interface:** Android XML (Custom high-contrast "Gym Neon" Theme)
* **Data Persistence:** SQLite (Embedded relational local database)

---

## ✨ Core Features

### 🔐 1. Secure User Authentication & Profiles
* Complete local registration and login processing via the `users` database table.
* Secure storage of critical personal biometrics including Full Name, Email, Age, Height, and Weight.

### 💪 2. Session-Based Workout Logging
* Dynamic exercise entry allowing users to input specific `sets`, `reps`, and `weight`.
* Advanced architectural grouping of individual exercises under a unique `session_id` to neatly categorize a single gym visit.
* Custom categorization by major muscle groups (e.g., Chest, Back, Legs).

### 📅 3. Automated Weekly Scheduling
* Personalized workout roadmap linked directly to the `workout_schedule` table.
* Integrated **Composite Unique Constraint** (`username` + `day_of_week`) preventing schedule conflicts and ensuring training discipline.

### 📊 4. "Gym Neon" Reporting Dashboard
* Real-time data aggregation that reads from localized SQLite records.
* Interactive visual reporting, featuring a **Muscle Split Pie Chart** showing muscle group training distribution and weekly consistency markers.

### 🐛 5. Integrated Support & Bug Reporting
* Local support ticketing interface allowing users to categorize and describe application issues directly into the `bug_reports` table for maintenance logging.

---

## 🛠️ Database Schema (SQLite)

The architecture relies on a robust relational "Hub-and-Spoke" model where the `users` table acts as the central anchor:

```sql
-- Core User Profiles
CREATE TABLE users (
    username TEXT PRIMARY KEY,
    password TEXT,
    fullname TEXT,
    email TEXT,
    age TEXT,
    height TEXT,
    weight TEXT,
    registration_date TEXT
);

-- Workout Sessions Log
CREATE TABLE workouts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    session_id INTEGER,
    username TEXT,
    workoutname TEXT,
    sets TEXT,
    reps TEXT,
    weight TEXT,
    muscle_group TEXT,
    date TEXT
);
