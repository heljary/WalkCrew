# WalkCrew Privacy Policy

**Effective Date:** October 9, 2026  
**Last Updated:** October 9, 2026

Welcome to WalkCrew ("we", "our", or "us"). WalkCrew is a community-driven fitness and step-tracking application designed to encourage physical activity with friends and crews. We value your privacy and are committed to protecting your personal data and health data.

This Privacy Policy explains what information we collect, how we use it, how we protect it, and your rights regarding your data, in full compliance with the Google Play Developer Program Policies, Google Play Health Connect Policy, and applicable global privacy regulations (including GDPR and CCPA).

---

## 1. Information We Collect

### A. Information You Provide Directly
* **Account Information:** When you sign in using Google Sign-In, we collect your authentication identifier (UID), your public display name, and optional avatar selections. We do not store or collect your Google password.
* **Profile Preferences:** Daily step goal (e.g., 3,000–20,000 steps), theme preference (System, Light, Dark), notification preferences (e.g., 6:00 PM daily check-in reminders).
* **Social Activity:** Crews you create or join, reactions (fire, clap, muscle, turtle, rocket) you leave on crew leaderboards, and nudge timestamps sent to or received from crew members.

### B. Health & Physical Activity Data (Health Connect)
* **Step Count Records (`READ_STEPS`):** With your explicit in-app consent, WalkCrew reads your daily and weekly cumulative step counts from Android Health Connect (or on-device step counter hardware sensors) to power crew leaderboards, display your progress rings, compute streaks, and track weekly challenges.
* **Important Policy Commitment:** 
  * We **ONLY** request permission to read steps (`READ_STEPS`).
  * We **NEVER** request access to heart rate, medical records, blood pressure, sleep cycles, GPS location, or any other sensitive clinical categories.
  * We **NEVER** sell, rent, monetize, or license your step data or health information to third-party data brokers, advertising networks, or pharmaceutical entities.
  * Step data is never used for serving targeted advertising.

### C. Technical & Notification Data
* **Device Token:** Firebase Cloud Messaging (FCM) registration token used exclusively to deliver peer nudges and milestone celebration alerts to your device.
* **Operating System & Performance Logs:** Non-identifying crash diagnostics collected solely to ensure stability and compatibility.

---

## 2. How We Use Your Information

We use the data we collect strictly for the operational features of the WalkCrew app:
1. **Leaderboards & Crews:** Displaying your daily and weekly steps on your crew's private leaderboard so members can motivate each other.
2. **Streaks & Ring Progress:** Calculating your active streaks when you achieve your personalized daily goal and displaying your progress ring.
3. **Friend Notifications (Nudges):** Allowing crew members to send a single friendly nudge per day via Firebase Cloud Messaging.
4. **Reminders:** Sending a local reminder at 6:00 PM if you are below 60% of your daily step goal (optional, controllable in settings).

---

## 3. Data Sharing & Third-Party Services

We do **NOT** sell, rent, or trade your personal data or health information to third parties.

Your data is handled strictly by trusted infrastructure providers required to operate WalkCrew:
* **Google Firebase (Google Cloud Platform):**
  * **Firebase Authentication:** Handles secure sign-in tokens.
  * **Cloud Firestore:** Stores crew memberships, leaderboard aggregates, and user profile documents encrypted at rest and in transit.
  * **Firebase Cloud Messaging (FCM):** Delivers peer nudges and milestone alerts.
* **Android Health Connect:** Health Connect remains under your direct system control. Step counts read by WalkCrew are subject to your system permissions and can be revoked at any time in Android Settings &rarr; Health Connect.

We do not integrate third-party ad SDKs or third-party behavioral trackers.

---

## 4. Data Security

We implement industry-standard security safeguards to protect your personal information:
* **Encryption in Transit:** All communications with backend servers use HTTPS/TLS 1.3 encryption.
* **Encryption at Rest:** Firestore and Firebase systems enforce 256-bit AES encryption at rest.
* **Role-Based Firestore Security Rules:** Our database enforces strict zero-trust security rules: only authenticated crew members can view crew aggregates, and only you can edit your personal profile and settings.

---

## 5. Account and Data Deletion (Your Rights)

You have full control over your data:

### In-App Deletion
You can delete your account and all associated data at any time directly in the app:
1. Navigate to **Profile** &rarr; **Account**.
2. Tap **Delete my account and all data**.
3. Confirm in the dialog.
This permanently deletes your Firebase Authentication user record, your Firestore user document, clears your step logs, and removes you from all crews immediately.

### Web Deletion Portal
If you have uninstalled the app or cannot access your device, you can submit an account deletion request using our online portal or by emailing **support@walkcrew.app**.

---

## 6. Children's Privacy

WalkCrew is not directed to individuals under the age of 13 (or under 16 in the European Economic Area). We do not knowingly collect personal or health information from children. If we discover that a child has provided us with personal data, we will immediately delete it from our systems.

---

## 7. Changes to This Privacy Policy

We may update this Privacy Policy periodically to reflect app updates, operating system requirements, or regulatory changes. We will notify you of any material changes via in-app banner or update notices.

---

## 8. Contact Us

If you have any questions, feedback, or requests regarding this Privacy Policy or how your health data is handled, please contact our privacy team:

* **Email:** support@walkcrew.app
* **Developer:** WalkCrew Team
* **Data Protection Inquiries:** privacy@walkcrew.app
