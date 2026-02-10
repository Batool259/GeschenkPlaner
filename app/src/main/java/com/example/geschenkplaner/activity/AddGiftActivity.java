package com.example.geschenkplaner.activity;

import com.example.geschenkplaner.MainActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class AddGiftActivity extends AppCompatActivity {

    // Intent-Extra-Key: darüber bekommt diese Activity die Person-ID (zu wem das Geschenk gehört).
    public static final String EXTRA_PERSON_ID = "personId";

    // Extra + Konstanten: damit MainActivity weiß, welchen Fragment-Tab sie öffnen soll.
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    // Aktueller Firebase-User (uid) + ausgewählte Person.
    private String uid;
    private String personId;

    // TextInputLayout = „Hülle“ um das Eingabefeld (kann z.B. Fehlermeldungen anzeigen).
    private TextInputLayout tilName, tilPrice, tilLink, tilNote;

    // Eingabefelder (EditText-Variante aus Material Design).
    private TextInputEditText etName;
    private TextInputEditText etPrice;
    private TextInputEditText etLink;  // Website-Link (User) – NICHT das Bild!
    private TextInputEditText etNote;
    private TextInputEditText etStatus; // Status wird nur angezeigt/gesetzt, nicht frei „berechnet“.

    private ImageView ivGiftImage;

    // Status: gekauft oder geplant.
    private boolean bought = false;

    // Bildquelle: entweder Galerie-URI ODER Direkt-URL (nie beides gleichzeitig).
    private Uri selectedImageUri = null;
    private String imageUrl = "";

    // Launcher für das System-Datei/Foto-Auswahlfenster (OpenDocument).
    // Ergebnis ist eine Uri (Verweis auf das ausgewählte Bild).
    private final ActivityResultLauncher<String[]> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return; // User hat abgebrochen

                // Versuch, die Leseberechtigung dauerhaft zu speichern (damit die Uri auch später noch funktioniert).
                // Das ist typisch bei ACTION_OPEN_DOCUMENT / OpenDocument. :contentReference[oaicite:0]{index=0}
                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                    // Manche Provider erlauben keine persistente Permission – dann geht es ggf. trotzdem temporär.
                }

                // Wenn ein Galerie-Bild gewählt wurde, löschen wir die URL-Quelle.
                selectedImageUri = uri;
                imageUrl = "";
                loadImagePreview(); // Vorschau aktualisieren
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Layout-Datei, die diese Activity darstellt.
        setContentView(R.layout.activity_add_gift);

        // Toolbar oben einrichten (Titel + Zurück-Pfeil).
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk hinzufügen");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // PersonId aus dem Intent holen – ohne PersonId macht diese Activity keinen Sinn.
        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null) { finish(); return; }

        // Sicherheit: ohne eingeloggten User abbrechen.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Views verbinden (findViewById = „gib mir das UI-Element aus dem XML“).
        ivGiftImage = findViewById(R.id.ivGiftImage);

        tilName = findViewById(R.id.tilName);
        tilPrice = findViewById(R.id.tilPrice);
        tilLink = findViewById(R.id.tilLink);
        tilNote = findViewById(R.id.tilNote);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);   // Website-Link (bleibt unabhängig vom Bild!)
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        // Status direkt anzeigen (Anfangszustand = geplant).
        renderStatus();

        // Button: Bild aus Galerie/Dateien auswählen (über OpenDocument).
        findViewById(R.id.btnPickImage).setOnClickListener(v ->
                pickImageLauncher.launch(new String[]{"image/*"})
        );

        // Button: Bild per direkter URL eingeben (separat vom Website-Link-Feld).
        findViewById(R.id.btnImageUrl).setOnClickListener(v -> showImageUrlDialog());

        // Status Buttons: nur boolean ändern + Anzeige neu rendern.
        findViewById(R.id.btnMarkBought).setOnClickListener(v -> { bought = true; renderStatus(); });
        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> { bought = false; renderStatus(); });

        // Buttons unten: Abbrechen schließt, Speichern schreibt in Firestore.
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNewGift());
    }

    /**
     * Öffnet MainActivity und „sagt“ ihr per Extra, welchen Fragment-Tab sie anzeigen soll.
     * Flags sorgen dafür, dass keine unnötigen Activity-Duplikate entstehen.
     */
    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    /**
     * Setzt den Status-Text passend zum boolean „bought“.
     * Vorteil: An einer Stelle zentral, damit es überall gleich aussieht.
     */
    private void renderStatus() {
        etStatus.setText(bought ? "Gekauft ✅" : "Geplant");
    }

    /**
     * Dialog, um eine Bild-URL einzugeben (direkter Bildlink, z.B. .jpg/.png).
     * MaterialAlertDialogBuilder = Material-Design Variante von AlertDialog. :contentReference[oaicite:1]{index=1}
     */
    private void showImageUrlDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://...");

        // Prefill: wir füllen NUR die Bild-URL (nicht den Website-Link).
        input.setText(imageUrl != null ? imageUrl.trim() : "");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bild per Link")
                .setMessage("Füge einen direkten Bild-Link ein (z.B. .jpg/.png).")
                .setView(input)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Übernehmen", (d, w) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty()) {
                        Toast.makeText(this, "Kein Bild-Link eingegeben", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Bildquelle = URL, deswegen Galerie-Uri löschen (klarer Zustand: genau 1 Quelle aktiv).
                    imageUrl = url;
                    selectedImageUri = null;
                    loadImagePreview();
                })
                .show();
    }

    /**
     * Lädt die Bild-Vorschau in die ImageView.
     * - Wenn Uri gesetzt: Uri anzeigen
     * - Sonst wenn URL gesetzt: URL anzeigen
     * - Sonst: „ausgegraut“, um zu zeigen: kein Bild ausgewählt
     *
     * fitCenter() zeigt das Bild komplett (keine harte Zuschneidung). :contentReference[oaicite:2]{index=2}
     */
    private void loadImagePreview() {
        if (selectedImageUri != null) {
            Glide.with(this).load(selectedImageUri).fitCenter().into(ivGiftImage);
            ivGiftImage.setAlpha(255);
            return;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl.trim()).fitCenter().into(ivGiftImage);
            ivGiftImage.setAlpha(255);
            return;
        }

        // Kein Bild: leicht transparent, damit es wie „Platzhalter“ wirkt.
        ivGiftImage.setAlpha(75);
    }

    /**
     * Liest Eingaben aus, validiert sie und speichert ein neues Geschenk in Firestore.
     * Firestore add() legt ein neues Dokument mit automatisch generierter ID an. :contentReference[oaicite:3]{index=3}
     */
    private void saveNewGift() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";

        // Minimal-Validation: Name muss vorhanden sein.
        if (name.isEmpty()) {
            tilName.setError("Bitte Geschenkname eingeben");
            return;
        } else {
            tilName.setError(null);
        }

        // Weitere Felder (dürfen leer sein).
        String p = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String websiteLink = etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String n = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        // Preis: optional; wenn gesetzt, muss er lesbar als Zahl sein.
        Double price = null;
        if (!p.isEmpty()) {
            try {
                // Komma erlauben (deutsche Eingabe), intern aber Punkt.
                price = Double.parseDouble(p.replace(",", "."));
            } catch (Exception e) {
                Toast.makeText(this, "Preis ungültig", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Datenpaket für Firestore: key/value Map.
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("personId", personId);
        data.put("title", name);
        data.put("price", price);

        // Website-Link bleibt weiterhin im Feld "link" (bestehende Struktur).
        data.put("link", websiteLink);

        data.put("note", n);
        data.put("bought", bought);

        // Bild getrennt speichern (URL oder Uri-String).
        data.put("imageUrl", (imageUrl != null && !imageUrl.trim().isEmpty()) ? imageUrl.trim() : null);
        data.put("imageUri", (selectedImageUri != null) ? selectedImageUri.toString() : null);

        // Serverseitig nutzbarer Zeitstempel (Firebase Timestamp).
        data.put("createdAt", Timestamp.now());

        // Schreiben in die Unter-Collection gifts(uid, personId).
        FirestorePaths.gifts(uid, personId)
                .add(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Geschenk gespeichert", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Erstellt das Menü (die drei Punkte / Toolbar-Menü).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Reagiert auf Menü-Klicks.
     * android.R.id.home = „Zurück-Pfeil“ in der Toolbar.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        int id = item.getItemId();

        // Falls es ein „Overflow“-Eintrag ist, einfach true zurückgeben.
        if (id == R.id.action_menu) return true;

        // Navigation zu Fragmenten in MainActivity.
        if (id == R.id.menu_home) {
            openMainFragment(FRAG_HOME);
            return true;
        } else if (id == R.id.menu_add_person) {
            openMainFragment(FRAG_ADD_PERSON);
            return true;
        } else if (id == R.id.menu_calendar) {
            openMainFragment(FRAG_CALENDAR);
            return true;
        } else if (id == R.id.menu_settings) {
            openMainFragment(FRAG_SETTINGS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
