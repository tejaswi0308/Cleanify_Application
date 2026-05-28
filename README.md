# 🧹 Cleanify
[![Android Platform](https://img.shields.io/badge/Platform-Android-green.svg?style=flat-square&logo=android)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Java-orange.svg?style=flat-square)](https://www.oracle.com/java/)
[![Backend](https://img.shields.io/badge/Backend-Supabase-blueviolet.svg?style=flat-square&logo=supabase)](https://supabase.com/)
[![QR Scanning](https://img.shields.io/badge/QR_Scanner-ZXing-blue.svg?style=flat-square)](https://github.com/zxing/zxing)
**Cleanify** is a native Android application designed to streamline and automate cleaning requests and management within campus hostels or residential complexes. Built with Java and integrated directly with a **Supabase** backend using lightweight HTTP REST communication, it offers a secure, role-based ecosystem for students and cleaning staff.
---
## 📱 Features & Workflows
###  Student Portal
- **Secure Registration & Login**: Registration collects essential location metadata (hostel name, floor, room number) and student ID credentials.
- **Request Booking**: Schedule regular, deep, or normal cleaning. Time slots are generated dynamically in 30-minute intervals based on availability and logic (automatically rolling over to the next day if booking late).
- **Live Status Tracking**: View current cleaning requests. An active request card shows the status (Pending ➔ Staff Assigned ➔ In Progress ➔ Completed) with an active countdown timer.
- **Verification System**: Generate unique QR codes for cleaning tasks to let staff securely verify completion.
- **History & Ratings**: Keep a record of past cleanings, with the ability to leave star ratings and textual feedback for completed tasks.
- **Profile Customization**: Edit profile details (e.g., phone, avatar) with profile images loaded dynamically via Glide.
### 🛠️ Staff / Househelp Portal
- **Task Dashboard**: View all assigned cleaning tasks for the day within the assigned hostel block.
- **Smart Assignment**: Automatically queries and matches requests with available, free staff members assigned to the student's hostel block.
- **QR Code Scanner**: Integrated camera scanner (powered by ZXing) to scan a student's QR code, securely changing the task status from `assigned` ➔ `in_progress` ➔ `completed`.
- **History Logs**: Maintain records of all completed and past cleaning duties.
### 🔒 Core Utilities & Security
- **Salted Password Hashing**: Utilizes PBKDF2 (Password-Based Key Derivation Function 2) with a random cryptographic salt client-side (`PasswordUtils.java`), securing direct DB credential storage.
- **Offline Profile Access**: Caches the active user profile locally via `SharedPreferences` (`LocalDataManager.java`), avoiding redundant API roundtrips.
- **Fail-safe Callbacks**: Protects UI tasks from network-related lifecycle leaks using a custom weak reference wrapper (`SafeCallback.java`).
---
## 🛠️ Tech Stack & Architecture
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **UI Components:** AppCompat, Material Design Components, ConstraintLayout, CircularImageView (CircleImageView)
- **Networking:** OkHttp3 (HTTP client) & Gson (JSON Serialization/Deserialization)
- **Database Engine:** Supabase (PostgreSQL REST Endpoint)
- **Image Caching:** Glide
- **QR Code Suite:** ZXing (Zebra Crossing) for scanning and generation
---
## 🚀 Setup & Installation
### 1. Clone the Project
```bash
git clone https://github.com/your-username/Cleanify_Application.git
cd Cleanify_Application
```
### 2. Configure the Supabase Database
Instead of standard Supabase Auth, the app connects directly to the PostgREST API using a custom client-side credential store for high flexibility. 
Go to your [Supabase Dashboard](https://supabase.com/dashboard), open the **SQL Editor**, and run the following commands to create the required tables:
```sql
-- 1. Create the Students Table
CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT,
    reg_number TEXT,
    hostel_id TEXT,
    floor_number TEXT,
    room_number TEXT,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    profile_image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
-- 2. Create the Staff Table
CREATE TABLE staff (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email TEXT UNIQUE NOT NULL,
    full_name TEXT NOT NULL,
    phone TEXT,
    assigned_hostel TEXT,
    househelp_id TEXT,
    shift TEXT,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    profile_image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
-- 3. Create the Cleaning Requests Table
CREATE TABLE cleaning_requests (
    request_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id TEXT NOT NULL,
    student_name TEXT,
    hostel TEXT NOT NULL,
    room_number TEXT NOT NULL,
    floor_number TEXT,
    date TEXT NOT NULL,
    time_slot TEXT NOT NULL,
    cleaning_type TEXT NOT NULL,
    additional_notes TEXT,
    status TEXT NOT NULL DEFAULT 'pending', -- pending, assigned, in_progress, completed, cancelled
    assigned_staff_id TEXT,
    assigned_staff_name TEXT,
    created_at TIMESTAMPTZ DEFAULT now(),
    start_time BIGINT,
    completed_at BIGINT,
    estimated_minutes INTEGER DEFAULT 25,
    rating INTEGER,
    feedback TEXT,
    qr_code TEXT
);
-- 4. Disable Row Level Security (RLS) for testing or enable anonymous access
ALTER TABLE students DISABLE ROW LEVEL SECURITY;
ALTER TABLE staff DISABLE ROW LEVEL SECURITY;
ALTER TABLE cleaning_requests DISABLE ROW LEVEL SECURITY;
```
> [!NOTE]
> If you have existing tables referencing `auth.users` via foreign key constraints, make sure to drop those constraints and adjust RLS settings as shown in [supabase_migration.sql](supabase_migration.sql) to permit anonymous operations using the anon key.
### 3. Add API Configurations (`local.properties`)
The build script reads credentials dynamically from a local configuration file to avoid committing sensitive details to Git.
1. In the root directory of the project, locate or create `local.properties`.
2. Add the following properties (using your actual project details from Settings -> API in the Supabase Dashboard):
   ```properties
   SUPABASE_URL=https://your-project-reference.supabase.co
   SUPABASE_KEY=your-anon-public-key
   ```
3. A template is provided at `local.properties.template` for reference.
### 4. Build and Run
1. Open the project in **Android Studio** (Koala or newer recommended).
2. Allow Gradle to sync and fetch all dependencies (`build.gradle.kts` handles OkHttp, Gson, Glide, ZXing, and Material design libraries automatically).
3. Connect your Android device or start an emulator.
4. Click **Run** (`Shift + F10`) to compile and launch.
---
## 📂 Project Structure
The project follows a clean packages hierarchy inside `app/src/main/java/com/example/cleanify_application/`:
```
├── activities
│   ├── SplashActivity            # Handles initial logo screen and session checks
│   ├── LoginActivity             # Standard login handling student & staff tabs
│   ├── RegisterActivity          # Form collection for hostel profile / worker details
│   ├── StudentDashboardActivity  # Student hub: request tracking, current status
│   ├── StudentHistoryActivity    # Completed list with feedback & reviews
│   ├── StaffDashboardActivity    # Task assignment view, camera scan entry point
│   ├── StaffHistoryActivity      # Logs of cleanings completed by the worker
│   ├── QRCodeActivity            # Renders scanning screens or code representation
│   ├── NewRequestActivity        # Form targeting slots, cleaning categories
│   ├── CleaningProgressActivity  # Details on current worker & progress meters
│   ├── FeedbackActivity          # Form to submit ratings/comments on completions
│   └── EditProfileActivity       # Form to alter bio data and upload avatars
├── adapters
│   ├── RequestHistoryAdapter     # Inflater showing past bookings lists
│   ├── CompletedTaskAdapter      # Inflater showing finished items on staff end
│   └── UpcomingTaskAdapter       # Inflater targeting current tasks lists for staff
├── constants
│   └── AppConstants              # Central constants (status, table names, preferences)
├── models
│   ├── User                      # Unified model mapping students/staff data structures
│   └── CleaningRequest           # Detailed representation of requests details
├── network
│   ├── SupabaseClient            # REST operations, headers configuration, and query strings
│   └── SafeCallback              # Weak reference callback to prevent memory leaks
└── utils
    ├── PasswordUtils             # Cryptographic algorithms (Salt generation & hashing)
    ├── LocalDataManager          # SharedPreferences CRUD operations
    └── DateTimeUtils             # Conversions and calendar formatters
```
---

