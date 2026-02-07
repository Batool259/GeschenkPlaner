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
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private String uid;
    private String personId;
    private String giftId;

    // Anzeige
    private TextView tvTitle, tvPrice, tvLink, tvNote, tvStatus;

    // Edit UI
    private View bottomBar;
    private TextInputLayout tilPrice, tilLink, tilNote;
    private TextInputEditText etPrice, etLink, etNote;

    // Werte
    private String title = "—";
    private Double price = null;
    private String link = "";
    private String note = "";
    private boolean bought = false;

    // gespeicherter Stand für Cancel
    private Double savedPrice = null;
    private String savedLink = "";
    private String savedNote = "";
    private boolean savedBought = false;

    private boolean editMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gift_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenkdetails");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (personId == null || giftId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvLink  = findViewById(R.id.tvLink);
        tvNote  = findViewById(R.id.tvNote);
        tvStatus= findViewById(R.id.tvStatus);

        bottomBar = findViewById(R.id.bottomBar);

        tilPrice = findViewById(R.id.tilPrice);
        tilLink  = findViewById(R.id.tilLink);
        tilNote  = findViewById(R.id.tilNote);

        etPrice = findViewById(R.id.etPrice);
        etLink  = findViewById(R.id.etLink);
        etNote  = findViewById(R.id.etNote);

        findViewById(R.id.btnUploadImage).setOnClickListener(v ->
                Toast.makeText(this, "Upload kommt als nächstes 🙂", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnMarkBought).setOnClickListener(v -> {
            bought = true;
            render();
            if (editMode) { /* bleibt */ }
        });

        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> {
            bought = false;
            render();
            if (editMode) { /* bleibt */ }
        });

        findViewById(R.id.btnEdit).setOnClickListener(v -> enterEditMode());

        findViewById(R.id.btnDeleteGift).setOnClickListener(v -> deleteGift());

        findViewById(R.id.btnCancel).setOnClickListener(v -> cancelEdit());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveEdit());

        loadGift();
    }

    private void render() {
        tvTitle.setText(title);

        if (price != null) tvPrice.setText("Preis: " + String.format("€ %.2f", price));
        else tvPrice.setText("Preis: —");

        tvLink.setText("Link: " + (!link.trim().isEmpty() ? link : "—"));
        tvNote.setText("Notiz: " + (!note.trim().isEmpty() ? note : "—"));
        tvStatus.setText("Status: " + (bought ? "Gekauft ✅" : "Geplant"));
    }

    private void enterEditMode() {
        editMode = true;

        // Werte in Felder
        etPrice.setText(price != null ? String.valueOf(price) : "");
        etLink.setText(link);
        etNote.setText(note);

        // Anzeige aus, Eingabe an
        tvPrice.setVisibility(View.GONE);
        tvLink.setVisibility(View.GONE);
        tvNote.setVisibility(View.GONE);

        tilPrice.setVisibility(View.VISIBLE);
        tilLink.setVisibility(View.VISIBLE);
        tilNote.setVisibility(View.VISIBLE);

        bottomBar.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Bearbeiten aktiv", Toast.LENGTH_SHORT).show();
    }

    private void cancelEdit() {
        // zurück auf gespeicherte Werte
        price = savedPrice;
        link = savedLink;
        note = savedNote;
        bought = savedBought;

        exitEditMode();
        render();
        Toast.makeText(this, "Änderungen verworfen", Toast.LENGTH_SHORT).show();
    }

    private void exitEditMode() {
        editMode = false;

        // Eingabe aus, Anzeige an
        tilPrice.setVisibility(View.GONE);
        tilLink.setVisibility(View.GONE);
        tilNote.setVisibility(View.GONE);

        tvPrice.setVisibility(View.VISIBLE);
        tvLink.setVisibility(View.VISIBLE);
        tvNote.setVisibility(View.VISIBLE);

        bottomBar.setVisibility(View.GONE);
    }

    private void saveEdit() {
        // Werte aus EditTexts lesen
        String p = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String l = etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String n = etNote.getText() != null ? etNote.getText().toString().trim() : "";

        Double pVal = null;
        if (!p.isEmpty()) {
            try {
                pVal = Double.parseDouble(p.replace(",", "."));
            } catch (Exception ignored) {
                Toast.makeText(this, "Preis ungültig", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        price = pVal;
        link = l;
        note = n;

        Map<String, Object> data = new HashMap<>();
        data.put("price", price);
        data.put("link", link);
        data.put("note", note);
        data.put("bought", bought);

        FirestorePaths.gift(uid, personId, giftId)
                .update(data)
                .addOnSuccessListener(v -> {
                    // saved = current
                    savedPrice = price;
                    savedLink = link;
                    savedNote = note;
                    savedBought = bought;

                    exitEditMode();
                    render();
                    Toast.makeText(this, "Gespeichert ✅", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void loadGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    title = doc.getString("title") != null ? doc.getString("title") : "—";
                    price = doc.getDouble("price");
                    link = doc.getString("link") != null ? doc.getString("link") : "";
                    note = doc.getString("note") != null ? doc.getString("note") : "";
                    Boolean b = doc.getBoolean("bought");
                    bought = b != null && b;

                    // saved für cancel
                    savedPrice = price;
                    savedLink = link;
                    savedNote = note;
                    savedBought = bought;

                    render();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void deleteGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Gelöscht ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
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
