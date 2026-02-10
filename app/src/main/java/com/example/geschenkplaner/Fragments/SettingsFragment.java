package com.example.geschenkplaner.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.geschenkplaner.activity.LoginActivity;
import com.example.geschenkplaner.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment implements ToolbarConfig {

    /**
     * Wird von der MainActivity abgefragt, um den Toolbar-Titel zu setzen.
     */
    @Override
    public String getToolbarTitle() {
        return "Einstellungen";
    }

    /**
     * onCreateView:
     * - Fragment-Layout aufbauen
     * - Views verbinden
     * - Button-Logik setzen
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Layout des Fragments laden
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        // UI-Elemente aus dem Layout holen
        TextView tvUser = v.findViewById(R.id.tvUser);
        Button btnLogout = v.findViewById(R.id.btnLogout);
        Button btnReset = v.findViewById(R.id.btnResetPassword);

        // Aktuell eingeloggten Firebase-User holen
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        // Benutzerinfo anzeigen (E-Mail)
        if (user != null && user.getEmail() != null) {
            tvUser.setText("Eingeloggt als: " + user.getEmail());
        } else {
            tvUser.setText("Nicht eingeloggt");
        }

        /**
         * Logout-Button:
         * - Firebase-Session beenden
         * - LoginActivity neu starten
         * - Backstack leeren (User kann nicht zurück)
         */
        btnLogout.setOnClickListener(x -> {
            FirebaseAuth.getInstance().signOut();

            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);

            // Aktuelle Activity sicher schließen
            requireActivity().finish();
        });

        /**
         * Passwort-Reset:
         * - schickt Reset-Mail an die gespeicherte E-Mail-Adresse
         */
        btnReset.setOnClickListener(x -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();

            // Sicherheit: ohne Login / E-Mail kein Reset möglich
            if (u == null || u.getEmail() == null) {
                Toast.makeText(getContext(), "Bitte einloggen.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(u.getEmail())
                    .addOnSuccessListener(a ->
                            Toast.makeText(getContext(),
                                    "Reset-Mail wurde gesendet ✅",
                                    Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(),
                                    "Fehler: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
        });

        // View an das System zurückgeben
        return v;
    }
}
