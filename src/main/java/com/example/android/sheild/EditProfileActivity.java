package com.example.android.sheild;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etName, etPhone;
    private RadioGroup radioGender;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String currentGender = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        etName = findViewById(R.id.editTextName);
        etPhone = findViewById(R.id.editTextPhone);
        radioGender = findViewById(R.id.radioGender);
        Button btnSave = findViewById(R.id.btnSave);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        loadUserData();

        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserData() {
        String uid = auth.getCurrentUser().getUid();

        db.collection("Users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (!document.exists()) return;

                    // --- text fields ---
                    etName.setText(document.getString("name"));
                    etPhone.setText(document.getString("phone"));

                    // --- gender & avatar ---
                    currentGender = document.getString("gender");     // "Male" / "Female" / "Other"
                    ImageView avatar = findViewById(R.id.imageAvatar);   // <-- your ImageView id

                    int avatarRes = R.drawable.avatar_neutral;              // default
                    if ("Male".equalsIgnoreCase(currentGender)) {
                        avatarRes = R.drawable.avatar_male;
                        radioGender.check(R.id.radioMale);
                    } else if ("Female".equalsIgnoreCase(currentGender)) {
                        avatarRes = R.drawable.avatar_female;
                        radioGender.check(R.id.radioFemale);
                    } else {
                        radioGender.check(R.id.radioOther);
                    }
                    avatar.setImageResource(avatarRes);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }


    private void saveProfileChanges() {
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        int selectedId = radioGender.getCheckedRadioButtonId();
        String gender = "";
        if (selectedId != -1) {
            RadioButton selectedRadio = findViewById(selectedId);
            gender = selectedRadio.getText().toString();
        }

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(phone) || TextUtils.isEmpty(gender)) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("gender", gender);

        String uid = auth.getCurrentUser().getUid();
        db.collection("Users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                    finish(); // go back
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
