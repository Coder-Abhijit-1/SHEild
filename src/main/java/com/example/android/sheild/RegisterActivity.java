package com.example.android.sheild;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Registers user with email+password and stores name / phone in Firestore.
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etPhone, etEmail, etPassword;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RadioGroup radioGenderGroup;


    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        /* UI refs */
        etName     = findViewById(R.id.editTextName);
        etPhone    = findViewById(R.id.editTextPhone);
        etEmail    = findViewById(R.id.editTextEmail);
        etPassword = findViewById(R.id.editTextPassword);
        Button btnRegister  = findViewById(R.id.btnRegister);
        TextView tvLogin    = findViewById(R.id.tvAlreadyHaveAccount);
        radioGenderGroup = findViewById(R.id.radioGenderGroup);


        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass  = etPassword.getText().toString().trim();
            String name  = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            int selectGenderId = radioGenderGroup.getCheckedRadioButtonId();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) ||
                    TextUtils.isEmpty(name) || TextUtils.isEmpty(phone)) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Enter a valid email");
                return;
            }

            if (pass.length() < 6) {
                etPassword.setError("Password must be at least 6 characters");
                return;
            }

            // 1) Read and validate gender first
            int selectedGenderId = radioGenderGroup.getCheckedRadioButtonId();
            if (selectedGenderId == -1) {
                Toast.makeText(this, "Please select your gender", Toast.LENGTH_SHORT).show();
                return;
            }
            RadioButton selectedGenderBtn = findViewById(selectedGenderId);
            String gender = selectedGenderBtn.getText().toString();

            auth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser currentUser = auth.getCurrentUser();
                            if (currentUser == null) {
                                Toast.makeText(this, "Registration failed unexpectedly", Toast.LENGTH_LONG).show();
                                return;
                            }

                            String uid = ((FirebaseUser) currentUser).getUid();
                            UserModel user = new UserModel(name, phone, email,gender);

                            db.collection("Users").document(uid)
                                    .set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(this, HomeActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "User saved but Firestore failed: " + e.getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            String errorMsg = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Registration failed.";
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
        tvLogin.setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // User is already logged in → skip login
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        }
    }

    /* simple POJO for Firestore */
    static class UserModel {
        public String name, phone, email, gender;
        UserModel() {}
        UserModel(String n, String p, String e, String g) {
            name = n; phone = p; email = e; gender = g;
        }
    }

}
