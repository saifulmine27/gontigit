# ঘণ্টি (Ghonti) – Reminder অ্যাপ

ওয়েব ডিজাইন + নেটিভ Android অ্যালার্ম + Firebase লগইন ও ক্লাউড সিঙ্ক।

## APK বানানো
GitHub repository-তে সব ফাইল আপলোড করলে **Actions → Build APK** নিজে APK বানায়।
Artifacts → ghonti-apk থেকে APK নামান।

## Firebase সেটআপ (একবার)
1. console.firebase.google.com → Add project।
2. **Authentication → Sign-in method**: Email/Password ও Google চালু করুন।
3. **Firestore Database** → Create database। তারপর Rules ট্যাবে নিচের রুল বসিয়ে Publish করুন:
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```
4. **Project settings → Your apps → Add app**:
   - **Android app**: package name `com.ghonti.app`, SHA-1 হলো:
     `37:62:F5:D4:4E:62:1D:CB:26:F7:E1:E7:EF:C7:73:15:7A:A8:CA:AC`
   - **Web app** (</>): যে firebaseConfig দেখাবে, সেটা `app/src/main/assets/firebase-config.js` ফাইলে বসান।
5. Authentication → Sign-in method → Google → **Web SDK configuration → Web client ID** কপি করে
   `app/src/main/res/values/firebase.xml`-এ `YOUR_WEB_CLIENT_ID...` জায়গায় বসান।
6. ফাইল সেভ (commit) করলে নতুন APK বিল্ড হবে।

## ব্যবহারকারীর সংখ্যা
Firebase Console → Authentication → Users: সব ব্যবহারকারীর তালিকা, সংখ্যা ও যোগ দেওয়ার তারিখ।
