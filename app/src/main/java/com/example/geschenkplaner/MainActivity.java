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

    private MaterialToolbar toolbar; // Obere Leiste (Titel + Menü)

    // Labels um gezielt ein Fragment zu öffnen (z.B. per Intent von anderen Activities)
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Wenn nicht eingeloggt -> direkt zur LoginActivity und zurück-Stack leeren
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        setContentView(R.layout.activity_main); // Main-Layout mit Fragment-Container

        toolbar = findViewById(R.id.toolbar); // Toolbar aus Layout holen
        setSupportActionBar(toolbar); // Toolbar als ActionBar nutzen

        // Kein Zurück-Pfeil in der MainActivity
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false); // Up-Button aus
        }

        // Wenn sich das Fragment ändert -> Toolbar-Titel neu setzen
        getSupportFragmentManager()
                .addOnBackStackChangedListener(this::applyToolbarFromCurrentFragment);

        if (savedInstanceState == null) {
            // Beim ersten Start: Fragment ggf. per Intent auswählen
            handleStartNavigation(getIntent());
        } else {
            // Bei Rotation Titel passend zum aktuellen Fragment setzen
            applyToolbarFromCurrentFragment();
        }
    }

    // Kommt, wenn MainActivity schon läuft und ein neues Intent reinkommt
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Intent aktualisieren, damit getIntent() stimmt
        handleStartNavigation(intent); // Direkt zum gewünschten Fragment springen
    }

    private void handleStartNavigation(Intent intent) {
        // Liest, welches Fragment geöffnet werden soll
        String target = intent != null ? intent.getStringExtra(EXTRA_OPEN_FRAGMENT) : null;

        // Je nach Ziel-String das passende Fragment öffnen
        if (FRAG_ADD_PERSON.equals(target)) {
            replaceFragment(new AddPersonFragment());
        } else if (FRAG_CALENDAR.equals(target)) {
            replaceFragment(new CalendarFragment());
        } else if (FRAG_SETTINGS.equals(target)) {
            replaceFragment(new SettingsFragment());
        } else {
            replaceFragment(new HomeFragment()); // Standard: Home
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menü oben rechts laden
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Reagiert auf Klicks im Toolbar-Menü
        int id = item.getItemId();

        if (id == R.id.action_menu) {
            return true; // Falls das nur ein „Platzhalter“-Item ist
        }

        // Navigation zwischen den 4 Hauptseiten (Fragments)
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

        return super.onOptionsItemSelected(item); // Standard-Fall
    }

    private void replaceFragment(Fragment fragment) {
        // Tauscht das aktuell sichtbare Fragment im Container aus
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .runOnCommit(this::applyToolbarFromCurrentFragment) // Titel nach dem Wechsel setzen
                .commit();
    }

    private void applyToolbarFromCurrentFragment() {
        // Holt das aktuell sichtbare Fragment aus dem Container
        Fragment f = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        // Standard-Titel, falls Fragment keinen eigenen liefert
        String title = "GeschenkPlaner";

        // Wenn Fragment ToolbarConfig implementiert -> Fragment bestimmt den Titel selbst
        if (f instanceof ToolbarConfig) {
            title = ((ToolbarConfig) f).getToolbarTitle();
        }

        // Titel in die ActionBar/Toolbar schreiben
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            toolbar.setTitle(title);
        }
    }

    public void navigateToAddPerson() {
        // Öffnet AddPerson
        replaceFragment(new AddPersonFragment());
    }

    public void openHome() {
        // Öffnet Home von überall innerhalb der MainActivity
        replaceFragment(new HomeFragment());
    }
}
