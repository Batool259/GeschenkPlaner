package com.example.geschenkplaner.activity;
import com.example.geschenkplaner.MainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.geschenkplaner.R;
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

    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    private String uid;
    private String personId;

    private TextInputLayout tilTitle;
    private TextInputEditText etTitle;

    private TextView tvPrice, tvLink, tvNote, tvStatus;

    private TextInputLayout tilPrice, tilLink, tilNote;
    private TextInputEditText etPrice, etLink, etNote;

    private View bottomBar;
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

        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null) { finish(); return; }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) { finish(); return; }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        tilTitle = findViewById(R.id.tilTitle);
        etTitle  = findViewById(R.id.etTitle);

        tvPrice  = findViewById(R.id.tvPrice);
        tvLink   = findViewById(R.id.tvLink);
        tvNote   = findViewById(R.id.tvNote);
        tvStatus = findViewById(R.id.tvStatus);

        tilPrice = findViewById(R.id.tilPrice);
        tilLink  = findViewById(R.id.tilLink);
        tilNote  = findViewById(R.id.tilNote);

        etPrice = findViewById(R.id.etPrice);
        etLink  = findViewById(R.id.etLink);
        etNote  = findViewById(R.id.etNote);

        bottomBar = findViewById(R.id.bottomBar);

        findViewById(R.id.btnUploadImage).setOnClickListener(v ->
                Toast.makeText(this, "Bild-Upload kommt später 🙂", Toast.LENGTH_SHORT).show()
        );

        findViewById(R.id.btnMarkBought).setOnClickListener(v -> { bought = true; renderStatus(); });
        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> { bought = false; renderStatus(); });

        findViewById(R.id.btnEdit).setVisibility(View.GONE);
        findViewById(R.id.btnDeleteGift).setVisibility(View.GONE);

        bottomBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNewGift());

        renderStatus();
    }

    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private void renderStatus() {
        tvStatus.setText("Status: " + (bought ? "Gekauft ✅" : "Geplant"));
    }

    private void saveNewGift() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";

        if (title.isEmpty()) {
            tilTitle.setError("Bitte Geschenkname eingeben");
            return;
        } else {
            tilTitle.setError(null);
        }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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
