package com.example.android.sheild;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private ImageView profileAvatar;
    private TextView tvName, tvPhone, tvEmail;
    private Button btnEditProfile, btnTrustedContacts, btnToggleTheme, btnLogout;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        preferences = getSharedPreferences("theme_pref", MODE_PRIVATE);
        boolean isDarkMode = preferences.getBoolean("dark_mode", false);

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        initViews();
        loadUserDetails();

        btnEditProfile.setOnClickListener(v ->
                startActivity(new Intent(this, EditProfileActivity.class)));

        btnTrustedContacts.setOnClickListener(v ->
                startActivity(new Intent(this, TrustedContactsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Confirm Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(R.drawable.baseline_logout_24) // Optional: your logout icon
                    .show();
        });


        btnToggleTheme.setOnClickListener(v -> toggleDarkMode());
    }

    private void toggleDarkMode() {
        boolean isDarkMode = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;

        SharedPreferences.Editor editor = getSharedPreferences("theme_pref", MODE_PRIVATE).edit();
        editor.putBoolean("dark_mode", !isDarkMode);
        editor.apply();

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_NO : AppCompatDelegate.MODE_NIGHT_YES
        );

        recreate(); // 🔁 Force UI refresh with new theme
    }



    private void initViews() {
        profileAvatar = findViewById(R.id.profileAvatar);
        tvName = findViewById(R.id.tvProfileName);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvEmail = findViewById(R.id.tvProfileEmail);

        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnTrustedContacts = findViewById(R.id.btnTrustedContacts);
        btnToggleTheme = findViewById(R.id.btnToggleTheme);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void loadUserDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Users").document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("name");
                    String phone = doc.getString("phone");
                    String email = doc.getString("email");
                    String gender = doc.getString("gender");

                    tvName.setText(name != null ? name : "User");
                    tvPhone.setText(phone != null ? phone : "Phone not set");
                    tvEmail.setText(email != null ? email : "Email not set");

                    int avatarRes = R.drawable.avatar_neutral;
                    if ("Female".equalsIgnoreCase(gender)) avatarRes = R.drawable.avatar_female;
                    else if ("Male".equalsIgnoreCase(gender)) avatarRes = R.drawable.avatar_male;

                    profileAvatar.setImageResource(avatarRes);
                });
    }
}
