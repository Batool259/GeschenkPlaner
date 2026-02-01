
package com.example.geschenkplaner;

import com.google.firebase.auth.FirebaseAuth;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText etEmail;
    private EditText etPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Views holen
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);


        // Firebase initialisieren
        auth = FirebaseAuth.getInstance(); // offizielle Initialisierung


        TextView tvRegister = findViewById(R.id.tvRegister);
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });



        Button btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();


        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Bitte E-Mail und Passwort eingeben.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Firebase Login (Email/Passwort)
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login erfolgreich → weiter in die MainActivity
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        String msg = (task.getException() != null && task.getException().getMessage() != null)
                                ? task.getException().getMessage()
                                : "Unbekannter Fehler";

                        Toast.makeText(this, "Login fehlgeschlagen: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}