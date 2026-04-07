# GymBro Privacy Policy

**Effective Date:** April 2026  
**Last Updated:** April 2026

---

## 1. Introduction

GymBro ("we," "us," "our," or "GymBro") is committed to protecting your privacy. This Privacy Policy explains how we collect, use, disclose, and otherwise handle your information when you use our iOS application and related services (the "Service").

**Important:** GymBro is designed with privacy as a core value. We collect minimal data, never sell your information, and are transparent about every use.

---

## 2. Information We Collect

### 2.1 HealthKit Data (Read-Only)

When you authorize GymBro to access Apple HealthKit, we read the following data types:

- **Resting Heart Rate** (daily average)
- **Heart Rate Variability (HRV)** — SDNN measurement (ms)
- **Sleep Analysis** — duration, stage breakdowns (light, deep, REM)
- **Active Energy Burned** — daily kcal
- **Workout History** — exercises, weight, reps, duration (from Apple Health or logged directly in GymBro)

**How We Use It:**
- Calculate your recovery readiness score
- Personalize your adaptive training program
- Detect training plateaus and fatigue signals
- Provide AI coaching recommendations

**Data Location:** Stored locally on your device. Never transmitted to third parties. Only synced via Apple CloudKit (encrypted) if you enable cloud backup.

**Important:** GymBro does NOT write to HealthKit. Your health data is read-only. We never export or transmit your health information outside your device.

---

### 2.2 Workout & Training Data

You provide this data directly when using GymBro:

- Exercise names, weights, reps, sets, dates, times
- Rest periods, RPE ratings, subjective comments
- Program names, structure, custom modifications
- Personal records (PRs), body weight measurements
- Training notes and goals

**How We Use It:**
- Display your workout history
- Calculate adaptive training recommendations
- Track progress and provide analytics
- Personalize AI coach responses

**Data Location:** Stored in local SwiftData database on your device. Optionally synced via CloudKit if you enable cloud backup (encrypted).

---

### 2.3 Account & Authentication Data

To create an account, we collect:

- **Email Address** — via Sign in with Apple
- **Apple User ID** — cryptographic identifier from Apple
- **Name** — optional, user-provided
- **Profile Picture** — optional, from Apple

**How We Use It:**
- Authenticate your account
- Enable CloudKit sync across your devices
- Support account recovery and deletion

**Data Location:** CloudKit (Apple's infrastructure, encrypted).

**Important:** We do NOT see your password. Sign in with Apple handles all authentication securely.

---

### 2.4 AI Coach Conversation Data

When you chat with GymBro's AI Coach, we collect:

- **Messages:** Your questions and the AI's responses
- **Context:** Snapshots of your recent workout history, PRs, and recovery data (sent only to OpenAI for response generation)
- **Timestamps:** When each message was sent

**How We Use It:**
- Provide personalized training advice
- Improve response quality (aggregate analysis, future fine-tuning)
- Maintain conversation context

**Data Location:**
- Local storage: Conversation history stored locally on your device (encrypted)
- Cloud storage: Messages stored in CloudKit if you enable backup
- API calls: OpenAI receives your message + context for real-time response (not retained on OpenAI servers for MVP)

**Important:** Your health data is NOT sent to OpenAI. Only training-related context (e.g., "last 3 workouts," "current bodyweight") is shared during API calls.

---

### 2.5 Diagnostic & Crash Data

Apple automatically collects app crash reports and performance data through iOS. This includes:

- App crashes, errors, performance metrics
- Device type, OS version, app version
- Crash stack traces

**How We Use It:**
- Fix bugs and improve app stability
- Optimize performance

**Data Location:** Apple's servers (not stored by GymBro)

**Important:** Crash data is anonymous and not linked to your account.

---

### 2.6 What We DON'T Collect

❌ Location data  
❌ Camera or microphone recordings  
❌ Device contacts  
❌ Calendar or email data  
❌ Behavioral tracking (no analytics pixels)  
❌ Biometric data (fingerprint, face ID usage is handled by OS, not by us)

---

## 3. Data Usage & Retention

### 3.1 How We Use Your Data

| Purpose | Data Used | Basis |
|---------|-----------|-------|
| **Provide Service** | Workout data, HealthKit | Contractual (you use the app) |
| **Adapt Training** | Workouts, HRV, sleep, recovery | Legitimate interest (core feature) |
| **AI Coaching** | Chat, workout history | Contractual (you request coaching) |
| **Account Management** | Email, Apple ID, name | Contractual (account auth) |
| **Bug Fixes** | Crash data | Legitimate interest (app stability) |
| **Aggregate Analytics** | Anonymous, aggregated patterns | Legitimate interest (understand usage) |

### 3.2 Data Retention

| Data Type | Retention Period | Delete On |
|-----------|------------------|-----------|
| **Workout History** | Until user deletes | User action or account deletion |
| **HealthKit Cache** | 30-day rolling window | Automatic; never archived |
| **Account Data** | Until account deletion | User deletion request |
| **Chat History** | Until user deletes | User action or account deletion |
| **Crash Data** | 90 days (Apple) | Apple's automatic cleanup |

**To Delete Your Data:**
1. Settings → Profile → Delete Account
2. All local data is deleted immediately
3. Cloud data (CloudKit) is deleted within 24 hours

---

## 4. Data Sharing & Third Parties

### 4.1 We DO NOT Share Your Data With:

❌ Marketing companies  
❌ Advertising networks  
❌ Data brokers  
❌ Social media platforms  
❌ Fitness tracking sites (unless you explicitly connect)  
❌ Insurance companies  
❌ Researchers (without explicit consent)

### 4.2 We DO Share Data With (Necessary Services):

**Apple (CloudKit & HealthKit):**
- Your workout data (if you enable cloud sync)
- Account information (for authentication)
- Health data read (via HealthKit API—we never transmit this)
- Apple's privacy policy applies: https://www.apple.com/privacy/

**OpenAI (AI Coach API Calls):**
- Only: User message + recent training context (no health data)
- Not retained on OpenAI servers for MVP
- OpenAI's privacy policy: https://openai.com/policies/privacy-policy/
- You can opt out: Use offline coaching instead

**Azure (Cloud Backup, Optional):**
- If enabled, encrypted backup of your workouts/chats
- Microsoft's privacy policy: https://privacy.microsoft.com/

---

## 5. Your Rights & Controls

### 5.1 Access Your Data

You can view and export your data in-app:
- Settings → Data Export → Download (creates JSON file)

### 5.2 Correct Your Data

You can edit:
- Workout entries (edit/delete anytime)
- Profile information (name, email)
- Training preferences

### 5.3 Delete Your Data

**Option 1: Delete Individual Items**
- Swipe on workout → Delete
- Delete specific chat messages

**Option 2: Delete Everything**
- Settings → Profile → Delete Account
- Removes all local data + cloud data within 24h
- Account cannot be recovered

### 5.4 Revoke HealthKit Access

Settings → Privacy → Health → GymBro → Deselect data types

When you revoke access:
- GymBro cannot read future health data
- Existing cached health data is deleted
- App continues functioning (without recovery score)

### 5.5 Opt Out of Cloud Sync

- Settings → Sync & Backup → Disable Cloud Sync
- Your data stays on-device only
- Multi-device sync disabled (but local features work)

---

## 6. Data Security

### 6.1 How We Protect Your Data

**On-Device Encryption:**
- SwiftData stores data in encrypted database
- iOS protects app data at rest with device encryption

**In-Transit Encryption:**
- All cloud communication uses HTTPS/TLS 1.3
- CloudKit data is encrypted by Apple
- No data transmitted without encryption

**API Security:**
- OpenAI API calls use OAuth 2.0
- No API keys stored in app (fetched securely at runtime)
- Rate limiting prevents abuse

**Access Control:**
- Your account is accessed only via Sign in with Apple
- No password stored by GymBro
- Only you can access your account

### 6.2 What We Can't Guarantee

While we use industry-standard security, no system is 100% secure. Potential risks include:

- Device compromise (if your phone is hacked)
- iCloud account compromise (if your Apple ID is breached)
- Network interception (if connecting on unsecured WiFi)

**Your Responsibility:**
- Keep your iOS device updated
- Use a strong Apple ID password
- Enable two-factor authentication on Apple ID
- Use secure WiFi for sensitive data

---

## 7. Children's Privacy

GymBro is **not intended for children under 13**. We do not knowingly collect data from children under 13.

If you believe a child under 13 has created an account, please contact us immediately at privacy@gymbro.app and we will delete their account.

---

## 8. International Data Transfers

GymBro is developed in the US. Data is stored in:

- **On-device:** Your iPhone (your jurisdiction)
- **CloudKit:** Apple's US data centers (compliant with GDPR, CCPA)
- **API calls:** OpenAI's servers (US-based, compliant with GDPR)

If you are an EU resident:

- You have rights under GDPR (access, rectification, erasure, portability)
- To exercise rights: privacy@gymbro.app
- We comply with GDPR data processing agreements

If you are a California resident:

- CCPA rights apply (see Section 10 below)

---

## 9. CCPA (California Consumer Privacy Act)

If you are a California resident, you have the right to:

**1. Know:** What data we collect about you
- Request: privacy@gymbro.app with subject "CCPA Data Request"
- Response time: 45 days

**2. Delete:** Your data
- Use in-app delete, or request via privacy@gymbro.app
- We delete within 45 days (CloudKit within 24h)

**3. Opt-Out:** Of data sales
- We do not sell your data, so opt-out is not applicable
- But you can disable all cloud features in Settings

**4. Non-Discrimination:** No penalties for exercising rights
- GymBro features work the same regardless of privacy choices

---

## 10. GDPR (EU General Data Protection Regulation)

If you are an EU resident, you have the right to:

**1. Access:** Request copy of your data
- Request: privacy@gymbro.app
- Response: 30 days

**2. Rectification:** Correct inaccurate data
- Edit in-app, or request: privacy@gymbro.app

**3. Erasure:** "Right to be Forgotten"
- Use Settings → Profile → Delete Account
- Or request: privacy@gymbro.app

**4. Portability:** Export your data
- Use Settings → Data Export (JSON file)
- Includes all workouts, training data, preferences

**5. Object:** Restrict processing
- Disable cloud sync: Settings → Sync & Backup
- Request data limitations: privacy@gymbro.app

**6. Lodge Complaint:** With your Data Protection Authority
- If you believe we violated your rights
- Contact your country's DPA (e.g., CNIL for France, ICO for UK)

---

## 11. Changes to This Privacy Policy

We may update this Privacy Policy periodically to reflect:

- Changes to our practices
- New features or data types
- Legal requirements

**When updated:**
- We will notify you in-app
- Email notification to your Apple ID email
- The "Last Updated" date will change

You are responsible for reviewing updates. Continued use of GymBro after updates means you accept the new policy.

---

## 12. Contact Us

**For Privacy Questions:**
- Email: privacy@gymbro.app
- Address: [GymBro HQ - to be added]

**For Account/Support Issues:**
- Email: support@gymbro.app

**For Legal Requests (subpoenas, warrants):**
- Email: legal@gymbro.app

We will respond to legitimate requests within 10 business days.

---

## 13. Summary: Our Privacy Promise

✅ **We collect only what we need** to provide training adaptation  
✅ **We never sell your data** to advertisers or brokers  
✅ **You control your data** — delete anytime  
✅ **We're transparent** about every use and third party  
✅ **Your health data stays on your device** (HealthKit is read-only)  
✅ **We encrypt everything** in transit and at rest  

---

**Version:** 1.0  
**Effective:** April 2026

By using GymBro, you accept this Privacy Policy.
