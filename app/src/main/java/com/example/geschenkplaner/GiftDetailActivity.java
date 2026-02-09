package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private static final int REQ_EDIT_GIFT = 1001;

    private String uid;
    private String personId;
    private String giftId;

    private TextView tvTitle, tvPrice, tvLink, tvNote, tvStatus;

    private String title = "—";
    private Double price = null;
    private String link = "";
    private String note = "";
    private boolean bought = false;

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

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent i = new Intent(this, EditGiftActivity.class);
            i.putExtra(EditGiftActivity.EXTRA_PERSON_ID, personId);
            i.putExtra(EditGiftActivity.EXTRA_GIFT_ID, giftId);
            startActivityForResult(i, REQ_EDIT_GIFT);
        });

        findViewById(R.id.btnDeleteGift).setOnClickListener(v -> deleteGift());

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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_EDIT_GIFT && resultCode == RESULT_OK) {
            loadGift(); // nach Speichern neu laden
        }
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
