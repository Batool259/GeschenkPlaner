package com.example.geschenkplaner;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddGiftActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";

    private boolean bought = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_gift);

        String personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        EditText etTitle = findViewById(R.id.etTitle);
        EditText etPrice = findViewById(R.id.etPrice);
        Button btnPlanned = findViewById(R.id.btnPlanned);
        Button btnBought = findViewById(R.id.btnBought);
        Button btnSave = findViewById(R.id.btnSave);

        btnPlanned.setOnClickListener(v -> bought = false);
        btnBought.setOnClickListener(v -> bought = true);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Titel fehlt", Toast.LENGTH_SHORT).show();
                return;
            }

            Double price = null;
            String priceStr = etPrice.getText().toString().trim();
            if (!TextUtils.isEmpty(priceStr)) {
                try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
            }

            Map<String, Object> data = new HashMap<>();
            data.put("uid", uid);
            data.put("personId", personId);
            data.put("title", title);
            data.put("price", price);
            data.put("bought", bought);
            data.put("createdAt", Timestamp.now());

            db.collection("gifts").add(data)
                    .addOnSuccessListener(r -> finish())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }
}
