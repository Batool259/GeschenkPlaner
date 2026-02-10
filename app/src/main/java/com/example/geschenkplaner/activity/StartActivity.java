package com.example.geschenkplaner.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.MainActivity;
import com.google.firebase.auth.FirebaseAuth;

//StartActivity dient als Einstiegspunkt der App
public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // FirebaseAuth-Instanz holen, um den aktuellen Login-Status zu prüfen
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Prüfen, ob bereits ein Benutzer eingeloggt ist
        if (auth.getCurrentUser() != null) {
            // Falls ein Benutzer existiert, direkt die MainActivity starten
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // Falls kein Benutzer eingeloggt ist, zur LoginActivity wechseln
            startActivity(new Intent(this, LoginActivity.class));
        }

        // StartActivity beenden, damit sie nicht im Backstack bleibt
        finish();
    }
}

