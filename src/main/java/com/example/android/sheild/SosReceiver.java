package com.example.android.sheild;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class SosReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if ("com.example.android.sheild.SOS_CONFIRMED".equals(intent.getAction())) {
            // ⚠️ Run emergency logic — e.g., start location and SMS service
            Toast.makeText(context, "Emergency! SOS Triggered from background.", Toast.LENGTH_LONG).show();

            // TODO: Create a helper class or foreground service that handles location + SMS
        }
    }
}
