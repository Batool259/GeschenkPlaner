package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etPassword2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Views holen
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvToLogin = findViewById(R.id.tvToLogin);

        // Firebase initialisieren
        auth = FirebaseAuth.getInstance();

        // Zurück zum Login
        tvToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        // Registrieren
        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String p1 = etPassword.getText().toString();
            String p2 = etPassword2.getText().toString();

            // Validierung
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(p1) || TextUtils.isEmpty(p2)) {
                Toast.makeText(this, "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Bitte gültige E-Mail eingeben.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (p1.length() < 6) {
                Toast.makeText(this, "Passwort muss mindestens 6 Zeichen haben.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!p1.equals(p2)) {
                Toast.makeText(this, "Passwörter stimmen nicht überein.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Firebase User anlegen
            auth.createUserWithEmailAndPassword(email, p1)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Registrierung erfolgreich!", Toast.LENGTH_SHORT).show();

                            // Nach erfolgreicher Registrierung direkt in die App
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Registrierung fehlgeschlagen.";
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}
