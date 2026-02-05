package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import Fragments.CalendarFragment;
import Fragments.HomeFragment;
import Fragments.PersonListFragment;
import Fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_ITEM = "selected_bottom_item";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Login-Check
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            // optional aber sauber: verhindert "Zurück" in MainActivity
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                replaceFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_calendar) {
                replaceFragment(new CalendarFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                replaceFragment(new SettingsFragment());
                return true;
            } else if (id == R.id.nav_persons) {
                replaceFragment(new PersonListFragment());
                return true;
            }

            return false;
        });

        if (savedInstanceState != null) {
            int selected = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.nav_home);
            bottomNav.setSelectedItemId(selected);
        }
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        outState.putInt(KEY_SELECTED_ITEM, bottomNav.getSelectedItemId());
    }
}
