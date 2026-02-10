package com.example.geschenkplaner.activity;
import com.example.geschenkplaner.MainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class EditGiftActivity extends AppCompatActivity {

    // Intent-Keys: Damit diese Activity weiß, zu welcher Person + welches Geschenk bearbeitet werden soll.
    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    // Extra + Konstanten: damit MainActivity einen bestimmten Fragment-Tab öffnen kann.
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    // Aktueller Firebase-User (uid) + IDs aus dem Intent.
    private String uid;
    private String personId;
    private String giftId;

    private ImageView ivGiftImage;
    private TextInputEditText etName, etPrice, etLink, etNote, etStatus;

    // Status: true = gekauft, false = geplant.
    private boolean bought = false;

    // Bild getrennt vom Website-Link:
    // Hier wird entweder eine URL oder eine Uri (als String) gespeichert.
    private String imageUrl = "";

    // Foto-Picker: holt Content-Uri (z.B. aus Galerie).
    // GetContent liefert eine Uri zurück, die wir als String speichern.
    // (registerForActivityResult ersetzt das alte onActivityResult.) :contentReference[oaicite:0]{index=0}
    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return; // User hat abgebrochen
                imageUrl = uri.toString();
                showImage(imageUrl); // Vorschau sofort aktualisieren
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_gift);

        // Kleine „Setup-Pipeline“: so ist onCreate übersichtlich und die Schritte sind getrennt.
        setupToolbar();
        readExtrasOrFinish();
        readAuthOrFinish();
        bindViews();
        setupClickListeners();

        // Geschenk-Daten aus Firestore laden und Felder befüllen.
        loadGiftToFields();
    }

    /**
     * Öffnet MainActivity und gibt mit, welcher Fragment-Tab angezeigt werden soll.
     * Flags verhindern doppelte MainActivities (z.B. wenn man öfter navigiert).
     */
    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    /**
     * Toolbar oben einrichten (Titel + Zurück-Pfeil).
     */
    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk bearbeiten");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * Holt personId + giftId aus dem Intent.
     * Wenn etwas fehlt: Meldung + Activity schließen.
     */
    private void readExtrasOrFinish() {
        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (personId == null || giftId == null) {
            Toast.makeText(this, "Fehlende Daten", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Check: User muss eingeloggt sein, sonst beenden.
     * uid wird später für Firestore-Pfade gebraucht.
     */
    private void readAuthOrFinish() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    /**
     * Verknüpft die UI-Elemente aus dem Layout mit Variablen.
     * Zusätzlich: Status-Feld wird read-only gemacht (Status ändert man über Buttons).
     */
    private void bindViews() {
        ivGiftImage = findViewById(R.id.ivGiftImage);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);   // Website-Link bleibt in "link"
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        makeReadOnly(etStatus);

        // Alpha 1f = komplett sichtbar (Android float von 0.0 bis 1.0).
        if (ivGiftImage != null) ivGiftImage.setAlpha(1f);
    }

    /**
     * Registriert alle Button-Klicks an einer Stelle.
     * Vorteil: man findet später schnell „wo passiert was“.
     */
    private void setupClickListeners() {
        // Bild aus Galerie wählen
        findViewById(R.id.btnPickImage).setOnClickListener(v -> photoPicker.launch("image/*"));

        // Bild-Link per Dialog eingeben (getrennt vom Website-Link-Feld)
        findViewById(R.id.btnImageUrl).setOnClickListener(v -> askForImageUrl());

        // Status ändern (boolean + UI-Text neu setzen)
        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> {
            bought = false;
            renderStatus();
        });

        findViewById(R.id.btnMarkBought).setOnClickListener(v -> {
            bought = true;
            renderStatus();
        });

        // Abbrechen / Speichern
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    /**
     * Macht ein Textfeld „nur Anzeige“:
     * - nicht fokussierbar, kein Cursor, kein Tippen möglich.
     * TYPE_NULL + KeyListener null = verhindert Eingabe komplett.
     */
    private void makeReadOnly(TextInputEditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(false);
        et.setCursorVisible(false);
        et.setLongClickable(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    /**
     * Zeigt einen Dialog, in den man einen direkten Bild-Link einfügen kann.
     * MaterialAlertDialogBuilder = Material-Variante von AlertDialog.Builder. :contentReference[oaicite:1]{index=1}
     */
    private void askForImageUrl() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://...");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        // Prefill mit aktueller Bild-URL (nicht Website-Link)
        input.setText(imageUrl != null ? imageUrl.trim() : "");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bild per Link")
                .setView(input)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Übernehmen", (d, w) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty()) {
                        Toast.makeText(this, "Bild-Link ist leer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    imageUrl = url;
                    showImage(imageUrl);
                })
                .show();
    }

    /**
     * Lädt das Geschenk aus Firestore und befüllt alle Felder.
     * doc.getString/getDouble/... holt Werte nach Feldnamen.
     */
    private void loadGiftToFields() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Geschenk nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Werte aus Firestore lesen (null möglich -> defensiv setzen).
                    String title = doc.getString("title");
                    Double price = doc.getDouble("price");
                    String link = doc.getString("link"); // Website-Link bleibt "link"
                    String note = doc.getString("note");
                    Boolean b = doc.getBoolean("bought");
                    String img = doc.getString("imageUrl");

                    bought = b != null && b;
                    imageUrl = img != null ? img : "";

                    if (etName != null) etName.setText(title != null ? title : "");
                    if (etPrice != null) etPrice.setText(price != null ? String.valueOf(price) : "");
                    if (etLink != null) etLink.setText(link != null ? link : "");
                    if (etNote != null) etNote.setText(note != null ? note : "");

                    renderStatus();
                    showImage(imageUrl);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Aktualisiert nur die Status-Anzeige im UI.
     */
    private void renderStatus() {
        if (etStatus != null) etStatus.setText(bought ? "Gekauft ✅" : "Geplant");
    }

    /**
     * Zeigt ein Bild in der ImageView:
     * - wenn leer: Platzhalter-Icon + halb transparent
     * - sonst: Glide lädt Uri/URL in die ImageView
     *
     * Glide Grundnutzung: Glide.with(...).load(...).into(imageView). :contentReference[oaicite:2]{index=2}
     */
    private void showImage(String uriOrUrl) {
        if (ivGiftImage == null) return;

        if (uriOrUrl == null || uriOrUrl.trim().isEmpty()) {
            ivGiftImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivGiftImage.setImageResource(android.R.drawable.ic_menu_gallery);
            ivGiftImage.setAlpha(0.75f);
            return;
        }

        ivGiftImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ivGiftImage.setAlpha(1f);

        Glide.with(this).load(uriOrUrl).into(ivGiftImage);
    }

    /**
     * Liest die Eingaben aus und schreibt die Änderungen nach Firestore.
     * update(...) aktualisiert nur die übergebenen Felder am Dokument. :contentReference[oaicite:3]{index=3}
     */
    private void save() {
        String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
        String p = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String websiteLink = etLink != null && etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String n = etNote != null && etNote.getText() != null ? etNote.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name darf nicht leer sein", Toast.LENGTH_SHORT).show();
            return;
        }

        // Preis optional: falls gesetzt, muss er in eine Zahl umwandelbar sein.
        Double pVal = null;
        if (!p.isEmpty()) {
            try {
                pVal = Double.parseDouble(p.replace(",", "."));
            } catch (Exception ignored) {
                Toast.makeText(this, "Preis ungültig", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Datenmap enthält nur die Felder, die wir aktualisieren wollen.
        Map<String, Object> data = new HashMap<>();
        data.put("title", name);
        data.put("price", pVal);

        // Website-Link bleibt getrennt
        data.put("link", websiteLink);

        data.put("note", n);
        data.put("bought", bought);

        // Bild getrennt
        data.put("imageUrl", imageUrl);

        FirestorePaths.gift(uid, personId, giftId)
                .update(data)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Gespeichert ✅", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK, new Intent());
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Baut das Menü oben rechts (inflate = XML-Menü wird „geladen“).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Reagiert auf Menü-Klicks:
     * - android.R.id.home = Toolbar-Zurückpfeil
     * - Rest: Navigation zu den MainActivity-Fragmenten
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        int id = item.getItemId();

        if (id == R.id.action_menu) return true;

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
