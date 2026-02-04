package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;

    // UI
    private TextView tvGreeting;
    private TextView tvNextEventTitle;
    private TextView tvNextEventDate;

    private Button btnOpenPersons;

    public HomeFragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views verbinden
        tvGreeting = view.findViewById(R.id.tvGreeting);
        tvNextEventTitle = view.findViewById(R.id.tvNextEventTitle);
        tvNextEventDate = view.findViewById(R.id.tvNextEventDate);
        btnOpenPersons = view.findViewById(R.id.btnOpenPersons);

        // Firebase
        auth = FirebaseAuth.getInstance();

        // Begrüßung setzen
        setGreeting();

        // Platzhalter für nächsten Termin
        setNextEventPlaceholder();

        // Navigation
        btnOpenPersons.setOnClickListener(v -> openPersons());
    }

    private void setGreeting() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            tvGreeting.setText("Hallo 👋");
            return;
        }

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        if (displayName != null && !displayName.trim().isEmpty()) {
            tvGreeting.setText("Hallo, " + displayName + " 👋");
        } else if (email != null && !email.trim().isEmpty()) {
            tvGreeting.setText("Hallo, " + email + " 👋");
        } else {
            tvGreeting.setText("Hallo 👋");
        }
    }

    private void setNextEventPlaceholder() {
        tvNextEventTitle.setText("Noch keine Termine/Geburtstage");
        tvNextEventDate.setText("Füge einen Termin im Kalender hinzu.");
    }

    private void openPersons() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new PersonListFragment())
                .addToBackStack(null)
                .commit();
    }

    private void openCalendar() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, new CalendarFragment())
                .addToBackStack(null)
                .commit();
    }

    private void addPerson() {
        startActivity(new Intent(requireContext(), AddPersonActivity.class));
    }
}
