package com.example.geschenkplaner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_GIFT_ID = "giftId";

    private FirebaseFirestore db;
    private String uid;
    private String giftId;

    private TextView tvTitle, tvPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gift_detail);

        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (giftId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        Button btn = findViewById(R.id.btnMarkBought);

        loadGift();

        btn.setOnClickListener(v -> markBought());
    }

    private void loadGift() {
        db.collection("gifts").document(giftId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) { finish(); return; }
                    String owner = doc.getString("uid");
                    if (owner != null && !owner.equals(uid)) { finish(); return; }

                    tvTitle.setText(doc.getString("title"));
                    Double price = doc.getDouble("price");
                    tvPrice.setText("€ " + (price != null ? price : "—"));
                });
    }

    private void markBought() {
        db.collection("gifts").document(giftId)
                .update("bought", true)
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Als gekauft gespeichert ✅", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
