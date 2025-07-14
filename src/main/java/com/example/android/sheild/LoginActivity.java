package com.example.android.sheild;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lets user login with:
 * 1) Email + Password  OR
 * 2) Phone number (OTP)
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etPhoneOrEmail, etPassword;
    private FirebaseAuth auth;

    private String userInput = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        etPhoneOrEmail = findViewById(R.id.editTextPhoneOrEmail);
        etPassword     = findViewById(R.id.editTextPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegister = findViewById(R.id.tvCreateAccount);

        auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
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

    private void attemptLogin() {
        userInput = etPhoneOrEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(userInput)) {
            etPhoneOrEmail.setError("Enter email or phone");
            return;
        }

        // -------- CASE 1: EMAIL --------
        if (Patterns.EMAIL_ADDRESS.matcher(userInput).matches()) {

            if (TextUtils.isEmpty(pass)) {
                etPassword.setError("Enter password");
                return;
            }

            /* 1️⃣ Check if this email exists */
            auth.fetchSignInMethodsForEmail(userInput)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<String> methods = task.getResult().getSignInMethods();
                            if (methods == null || methods.isEmpty()) {
                                Toast.makeText(this,
                                        "Account not found. Please register first.",
                                        Toast.LENGTH_LONG).show();
                                return;
                            }

                            /* 2️⃣ Email exists → attempt login */
                            auth.signInWithEmailAndPassword(userInput, pass)
                                    .addOnCompleteListener(t -> {
                                        if (t.isSuccessful()) {
                                            goHome();
                                        } else {
                                            Toast.makeText(this,
                                                    "Authentication failed: "
                                                            + t.getException().getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(this,
                                    "Error: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
        // -------- CASE 2: PHONE --------
        else if (userInput.length() >= 10) {

            /* Check Firestore if a user document with this phone exists */
            FirebaseFirestore.getInstance()
                    .collection("Users")
                    .whereEqualTo("phone", userInput.startsWith("+") ? userInput : "+91" + userInput)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.isEmpty()) {
                            Toast.makeText(this,
                                    "Account with this phone not found. Please register.",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            startPhoneVerification(userInput); // continue with OTP
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        }
        // -------- INVALID INPUT --------
        else {
            etPhoneOrEmail.setError("Enter a valid email or phone");
        }
    }


    /* ----- Phone Auth logic ----- */
    private void startPhoneVerification(String phoneNumberRaw) {
        String phone = phoneNumberRaw.startsWith("+") ? phoneNumberRaw : "+91" + phoneNumberRaw;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);

        Toast.makeText(this, "OTP sent to " + phone, Toast.LENGTH_SHORT).show();
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    /* Auto-retrieval or instant verification */
                    auth.signInWithCredential(credential)
                            .addOnCompleteListener(t -> {
                                if (t.isSuccessful()) goHome();
                            });
                }

                @Override public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(LoginActivity.this,
                            "Phone auth failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override public void onCodeSent(@NonNull String verificationId,
                                                 @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    /* Launch OTP screen */
                    Intent i = new Intent(LoginActivity.this, OtpActivity.class);
                    i.putExtra("verificationId", verificationId);
                    i.putExtra("resendToken", token);
                    i.putExtra("phoneNumber", userInput); // optional
                    startActivity(i);

                }
            };

    private void goHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }
}
