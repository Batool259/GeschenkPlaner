package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.geschenkplaner.Fragments.AddPersonFragment;
import com.example.geschenkplaner.Fragments.CalendarFragment;
import com.example.geschenkplaner.Fragments.HomeFragment;
import com.example.geschenkplaner.Fragments.SettingsFragment;
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

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("GeschenkPlaner");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // immer sichtbar (laut Anforderung)
        }

        // Start: Home
        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment(), false);
        }

        toolbar.setNavigationOnClickListener(v -> {
            // Zurück-Pfeil: Backstack poppen
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu); // nur das Icon
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_menu) {
            showDropdownMenu();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDropdownMenu() {
        PopupMenu popup = new PopupMenu(this, toolbar);
        popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
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
            return false;
        });

        popup.show();
    }

    public void replaceFragment(Fragment fragment, boolean addToBackstack) {
        var tx = getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment);

        if (addToBackstack) tx.addToBackStack(null);

        tx.commit();
    }

    // Für HomeFragment-FAB
    public void navigateToAddPerson() {
        replaceFragment(new AddPersonFragment(), true);
    }
}
