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
import com.example.geschenkplaner.activity.LoginActivity;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;

    // Muss exakt zu den 4 Unterseiten passen
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        getSupportFragmentManager()
                .addOnBackStackChangedListener(this::applyToolbarFromCurrentFragment);

        if (savedInstanceState == null) {
            // Neu: Startfragment kann per Intent kommen
            handleStartNavigation(getIntent());
        } else {
            applyToolbarFromCurrentFragment();
        }
    }

    // Neu: wenn MainActivity schon existiert (SINGLE_TOP) kommt hier das neue Intent an
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleStartNavigation(intent);
    }

    private void handleStartNavigation(Intent intent) {
        String target = intent != null ? intent.getStringExtra(EXTRA_OPEN_FRAGMENT) : null;

        if (FRAG_ADD_PERSON.equals(target)) {
            replaceFragment(new AddPersonFragment());
        } else if (FRAG_CALENDAR.equals(target)) {
            replaceFragment(new CalendarFragment());
        } else if (FRAG_SETTINGS.equals(target)) {
            replaceFragment(new SettingsFragment());
        } else {
            replaceFragment(new HomeFragment());
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

        if (id == R.id.action_menu) {
            return true;
        }

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
                .runOnCommit(this::applyToolbarFromCurrentFragment)
                .commit();
    }

    private void applyToolbarFromCurrentFragment() {
        Fragment f = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        String title = "GeschenkPlaner";
        if (f instanceof ToolbarConfig) {
            title = ((ToolbarConfig) f).getToolbarTitle();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            toolbar.setTitle(title);
        }
    }

    public void navigateToAddPerson() {
        replaceFragment(new AddPersonFragment());
    }

    // ✅ Neu: von überall zurück zur Home
    public void openHome() {
        replaceFragment(new HomeFragment());
    }
}
