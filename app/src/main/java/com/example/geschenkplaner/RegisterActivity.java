
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

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private EditText etPassword2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPassword2 = findViewById(R.id.etPassword2);

        Button btnRegister = findViewById(R.id.btnRegister);
        TextView tvToLogin = findViewById(R.id.tvToLogin);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String p1 = etPassword.getText().toString();
            String p2 = etPassword2.getText().toString();

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

            AuthManager.register(this, email, p1);
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        tvToLogin.setOnClickListener(v -> finish());
    }
}
