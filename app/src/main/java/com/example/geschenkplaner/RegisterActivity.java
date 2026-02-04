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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etPassword2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Views holen
        etUsername = findViewById(R.id.etUsername);
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
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String p1 = etPassword.getText().toString();
        String p2 = etPassword2.getText().toString();

        // Validierung
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) || TextUtils.isEmpty(p1) || TextUtils.isEmpty(p2)) {
            Toast.makeText(this, "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (username.length() < 3) {
            Toast.makeText(this, "Benutzername muss mindestens 3 Zeichen haben.", Toast.LENGTH_SHORT).show();
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
                    if (!task.isSuccessful()) {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Registrierung fehlgeschlagen.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // displayName (Benutzername) im Firebase-Profil speichern
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) {
                        Toast.makeText(this, "Registrierung erfolgreich, aber Benutzer nicht gefunden.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                if (profileTask.isSuccessful()) {
                                    Toast.makeText(this, "Registrierung erfolgreich!", Toast.LENGTH_SHORT).show();
                                } else {
                                    // Falls updateProfile fehlschlägt, ist der Account trotzdem erstellt.
                                    Toast.makeText(this, "Registrierung erfolgreich, Benutzername konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show();
                                }

                                // Nach erfolgreicher Registrierung direkt in die App
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            });
                });
    }
}
