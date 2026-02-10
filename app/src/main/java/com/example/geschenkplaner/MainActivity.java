package com.example.geschenkplaner;

// Android-Imports für Navigation, Lifecycle und Menü
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

// AndroidX-Imports für Activity und Fragment-Handling
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

// Eigene Fragmente (Unterseiten der App)
import com.example.geschenkplaner.Fragments.AddPersonFragment;
import com.example.geschenkplaner.Fragments.CalendarFragment;
import com.example.geschenkplaner.Fragments.HomeFragment;
import com.example.geschenkplaner.Fragments.SettingsFragment;
import com.example.geschenkplaner.Fragments.ToolbarConfig;

// Login-Activity für nicht eingeloggte User
import com.example.geschenkplaner.activity.LoginActivity;

// Material Toolbar
import com.google.android.material.appbar.MaterialToolbar;

// Firebase Auth zur Prüfung des Login-Status
import com.google.firebase.auth.FirebaseAuth;

/**
 * MainActivity ist der Haupt-Container der App nach dem Login.
 * Sie enthält die Toolbar und lädt je nach Menüauswahl unterschiedliche Fragmente
 * (Home, Person hinzufügen, Kalender, Einstellungen).
 */
public class MainActivity extends AppCompatActivity {

    // Toolbar, die oben in der App angezeigt wird
    private MaterialToolbar toolbar;

    // Intent-Extra, um beim Start gezielt ein bestimmtes Fragment zu öffnen
    // Muss exakt zu den 4 Unterseiten passen
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";

    // Mögliche Zielwerte für EXTRA_OPEN_FRAGMENT
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    /**
     * Wird beim Start der Activity aufgerufen.
     * Prüft zuerst, ob ein User eingeloggt ist. Falls nicht, wird zur LoginActivity umgeleitet.
     * Danach wird die Toolbar eingerichtet und das Start-Fragment geladen.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auth-Gate: Ohne eingeloggten Nutzer darf man nicht in die MainActivity
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // LoginActivity starten und Backstack löschen (User kann nicht zurück)
            Intent i = new Intent(this, LoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }

        // Layout der MainActivity laden
        setContentView(R.layout.activity_main);

        // Toolbar aus dem Layout holen und als ActionBar setzen
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Navigation-Icon deaktivieren (kein "Zurück"-Pfeil, weil Top-Level Navigation)
        toolbar.setNavigationIcon(null);
        toolbar.setNavigationOnClickListener(null);

        // Auch über ActionBar sicherstellen, dass kein Back-Button angezeigt wird
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }

        // Listener: Wenn sich der Fragment-Backstack ändert, Toolbar-Titel neu setzen
        getSupportFragmentManager()
                .addOnBackStackChangedListener(this::applyToolbarFromCurrentFragment);

        // Wenn Activity neu gestartet wurde
        if (savedInstanceState == null) {
            // Startfragment kann per Intent bestimmt werden (z.B. "calendar" öffnen)
            handleStartNavigation(getIntent());
        } else {
            // Bei Restore den Toolbar-Titel passend zum aktuellen Fragment setzen
            applyToolbarFromCurrentFragment();
        }
    }

    //Wird aufgerufen, wenn die MainActivity bereits läuft und ein neues Intent bekommt
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // Das neue Intent speichern, damit getIntent() aktuell bleibt
        setIntent(intent);

        // Das gewünschte Startfragment anhand des Intents öffnen
        handleStartNavigation(intent);
    }

    /**
     * Liest aus dem Intent, welches Fragment geöffnet werden soll.
     * Falls nichts angegeben ist, wird standardmäßig Home geladen.
     */
    private void handleStartNavigation(Intent intent) {
        // Intent-Extra auslesen (kann null sein)
        String target = intent != null ? intent.getStringExtra(EXTRA_OPEN_FRAGMENT) : null;

        // Je nach Zielwert entsprechendes Fragment laden
        if (FRAG_ADD_PERSON.equals(target)) {
            replaceFragment(new AddPersonFragment());
        } else if (FRAG_CALENDAR.equals(target)) {
            replaceFragment(new CalendarFragment());
        } else if (FRAG_SETTINGS.equals(target)) {
            replaceFragment(new SettingsFragment());
        } else {
            // Standard: Home
            replaceFragment(new HomeFragment());
        }
    }

    //Erstellt das Options-Menü (Toolbar-Menü)

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    //Reagiert auf Klicks im Menü und wechselt zu den entsprechenden Fragmenten
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // Falls ein allgemeiner Menüpunkt existiert (Platzhalter), wird hier abgefangen
        if (id == R.id.action_menu) {
            return true;
        }

        // Menü-Navigation: je nach Auswahl Fragment ersetzen
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

    //Ersetzt das aktuell angezeigte Fragment im fragmentContainer
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                // Nach erfolgreichem Fragment-Wechsel Toolbar-Titel aktualisieren
                .runOnCommit(this::applyToolbarFromCurrentFragment)
                .commit();
    }

    /**
     * Setzt den Toolbar-Titel passend zum aktuell sichtbaren Fragment.
     * Falls das Fragment ToolbarConfig implementiert, wird dessen Titel verwendet.
     */
    private void applyToolbarFromCurrentFragment() {
        // Aktuelles Fragment aus dem Container holen
        Fragment f = getSupportFragmentManager()
                .findFragmentById(R.id.fragmentContainer);

        // Standardtitel, falls Fragment keinen eigenen Titel liefert
        String title = "GeschenkPlaner";

        // Falls Fragment ToolbarConfig implementiert, Titel dynamisch holen
        if (f instanceof ToolbarConfig) {
            title = ((ToolbarConfig) f).getToolbarTitle();
        }

        // Titel in der ActionBar setzen
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        } else {
            toolbar.setTitle(title);
        }
    }

    //Öffnet das AddPersonFragment
    public void navigateToAddPerson() {
        replaceFragment(new AddPersonFragment());
    }

    //Öffnet das HomeFragment (z.B. nach dem Speichern einer Person)
    public void openHome() {
        replaceFragment(new HomeFragment());
    }
}
