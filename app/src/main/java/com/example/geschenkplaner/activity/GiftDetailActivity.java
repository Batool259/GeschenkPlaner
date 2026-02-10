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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class GiftDetailActivity extends AppCompatActivity {

    // Intent-Keys: diese Activity erwartet Person + Geschenk-ID.
    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    // Request-Code für startActivityForResult -> EditGiftActivity.
    private static final int REQ_EDIT_GIFT = 1001;

    // Extra + Konstanten: Navigation zurück zur MainActivity (welcher Tab soll geöffnet werden).
    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    // uid = eingeloggter Firebase-User; personId/giftId = Kontext für Firestore-Pfade.
    private String uid;
    private String personId;
    private String giftId;

    // UI-Elemente (Detailansicht).
    private ImageView ivGiftImage;
    private EditText etName, etPrice, etLink, etNote, etStatus;

    // „Model“-Daten, die aus Firestore geladen und dann in render() angezeigt werden.
    private String title = "";
    private Double price = null;
    private String link = "";     // Website-Link (separat vom Bild)
    private String note = "";
    private boolean bought = false;
    private String imageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gift_detail);

        // Toolbar oben einrichten (inkl. Zurückpfeil).
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // IDs aus dem Intent holen (ohne die kann man das Geschenk nicht laden).
        Intent intent = getIntent();
        personId = intent.getStringExtra(EXTRA_PERSON_ID);
        giftId = intent.getStringExtra(EXTRA_GIFT_ID);

        if (personId == null || giftId == null) {
            Toast.makeText(this, "Fehlende Daten", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Sicherheit: ohne Login keine Details anzeigen.
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Views verbinden.
        ivGiftImage = findViewById(R.id.ivGiftImage);
        if (ivGiftImage != null) ivGiftImage.setAlpha(1f);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        // Detailansicht: Felder sollen hier nicht bearbeitbar sein.
        makeReadOnly(etName);
        makeReadOnly(etPrice);

        // Link-Feld: nicht komplett deaktivieren, weil es anklickbar sein soll.
        makeReadOnlyButClickable(etLink);
        etLink.setOnClickListener(v -> openWebsiteLink(link));

        makeReadOnly(etNote);
        makeReadOnly(etStatus);

        // „Bearbeiten“ öffnet EditGiftActivity; Ergebnis kommt über onActivityResult zurück.
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent i = new Intent(this, EditGiftActivity.class);
            i.putExtra(EditGiftActivity.EXTRA_PERSON_ID, personId);
            i.putExtra(EditGiftActivity.EXTRA_GIFT_ID, giftId);
            startActivityForResult(i, REQ_EDIT_GIFT);
        });

        // Löschen-Button entfernt das Dokument in Firestore.
        findViewById(R.id.btnDeleteGift).setOnClickListener(v -> deleteGift());

        // Geschenk laden (Firestore) und danach rendern.
        loadGift();
    }

    /**
     * Macht ein EditText komplett „nur Anzeige“:
     * - nicht fokussierbar, kein Cursor, keine Tastatur-Eingabe.
     */
    private void makeReadOnly(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    /**
     * Read-only, aber klickbar (für Links):
     * - keine Eingabe möglich
     * - OnClick funktioniert trotzdem
     */
    private void makeReadOnlyButClickable(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(true);
        et.setLongClickable(true);
        et.setCursorVisible(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    /**
     * Öffnet MainActivity und gibt mit, welcher Tab/Fragment angezeigt werden soll.
     * Flags vermeiden doppelte MainActivities.
     */
    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    /**
     * Lädt das Geschenk-Dokument aus Firestore (per uid/personId/giftId) und speichert die Werte
     * in die Felder title/price/link/note/bought/imageUrl, danach render().
     */
    private void loadGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Geschenk nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    // Defensive Reads: Firestore kann null liefern -> wir setzen dann leere Defaults.
                    title = doc.getString("title") != null ? doc.getString("title") : "";
                    price = doc.getDouble("price");
                    link = doc.getString("link") != null ? doc.getString("link") : "";
                    note = doc.getString("note") != null ? doc.getString("note") : "";
                    Boolean b = doc.getBoolean("bought");
                    bought = b != null && b;
                    imageUrl = doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "";

                    render();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Schreibt die geladenen Daten in die UI (Textfelder) und zeigt das Bild.
     * Locale.GERMANY sorgt für deutsches Zahlenformat (z.B. 12,34 wird korrekt formatiert).
     */
    private void render() {
        if (etName != null) etName.setText(title);

        if (etPrice != null) {
            if (price != null) etPrice.setText(String.format(Locale.GERMANY, "%.2f", price));
            else etPrice.setText("");
        }

        if (etLink != null) etLink.setText(link);
        if (etNote != null) etNote.setText(note);
        if (etStatus != null) etStatus.setText(bought ? "Gekauft ✅" : "Geplant");

        showImage(imageUrl);
    }

    /**
     * Macht aus „example.com“ -> „https://example.com“
     * Damit ACTION_VIEW nicht an fehlendem Schema scheitert.
     */
    private String normalizeUrl(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://" + s;
    }

    /**
     * Öffnet den gespeicherten Website-Link im Browser (Intent.ACTION_VIEW).
     * Falls nichts gespeichert oder Fehler: Toast anzeigen.
     */
    private void openWebsiteLink(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url.isEmpty()) {
            Toast.makeText(this, "Kein Website-Link gespeichert", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Link kann nicht geöffnet werden", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Bildanzeige:
     * - leer: Platzhalter-Icon + leicht transparent
     * - sonst: Glide lädt Uri/URL in die ImageView.
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

        Glide.with(this)
                .load(uriOrUrl)
                .into(ivGiftImage);
    }

    /**
     * Löscht das Geschenk-Dokument aus Firestore.
     * Bei Erfolg: Toast + Activity schließen.
     */
    private void deleteGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Geschenk gelöscht", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    /**
     * Kommt zurück, wenn EditGiftActivity fertig ist.
     * Wenn RESULT_OK: neu laden, damit Änderungen sofort angezeigt werden.
     *
     * Hinweis: startActivityForResult/onActivityResult ist inzwischen „legacy“,
     * aber solange ihr es so nutzt, ist das Verhalten korrekt. ([developer.android.com](https://developer.android.com/training/basics/intents/result?utm_source=chatgpt.com))
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT_GIFT && resultCode == RESULT_OK) {
            loadGift();
        }
    }

    /**
     * Baut das Menü (Toolbar-Menü wird aus XML geladen).
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * Menü-Klicks:
     * - android.R.id.home = Zurückpfeil
     * - Rest: Navigation in MainActivity (Tabs/Fragmente)
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
