package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import com.example.geschenkplaner.Fragments.AddPersonFragment;
import com.example.geschenkplaner.Fragments.CalendarFragment;
import com.example.geschenkplaner.Fragments.HomeFragment;
import com.example.geschenkplaner.Fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Login-Check
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GeschenkPlaner");
            getSupportActionBar().setDisplayHomeAsUpEnabled(
                    getSupportFragmentManager().getBackStackEntryCount() > 0
            );
        }

        // Start: Home
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), false);
        }

        // Back-Pfeil Klick
        toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
        });

        // Backstack Listener -> Pfeil ein/aus
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportActionBar() != null) {
                boolean showBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
                getSupportActionBar().setDisplayHomeAsUpEnabled(showBack);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_home) {
            replaceFragment(new HomeFragment(), false);
            return true;

        } else if (id == R.id.menu_add_person) {
            replaceFragment(new AddPersonFragment(), true);
            return true;

        } else if (id == R.id.menu_calendar) {
            replaceFragment(new CalendarFragment(), true);
            return true;

        } else if (id == R.id.menu_settings) {
            replaceFragment(new SettingsFragment(), true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void replaceFragment(Fragment fragment, boolean addToBackstack) {
        var tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);

        if (addToBackstack) tx.addToBackStack(null);

        tx.commit();
    }

    public void navigateToAddPerson() {
        replaceFragment(new AddPersonFragment(), true);
    }
}
