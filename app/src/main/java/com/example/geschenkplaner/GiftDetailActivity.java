package com.example.geschenkplaner;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private FirebaseFirestore db;
    private String uid;
    private String personId;
    private String giftId;

    private TextView tvTitle, tvPrice, tvLink, tvNote, tvStatus;

    // Aktuelle Werte
    private String currentTitle = "—";
    private Double currentPrice = null;
    private String currentLink = "";
    private String currentNote = "";
    private boolean currentBought = false;

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
        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvLink  = findViewById(R.id.tvLink);
        tvNote  = findViewById(R.id.tvNote);
        tvStatus= findViewById(R.id.tvStatus);

        // Aktionen
        findViewById(R.id.btnMarkBought).setOnClickListener(v -> {
            currentBought = true;
            renderAll();
        });

        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> {
            currentBought = false;
            renderAll();
        });

        findViewById(R.id.btnEdit).setOnClickListener(v -> openEditDialog());

        findViewById(R.id.btnDeleteGift).setOnClickListener(v -> deleteGift());

        // Bottom Buttons
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAll());

        // Upload vorerst
        findViewById(R.id.btnUploadImage).setOnClickListener(v ->
                Toast.makeText(this, "Upload kommt als nächstes 🙂", Toast.LENGTH_SHORT).show()
        );

        loadGift();
    }

    private void loadGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    String title = doc.getString("title");
                    Double price = doc.getDouble("price");
                    String link  = doc.getString("link");
                    String note  = doc.getString("note");
                    Boolean bought = doc.getBoolean("bought");

                    currentTitle = title != null ? title : "—";
                    currentPrice = price;
                    currentLink = (link != null) ? link : "";
                    currentNote = (note != null) ? note : "";
                    currentBought = bought != null && bought;

                    renderAll();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderAll() {
        tvTitle.setText(currentTitle);

        if (currentPrice != null) {
            tvPrice.setText("Preis: " + String.format("€ %.2f", currentPrice));
        } else {
            tvPrice.setText("Preis: —");
        }

        tvLink.setText("Link: " + (!currentLink.trim().isEmpty() ? currentLink : "—"));
        tvNote.setText("Notiz: " + (!currentNote.trim().isEmpty() ? currentNote : "—"));
        tvStatus.setText("Status: " + (currentBought ? "Gekauft ✅" : "Geplant"));
    }

    private void openEditDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        box.setPadding(pad, pad, pad, pad);

        EditText etPrice = new EditText(this);
        etPrice.setHint("Preis (z.B. 14.99)");
        etPrice.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etPrice.setText(currentPrice != null ? String.valueOf(currentPrice) : "");

        EditText etLink = new EditText(this);
        etLink.setHint("Link");
        etLink.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        etLink.setText(currentLink);

        EditText etNote = new EditText(this);
        etNote.setHint("Notiz");
        etNote.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        etNote.setMinLines(2);
        etNote.setText(currentNote);

        box.addView(etPrice);
        box.addView(etLink);
        box.addView(etNote);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Geschenk bearbeiten")
                .setView(box)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Übernehmen", (d, w) -> {
                    String p = etPrice.getText().toString().trim();
                    String link = etLink.getText().toString().trim();
                    String note = etNote.getText().toString().trim();

                    Double priceVal = null;
                    if (!p.isEmpty()) {
                        try {
                            priceVal = Double.parseDouble(p.replace(",", "."));
                        } catch (Exception ignored) {
                            Toast.makeText(this, "Preis ist ungültig", Toast.LENGTH_SHORT).show();
                        }
                    }

                    currentPrice = priceVal;
                    currentLink = link;
                    currentNote = note;
                    renderAll();
                })
                .show();
    }

    private void saveAll() {
        Map<String, Object> data = new HashMap<>();
        data.put("bought", currentBought);
        data.put("price", currentPrice);
        data.put("link", currentLink);
        data.put("note", currentNote);

        FirestorePaths.gift(uid, personId, giftId)
                .update(data)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Gespeichert ✅", Toast.LENGTH_SHORT).show())
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
