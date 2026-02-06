package com.example.geschenkplaner;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
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

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk hinzufügen");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // db wird hier nicht zwingend gebraucht, aber wir lassen es drin falls du später noch mehr machst
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        EditText etName = findViewById(R.id.etGiftName);
        EditText etPrice = findViewById(R.id.etPrice);
        EditText etLink = findViewById(R.id.etLink);

        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleStatus);
        MaterialButton btnSave = findViewById(R.id.btnSaveGift);

        // default: geplant
        bought = false;

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnBought) bought = true;
            if (checkedId == R.id.btnPlanned) bought = false;
        });

        btnSave.setOnClickListener(v -> {
            String title = etName.getText().toString().trim();
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(this, "Name fehlt", Toast.LENGTH_SHORT).show();
                return;
            }

            Double price = null;
            String priceStr = etPrice.getText().toString().trim();
            if (!TextUtils.isEmpty(priceStr)) {
                try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {}
            }

            String link = etLink.getText().toString().trim();

            Map<String, Object> data = new HashMap<>();
            // optional, aber praktisch (Debug / spätere CollectionGroup-Auswertung)
            data.put("uid", uid);
            data.put("personId", personId);

            data.put("title", title);
            data.put("price", price);
            data.put("link", link);
            data.put("bought", bought);
            data.put("createdAt", Timestamp.now());

            FirestorePaths.gifts(uid, personId)
                    .add(data)
                    .addOnSuccessListener(r -> finish())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
        });
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
