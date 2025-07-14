package com.example.android.sheild;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;

import java.util.concurrent.TimeUnit;

/**
 * OTP screen for phone authentication.
 * XML layout: activity_otp.xml
 */
public class OtpActivity extends AppCompatActivity {

    /* ----- UI refs ----- */
    private EditText etOtp;
    private Button btnVerify, btnResend;
    private TextView tvCountdown;

    /* ----- Firebase ----- */
    private FirebaseAuth auth;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;

    /* ----- Constants ----- */
    private static final long OTP_TIMEOUT_SEC = 60;  // resend allowed after 60 s
    private static final long COUNTDOWN_INTERVAL_MS = 1000;

    private CountDownTimer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        /* Initialize Firebase auth */
        auth = FirebaseAuth.getInstance();

        /* Get data from intent */
        verificationId = getIntent().getStringExtra("verificationId");
        resendToken    = getIntent().getParcelableExtra("resendToken");
        final String phoneNumber = getIntent().getStringExtra("phoneNumber"); // passed from Login

        /* Bind UI */
        etOtp        = findViewById(R.id.editTextOtp);
        btnVerify    = findViewById(R.id.btnVerify);
        btnResend    = findViewById(R.id.btnResendOtp);
        tvCountdown  = findViewById(R.id.tvCountdown);

        btnVerify.setOnClickListener(v -> verifyEnteredOtp());
        btnResend.setOnClickListener(v -> resendOtp(phoneNumber));

        startCountdown(); // kick‑off timer immediately
    }

    /* ----- Verify the OTP entered by user ----- */
    private void verifyEnteredOtp() {
        String code = etOtp.getText().toString().trim();
        if (TextUtils.isEmpty(code) || code.length() < 6) {
            etOtp.setError("Enter 6‑digit code");
            return;
        }
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    /* ----- Sign in to Firebase with credential ----- */
    private void signInWithCredential(PhoneAuthCredential credential) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Phone verified!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, HomeActivity.class));
                        finishAffinity();
                    } else {
                        Toast.makeText(this,
                                "Verification failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /* ----- Resend code flow ----- */
    private void resendOtp(String phoneNumberRaw) {
        if (resendToken == null) {
            Toast.makeText(this, "Please wait before resending.", Toast.LENGTH_SHORT).show();
            return;
        }
        String phone = phoneNumberRaw.startsWith("+") ? phoneNumberRaw : "+91" + phoneNumberRaw;

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phone)
                .setTimeout(OTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(callbacks)
                .setForceResendingToken(resendToken)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "OTP resent.", Toast.LENGTH_SHORT).show();
        startCountdown();   // restart countdown
    }

    /* ----- Countdown timer to handle resend button ----- */
    private void startCountdown() {
        btnResend.setEnabled(false);
        if (timer != null) timer.cancel();

        timer = new CountDownTimer(OTP_TIMEOUT_SEC * 1000, COUNTDOWN_INTERVAL_MS) {
            @Override public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Resend in " + (millisUntilFinished / 1000) + " s");
            }
            @Override public void onFinish() {
                tvCountdown.setText("");
                btnResend.setEnabled(true);
            }
        }.start();
    }

    /* ----- Firebase callbacks ----- */
    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    /* Auto‑retrieval or instant verification */
                    signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(OtpActivity.this, "OTP failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    /* Save id and token for future, restart countdown */
                    verificationId = verId;
                    resendToken    = token;
                    startCountdown();
                }
            };

    @Override protected void onDestroy() {
        if (timer != null) timer.cancel();
        super.onDestroy();
    }
}
