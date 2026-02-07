package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.geschenkplaner.Fragments.AddPersonFragment;
import com.example.geschenkplaner.Fragments.CalendarFragment;
import com.example.geschenkplaner.Fragments.HomeFragment;
import com.example.geschenkplaner.Fragments.SettingsFragment;
import com.example.geschenkplaner.Fragments.ToolbarConfig;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

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

        // In der MainActivity NIE Up/Back-Pfeil anzeigen
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationIcon(null);

        // Wenn sich der sichtbare Fragment ändert, Toolbar-Titel aktualisieren
        getSupportFragmentManager().addOnBackStackChangedListener(this::applyToolbarFromCurrentFragment);

        // Start: Home
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        } else {
            applyToolbarFromCurrentFragment();
        }
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
            replaceFragment(new HomeFragment());
            return true;
        } else if (id == R.id.menu_add_person) {
            replaceFragment(new AddPersonFragment());
            return true;
        } else if (id == R.id.menu_calendar) {
            replaceFragment(new CalendarFragment());
            return true;
        } else if (id == R.id.menu_settings) {
            replaceFragment(new SettingsFragment());
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                // Kein Backstack -> kein Pfeil,ückweg, bleibt “Top-Level”
                .runOnCommit(this::applyToolbarFromCurrentFragment)
                .commit();
    }

    private void applyToolbarFromCurrentFragment() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);

        String title = "GeschenkPlaner";
        if (f instanceof ToolbarConfig) {
            title = ((ToolbarConfig) f).getToolbarTitle();
        }

        // Toolbar-Titel setzen (MaterialToolbar/Toolbar unterstützt setTitle)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        } else {
            toolbar.setTitle(title);
        }

        // Sicherstellen: in MainActivity nie Pfeil anzeigen
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        toolbar.setNavigationIcon(null);
    }

    public void navigateToAddPerson() {
        replaceFragment(new AddPersonFragment());
    }
}
