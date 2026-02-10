package com.example.geschenkplaner.activity;

// Android-Imports für Navigation, UI und Hilfsklassen
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// Basis-Klasse für Activities
import androidx.appcompat.app.AppCompatActivity;

// Eigene App-Klassen und Ressourcen
import com.example.geschenkplaner.MainActivity;
import com.example.geschenkplaner.R;

// Firebase-Klassen für Registrierung und Benutzerprofil
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

//RegisterActivity ist für die Registrierung neuer Nutzer zuständig.

 public class RegisterActivity extends AppCompatActivity {

    // FirebaseAuth-Instanz zur Benutzerregistrierung
    private FirebaseAuth auth;

    // Eingabefelder für Benutzername, E-Mail und Passwörter
    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etPassword2;

    /**
     * Wird beim Start der Activity aufgerufen.
     * Initialisiert Layout, UI-Elemente und Listener.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verknüpft die Activity mit dem Register-Layout
        setContentView(R.layout.activity_register);

        // Referenzen auf die Eingabefelder aus dem Layout holen
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);

        // Button zum Registrieren und Text zum Wechseln zum Login
        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvToLogin = findViewById(R.id.tvToLogin);

        // FirebaseAuth initialisieren
        auth = FirebaseAuth.getInstance();

        // Klick auf Zum Login → Wechsel zur LoginActivity
        tvToLogin.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish(); // verhindert Zurückspringen zur RegisterActivity
        });

        // Klick auf Registrieren → Registrierung starten
        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    /**
     * Versucht einen neuen Nutzer zu registrieren.
     * Prüft zuerst alle Eingaben und nutzt anschließend FirebaseAuth.
     */
    private void attemptRegister() {

        // Eingaben aus den Feldern auslesen
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String p1 = etPassword.getText().toString();
        String p2 = etPassword2.getText().toString();

        // Prüfen, ob alle Felder ausgefüllt sind
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(p1) || TextUtils.isEmpty(p2)) {
            Toast.makeText(this, "Bitte alle Felder ausfüllen.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Benutzername muss mindestens 3 Zeichen lang sein
        if (username.length() < 3) {
            Toast.makeText(this, "Benutzername muss mindestens 3 Zeichen haben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // E-Mail-Format prüfen
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Bitte gültige E-Mail eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Passwortlänge prüfen
        if (p1.length() < 6) {
            Toast.makeText(this, "Passwort muss mindestens 6 Zeichen haben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prüfen, ob beide Passwörter übereinstimmen
        if (!p1.equals(p2)) {
            Toast.makeText(this, "Passwörter stimmen nicht überein.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Benutzer mit E-Mail und Passwort bei Firebase erstellen
        auth.createUserWithEmailAndPassword(email, p1)
                .addOnCompleteListener(task -> {

                    // Registrierung fehlgeschlagen
                    if (!task.isSuccessful()) {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage()
                                : "Registrierung fehlgeschlagen.";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Aktuellen Firebase-Benutzer holen
                    FirebaseUser user = auth.getCurrentUser();

                    // Falls kein Benutzerobjekt existiert, direkt zur MainActivity
                    if (user == null) {
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                        return;
                    }

                    // Username im Firebase-Profil speichern
                    UserProfileChangeRequest profileUpdates =
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                    // Profil aktualisieren und danach zur MainActivity wechseln
                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(profileTask -> {
                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                finish();
                            });
                });
    }
}
