package com.example.geschenkplaner;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GIFT_ID = "giftId";

    private FirebaseFirestore db;
    private String uid;
    private String giftId;

    private TextView tvTitle, tvPrice, tvLink, tvStatus;

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

        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (giftId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvLink = findViewById(R.id.tvLink);
        tvStatus = findViewById(R.id.tvStatus);

        Button btnBought = findViewById(R.id.btnMarkBought);
        Button btnDelete = findViewById(R.id.btnDeleteGift);

        loadGift();

        btnBought.setOnClickListener(v -> markBought());
        btnDelete.setOnClickListener(v -> deleteGift());
    }

    private void loadGift() {
        db.collection("gifts").document(giftId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }
                    String owner = doc.getString("uid");
                    if (owner != null && !owner.equals(uid)) { finish(); return; }

                    String title = doc.getString("title");
                    Double price = doc.getDouble("price");
                    String link = doc.getString("link");
                    Boolean bought = doc.getBoolean("bought");

                    tvTitle.setText(title != null ? title : "—");
                    tvPrice.setText("Preis: € " + (price != null ? price : "—"));
                    tvLink.setText("Link: " + (link != null && !link.trim().isEmpty() ? link : "—"));
                    tvStatus.setText("Status: " + ((bought != null && bought) ? "Gekauft ✅" : "Geplant"));
                });
    }

    private void markBought() {
        db.collection("gifts").document(giftId)
                .update("bought", true)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Als gekauft gespeichert ✅", Toast.LENGTH_SHORT).show();
                    loadGift();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void deleteGift() {
        db.collection("gifts").document(giftId)
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
