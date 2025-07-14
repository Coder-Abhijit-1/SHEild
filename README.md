# SHEild - Women's Safety App 🚨

**SHEild** is an Android application designed to enhance personal safety for women by providing real-time emergency alert features. The app allows users to quickly notify trusted contacts via SMS, sharing their live location in distress situations using intuitive triggers like device shake + volume-down or a one-tap panic button.

---

## 🔧 Tech Stack

- **Android** (Java)
- **Firebase** (Authentication, Firestore)
- **Google Location API**
- **SMSManager**
- **Material Design Components**

---

## ✨ Features

- **Emergency SOS Alerts**: Shake + Volume-down or press panic button to send alerts.
- **Live Location Sharing**: Sends real-time Google Maps location via SMS to trusted contacts.
- **Trusted Contacts Management**: Add, update, and delete trusted contacts in-app.
- **Gesture-Based Triggers**: Detects device shake and confirms with volume-down key.
- **Home Screen Widget**: Trigger SOS alert instantly without opening the app.
- **Dark Mode Support**: Switch between light and dark themes.
- **User Profile**: Avatar-based profile with editable personal details.
- **Permission Handling**: Manages location and SMS permissions gracefully.

---

## 📦 Folder Structure

SHEild/
├── app/
│ ├── java/
│ │ └── com.example.android.sheild/
│ │ ├── HomeActivity.java
│ │ ├── ProfileActivity.java
│ │ ├── TrustedContactsActivity.java
│ │ ├── ContactAdapter.java
│ │ └── ContactModel.java
│ └── res/
│ ├── layout/
│ ├── drawable/
│ └── values/
├── AndroidManifest.xml
└── README.md



---

## 🛡️ Permissions Used

- `ACCESS_FINE_LOCATION` – for fetching live location.
- `SEND_SMS` – to send emergency messages.
- `VIBRATE` – to give vibration feedback on trigger.

---

## 🧪 Testing

The app has been tested on real Android devices with working SIM cards and verified SMS delivery using live location. Make sure the phone used for testing has active SMS services.

---

## 📌 Setup Instructions

1. Clone the repository.
2. Open in Android Studio.
3. Add your Firebase project `google-services.json` file in `app/`.
4. Ensure SMS and Location permissions are granted.
5. Run the app on a physical Android device.

---

## 🧑‍💻 Author

**Abhijit Mondal**

> Final year B.Tech student passionate about building impactful, real-world Android applications.

---

## 📄 License

This project is for academic and learning purposes. Commercial use is not permitted without the author's consent.


