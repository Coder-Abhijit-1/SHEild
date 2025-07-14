package com.example.android.sheild;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

/**
 * Foreground service that continuously listens for shake gestures,
 * and confirms SOS via Volume Down key globally (even when app is in background or phone is locked).
 */
public class ShakeDetectorService extends Service implements SensorEventListener {

    // Sensor-related variables
    private SensorManager sensorManager;
    private Sensor accelerometer;

    // MediaSession to capture volume down globally
    private MediaSessionCompat mediaSession;

    // Constants to define shake detection behavior
    private static final float SHAKE_THRESHOLD = 12.0f;    // Minimum shake acceleration (m/s²)
    private long lastShakeTime = 0;                        // Timestamp of last shake accepted

    // A shared flag to indicate if we're waiting for user to confirm SOS
    public static boolean awaitingConfirmation = false;

    // Timer to automatically cancel SOS confirmation window
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();

        // 🔧 Initialize accelerometer and sensor manager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        handler = new Handler();

        // 🔔 Create persistent notification so service stays alive in background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "sos_channel",
                    "SHEild Background Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, "sos_channel")
                .setContentTitle("SHEild is active")
                .setContentText("Shake detection is running in the background.")
                .setSmallIcon(R.drawable.icon) // Use your custom icon
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // 📌 Start service in foreground to prevent getting killed
        startForeground(1, notification);

        // 🎧 Set up MediaSession to intercept volume button presses
        setupMediaSession();
    }

    /**
     * Setup the media session to receive volume button key events globally
     */
    private void setupMediaSession() {
        mediaSession = new MediaSessionCompat(this, "SHEildSession");

        // 🔁 Handle volume key events
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent keyEvent = mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);

                if (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                        if (awaitingConfirmation) {
                            awaitingConfirmation = false;

                            // 📡 Broadcast SOS confirmation (receiver handles alert logic)
                            sendBroadcast(new Intent("com.example.android.sheild.SOS_CONFIRMED"));

                            Toast.makeText(getApplicationContext(), "SOS Confirmed (Background)", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                return super.onMediaButtonEvent(mediaButtonIntent);
            }
        });

        // 🟢 Important to allow button handling
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);

        // 🎮 Set media session as active to keep it alive and receive key events
        mediaSession.setActive(true);

        mediaSession.setPlaybackState(
                PlaybackStateCompat.fromPlaybackState(new PlaybackState.Builder()
                        .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                        .setState(PlaybackState.STATE_PLAYING, 0, 1.0f)
                        .build())
        );
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "SHEild::ShakeWakeLock");

        wakeLock.acquire(6000);  // keep CPU running for 6 seconds


    }


    /**
     * Called when the service is started — register the shake sensor listener
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 🧠 Register the accelerometer listener to detect shake
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        return START_STICKY;
    }

    /**
     * This method is triggered on any sensor update (here, accelerometer)
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        // ⛳ Get the current x, y, z accelerometer values
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // 📏 Calculate net acceleration excluding gravity
        double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

        long now = System.currentTimeMillis();

        // ✅ Accept shake only if:
        //    1. Acceleration exceeds threshold
        //    2. At least 1 second has passed since last shake
        if (acceleration > SHAKE_THRESHOLD && now - lastShakeTime > 1000) {
            lastShakeTime = now;

            Toast.makeText(this,
                    "Shake detected. Press Volume Down within 5s to confirm SOS alert.",
                    Toast.LENGTH_LONG).show();

            awaitingConfirmation = true;  // 📌 Set confirmation window open

            // 🕓 Cancel confirmation after 5 seconds automatically
            handler.postDelayed(() -> awaitingConfirmation = false, 5000);
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this app
    }

    /**
     * Stop listening to sensors and release media session on service destroy
     */
    @Override
    public void onDestroy() {
        sensorManager.unregisterListener(this);
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;  // not a bound service
    }
}
