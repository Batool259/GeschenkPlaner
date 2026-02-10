package com.example.geschenkplaner.activity;

// Imports für Android-Komponenten und Hilfsklassen
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

// Basis-Klasse für Activities mit AppCompat-Unterstützung
import androidx.appcompat.app.AppCompatActivity;

// Eigene App-Klassen und Ressourcen
import com.example.geschenkplaner.MainActivity;
import com.example.geschenkplaner.R;

// Firebase-Klasse für Authentifizierung
import com.google.firebase.auth.FirebaseAuth;


public class LoginActivity extends AppCompatActivity {

    // FirebaseAuth-Instanz zur Verwaltung der Authentifizierung
    private FirebaseAuth auth;

    // Eingabefelder für E-Mail und Passwort
    private EditText etEmail;
    private EditText etPassword;

    /**
     * Wird aufgerufen, wenn die Activity gestartet wird.
     * Initialisiert UI-Komponenten und Listener.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verknüpft die Activity mit dem zugehörigen XML-Layout
        setContentView(R.layout.activity_login);

        // Referenzen auf die Eingabefelder aus dem Layout holen
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);

        // FirebaseAuth initialisieren (Singleton-Instanz)
        auth = FirebaseAuth.getInstance();

        // Klickbarer Text zum Wechseln in die RegisterActivity
        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));

        // Login-Button initialisieren
        Button btnLogin = findViewById(R.id.btnLogin);

        // Klick auf Login-Button startet den Login-Vorgang
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    //Prüft zuerst die Eingaben und nutzt anschließend FirebaseAuth
    private void attemptLogin() {

        // E-Mail und Passwort aus den Eingabefeldern auslesen
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Prüfen, ob eines der Felder leer ist
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            // Rückmeldung an den Nutzer bei fehlenden Eingaben
            Toast.makeText(this, "Bitte E-Mail und Passwort eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Login-Versuch über Firebase Authentication (asynchron)
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    // Login erfolgreich
                    if (task.isSuccessful()) {
                        // Wechsel zur MainActivity
                        startActivity(new Intent(this, MainActivity.class));

                        finish();
                    }
                    // Login fehlgeschlagen
                    else {
                        // Fehlermeldung aus der Exception lesen, falls vorhanden
                        String msg = (task.getException() != null && task.getException().getMessage() != null)
                                ? task.getException().getMessage()
                                : "Unbekannter Fehler";

                        // Rückmeldung an den Nutzer über eine Fehlermeldung
                        Toast.makeText(this, "Login fehlgeschlagen: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
