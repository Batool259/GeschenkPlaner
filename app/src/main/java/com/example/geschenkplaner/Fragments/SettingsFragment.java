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

import com.example.geschenkplaner.LoginActivity;
import com.example.geschenkplaner.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);

        TextView tvUser = v.findViewById(R.id.tvUser);
        Button btnLogout = v.findViewById(R.id.btnLogout);
        Button btnReset = v.findViewById(R.id.btnResetPassword);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            tvUser.setText("Eingeloggt als: " + user.getEmail());
        } else {
            tvUser.setText("Nicht eingeloggt");
        }

        btnLogout.setOnClickListener(x -> {
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            requireActivity().finish();
        });

        btnReset.setOnClickListener(x -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null || u.getEmail() == null) {
                Toast.makeText(getContext(), "Bitte einloggen.", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseAuth.getInstance()
                    .sendPasswordResetEmail(u.getEmail())
                    .addOnSuccessListener(a -> Toast.makeText(getContext(),
                            "Reset-Mail wurde gesendet ✅", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getContext(),
                            "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

        return v;
    }
}
