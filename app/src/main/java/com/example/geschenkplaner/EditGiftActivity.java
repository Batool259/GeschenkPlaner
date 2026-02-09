package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class EditGiftActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private String uid;
    private String personId;
    private String giftId;

    private TextInputEditText etPrice, etLink, etNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_gift);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk bearbeiten");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (personId == null || giftId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etPrice = findViewById(R.id.etPrice);
        etLink  = findViewById(R.id.etLink);
        etNote  = findViewById(R.id.etNote);

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());

        loadGiftToFields();
    }

    private void loadGiftToFields() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }

                    Double price = doc.getDouble("price");
                    String link = doc.getString("link");
                    String note = doc.getString("note");

                    etPrice.setText(price != null ? String.valueOf(price) : "");
                    etLink.setText(link != null ? link : "");
                    etNote.setText(note != null ? note : "");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void save() {
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

        Map<String, Object> data = new HashMap<>();
        data.put("price", pVal);
        data.put("link", l);
        data.put("note", n);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
