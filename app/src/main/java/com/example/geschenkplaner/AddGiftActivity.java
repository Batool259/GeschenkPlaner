package com.example.geschenkplaner;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class AddGiftActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";

    private String uid;
    private String personId;

    private TextView tvTitle, tvPrice, tvLink, tvNote, tvStatus;
    private View bottomBar;

    private TextInputLayout tilPrice, tilLink, tilNote;
    private TextInputEditText etPrice, etLink, etNote;

    private boolean bought = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_gift);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk hinzufügen");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Nur personId nötig!
        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Bind Views (dein XML hat diese IDs)
        tvTitle  = findViewById(R.id.tvTitle);
        tvPrice  = findViewById(R.id.tvPrice);
        tvLink   = findViewById(R.id.tvLink);
        tvNote   = findViewById(R.id.tvNote);
        tvStatus = findViewById(R.id.tvStatus);

        bottomBar = findViewById(R.id.bottomBar);

        tilPrice = findViewById(R.id.tilPrice);
        tilLink  = findViewById(R.id.tilLink);
        tilNote  = findViewById(R.id.tilNote);

        etPrice = findViewById(R.id.etPrice);
        etLink  = findViewById(R.id.etLink);
        etNote  = findViewById(R.id.etNote);

        // Titel Platzhalter (weil du kein Feld für Name hast)
        // Wenn du ein Namensfeld willst: sag, dann bauen wir etTitle ein.
        tvTitle.setText("Neues Geschenk");

        // Für Add: direkt Edit-Mode an
        enterEditMode();

        findViewById(R.id.btnUploadImage).setOnClickListener(v ->
                Toast.makeText(this, "Upload kommt als nächstes 🙂", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnMarkBought).setOnClickListener(v -> {
            bought = true;
            renderStatus();
        });

        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> {
            bought = false;
            renderStatus();
        });

        // Bei Add brauchst du KEIN Bearbeiten/Löschen:
        findViewById(R.id.btnEdit).setVisibility(View.GONE);
        findViewById(R.id.btnDeleteGift).setVisibility(View.GONE);

        // Bottom Buttons
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNewGift());

        renderStatus();
    }

    private void renderStatus() {
        tvStatus.setText("Status: " + (bought ? "Gekauft ✅" : "Geplant"));
    }

    private void enterEditMode() {
        // Anzeige aus, Eingabe an
        tvPrice.setVisibility(View.GONE);
        tvLink.setVisibility(View.GONE);
        tvNote.setVisibility(View.GONE);

        tilPrice.setVisibility(View.VISIBLE);
        tilLink.setVisibility(View.VISIBLE);
        tilNote.setVisibility(View.VISIBLE);

        // BottomBar bei Add immer sichtbar
        bottomBar.setVisibility(View.VISIBLE);
    }

    private void saveNewGift() {
        String p = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String l = etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String n = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        Double price = null;
        if (!p.isEmpty()) {
            try {
                price = Double.parseDouble(p.replace(",", "."));
            } catch (Exception e) {
                Toast.makeText(this, "Preis ungültig", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // WICHTIG: Du hast in deinem Add-Layout kein Name-Feld.
        // Wir speichern erstmal einen Default-Titel.
        String title = "Neues Geschenk";

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("personId", personId);

        data.put("title", title);
        data.put("price", price);
        data.put("link", l);
        data.put("note", n);
        data.put("bought", bought);
        data.put("createdAt", Timestamp.now());

        FirestorePaths.gifts(uid, personId)
                .add(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Geschenk gespeichert ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
