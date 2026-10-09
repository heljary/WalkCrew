# Google Play Console - Data Safety Form Answer Sheet (WalkCrew)

Use this cheat sheet to fill out the **Data safety** questionnaire in Google Play Console (Policy & Programs &rarr; App content &rarr; Data safety).

---

## Overview Summary

| Question | Answer | Details / Explanation |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **Yes** | App collects User Name, Email/User ID, Steps (Health data), and Device token for notifications. |
| **Is all of the user data collected by your app encrypted in transit?** | **Yes** | All network requests use TLS/HTTPS encryption with Firebase & Google Cloud. |
| **Do you provide a way for users to request that their data is deleted?** | **Yes** | Yes, users can delete their account in-app (Profile > Delete account) or via web request page. |
| **Does your app allow users to create an account?** | **Yes** | Yes, via Google Sign-In (Firebase Auth). |
| **Add a link that users can use to request that their account and associated data be deleted:** | Provide URL to hosted `account-deletion.html` | e.g., `https://your-domain.web.app/account-deletion.html` |

---

## Detailed Data Types Breakdown

### 1. Personal Info

#### A. Name
* **Collected?** Yes
* **Shared?** No (only visible to users within private crews joined by the user)
* **Ephemeral?** No (stored in Firestore user profile until account deletion)
* **Required or Optional?** Required for leaderboard identification
* **Purpose:** App functionality (Display name on leaderboards & crew rosters), Personalization

#### B. Email address
* **Collected?** Yes (via Google Sign-In)
* **Shared?** No
* **Ephemeral?** No
* **Required or Optional?** Required for account authentication
* **Purpose:** App functionality (Account management, authentication)

#### C. User IDs
* **Collected?** Yes (Firebase Auth UID)
* **Shared?** No
* **Ephemeral?** No
* **Required or Optional?** Required
* **Purpose:** App functionality (Account management, database access control)

---

### 2. Health and Fitness

#### A. Fitness info (Step Count / Distance)
* **Collected?** Yes (Reads steps from Health Connect / hardware sensor)
* **Shared?** No (visible only to mutual members of crews the user chose to join)
* **Ephemeral?** No (stored daily/weekly aggregates in Firestore)
* **Required or Optional?** Required for core app functionality (step leaderboard & tracking)
* **Purpose:** App functionality (Leaderboard, streak tracking, progress ring), Analytics/progress visualization
* **Is this data sold?** **NO** (Never sold to data brokers or third parties)
* **Used for advertising?** **NO**

---

### 3. Messages / Social

* **Not collected.** (WalkCrew uses preset nudges and reaction emojis rather than free-form messaging).

---

### 4. App Activity

#### A. App interactions
* **Collected?** Yes (crew joins, reactions: fire/clap/muscle/turtle/rocket, nudge actions)
* **Shared?** No
* **Ephemeral?** No
* **Purpose:** App functionality (Social motivation, leaderboards)

---

### 5. Device or other identifiers

#### A. Device or other identifiers
* **Collected?** Yes (Firebase Cloud Messaging / FCM token)
* **Shared?** No
* **Ephemeral?** No
* **Purpose:** App functionality (Push notifications for friend nudges, daily 6:00 PM goal reminders)

---

## Health Connect Policy Specific Declarations

* **Permissions declared:** `android.permission.health.READ_STEPS`
* **Health Connect Rationale Activity:** Declared with intent filter `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`
* **Zero-Selling Commitment:** Affirm in console that health data is never transferred to third parties for advertising or tracking.
