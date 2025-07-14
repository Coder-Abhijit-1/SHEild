package com.example.android.sheild;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

/**
 * HOME SCREEN
 * ------------
 * 1. Provides Shake → Volume‑Down SOS trigger (with vibration + SMS).
 * 2. Shows Navigation Drawer with profile & options (Edit, Contacts, Dark‑Mode, Logout).
 */
public class HomeActivity extends AppCompatActivity implements SensorEventListener {

    /* ---------- Constants ---------- */
    private static final int PERMISSION_REQUEST = 1001;
    private static final float SHAKE_THRESHOLD = 12f;           // Acceleration threshold
    private static final long SHAKE_COOLDOWN_MS = 1000;         // 1 sec cooldown between shakes
    private static final long CONFIRM_WINDOW_MS = 5000;         // 5 sec for Volume Down confirmation

    /* ---------- UI Components ---------- */
    private MaterialButton btnBroadcast;
    private ImageView avatarToolbar;

    /* ---------- Sensors ---------- */
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private boolean awaitingConfirmation = false;

    /* ---------- Location ---------- */
    private FusedLocationProviderClient fusedLocationClient;

    /* ---------- onCreate ---------- */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /* Light status bar */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        /* Initialize Views */
        btnBroadcast = findViewById(R.id.btnBroadcast);
        avatarToolbar = findViewById(R.id.userAvatar);

        /* Set Click Listeners */
        btnBroadcast.setOnClickListener(v -> {
            Toast.makeText(this, "🚨 Manual SOS triggered.", Toast.LENGTH_SHORT).show();
            vibrateDevice(400);
            sendLocationSmsToTrustedContacts();
        });

        avatarToolbar.setOnClickListener(v -> {
            // Navigate to ProfileActivity
            startActivity(new Intent(this, ProfileActivity.class));
        });

        /* Animate Shake Icon */
        ImageView shakeImage = findViewById(R.id.imageShake);
        Animation shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake);
        shakeImage.startAnimation(shakeAnim);

        /* Sensor & Location Setup */
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this); // Use activity context

        /* Permissions */
        requestPermissionsIfNeeded();

        /* Populate User Info */
        populateUserData();
    }

    /* ---------- Load Avatar & Name from Firestore ---------- */
    private void populateUserData() {
        ImageView avatar = findViewById(R.id.userAvatar);
        TextView userName = findViewById(R.id.tvUserName);

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance().collection("Users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String gender = doc.getString("gender");

                    userName.setText(name != null ? "Hello, " + name : "Hello, User");

                    int avatarRes = R.drawable.avatar_neutral;
                    if ("Female".equalsIgnoreCase(gender)) avatarRes = R.drawable.avatar_female;
                    else if ("Male".equalsIgnoreCase(gender)) avatarRes = R.drawable.avatar_male;

                    avatar.setImageResource(avatarRes);
                });
    }

    /* ---------- Sensor Logic ---------- */
    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null)
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0], y = event.values[1], z = event.values[2];
        double accel = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        long now = System.currentTimeMillis();
        if (accel > SHAKE_THRESHOLD && (now - lastShakeTime) > SHAKE_COOLDOWN_MS) {
            lastShakeTime = now;
            awaitingConfirmation = true;

            Toast.makeText(this, "Shake detected! Press Volume Down within 5s to confirm SOS.", Toast.LENGTH_LONG).show();

            getWindow().getDecorView().postDelayed(() -> awaitingConfirmation = false, CONFIRM_WINDOW_MS);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    /* ---------- Volume Down Key Confirmation ---------- */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && awaitingConfirmation) {
            awaitingConfirmation = false;
            vibrateDevice(400);
            Toast.makeText(this, "✅ SOS confirmed – sending alert!", Toast.LENGTH_SHORT).show();
            sendLocationSmsToTrustedContacts();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /* ---------- Send SOS Alert with Location ---------- */
    private void sendLocationSmsToTrustedContacts() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Location or SMS permission missing!", Toast.LENGTH_LONG).show();
            return;
        }

        @SuppressLint("MissingPermission")
        Task<Location> task = fusedLocationClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            String msg;

            if (location != null) {
                String url = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                msg = "I need help! My location: " + url;
            } else {
                msg = "I need help! Location not available.";
                Toast.makeText(this, "⚠️ Location was null. Sending SOS without location.", Toast.LENGTH_SHORT).show();
            }

            Log.d("SHEild_SMS", "Final message: " + msg);  // Debug print

            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null) return;

            FirebaseFirestore.getInstance()
                    .collection("Users").document(uid)
                    .collection("contacts")
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            Toast.makeText(this, "No trusted contacts found!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int count = 0;
                        for (QueryDocumentSnapshot doc : snapshot) {
                            String phone = doc.getString("phone");
                            if (phone != null && phone.matches("\\+?\\d{10,13}")) {
                                Log.d("SHEild_SMS", "Sending to: " + phone);  // Optional: log for debugging
                                sendSms(phone, msg);
                                count++;
                            }
                        }

                        if (count > 0) {
                            Toast.makeText(this, "✅ SOS alert sent to all trusted contacts.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "⚠️ No valid phone numbers found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to fetch contacts: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void sendSms(String phone, String message) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phone, null, message, null, null);
//            Toast.makeText(this, "Message sent to " + phone, Toast.LENGTH_SHORT).show();
        } catch (SecurityException se) {
            Toast.makeText(this, "Permission error: " + se.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Send failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    /* ---------- Vibrate Utility ---------- */
    private void vibrateDevice(int durationMs) {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(durationMs);
            }
        }
    }

    /* ---------- Permission Helpers ---------- */
    private boolean hasRequiredPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsIfNeeded() {
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS},
                    PERMISSION_REQUEST
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int rc, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(rc, permissions, grantResults);
        if (rc == PERMISSION_REQUEST) {
            if (hasRequiredPermissions()) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions denied – SOS disabled.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
