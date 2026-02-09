package com.example.geschenkplaner.activity;

import com.example.geschenkplaner.MainActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
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

    private TextInputLayout tilName, tilPrice, tilLink, tilNote;

    private TextInputEditText etName;
    private TextInputEditText etPrice;
    private TextInputEditText etLink;  // <-- Website-Link (User)
    private TextInputEditText etNote;
    private TextInputEditText etStatus;

    private ImageView ivGiftImage;

    private boolean bought = false;

    // Bild: entweder Galerie-URI ODER Direkt-URL
    private Uri selectedImageUri = null;
    private String imageUrl = "";

    private final ActivityResultLauncher<String[]> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;

                try {
                    getContentResolver().takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    );
                } catch (Exception ignored) {
                    // Manche Provider erlauben keine persistente Permission – ist dann trotzdem oft lesbar.
                }

                selectedImageUri = uri;
                imageUrl = "";
                loadImagePreview();
            });

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

        ivGiftImage = findViewById(R.id.ivGiftImage);

        tilName = findViewById(R.id.tilName);
        tilPrice = findViewById(R.id.tilPrice);
        tilLink = findViewById(R.id.tilLink);
        tilNote = findViewById(R.id.tilNote);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);   // Website-Link (NICHT Bild!)
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        renderStatus();

        // Bild: Galerie
        findViewById(R.id.btnPickImage).setOnClickListener(v ->
                pickImageLauncher.launch(new String[]{"image/*"})
        );

        // Bild: Direkt-URL (separat vom Website-Link!)
        findViewById(R.id.btnImageUrl).setOnClickListener(v -> showImageUrlDialog());

        // Status Buttons
        findViewById(R.id.btnMarkBought).setOnClickListener(v -> { bought = true; renderStatus(); });
        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> { bought = false; renderStatus(); });

        // Buttons unten
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveNewGift());
    }

    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private void renderStatus() {
        etStatus.setText(bought ? "Gekauft ✅" : "Geplant");
    }

    private void showImageUrlDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setHint("https://...");

        // WICHTIG: Prefill aus imageUrl (nicht aus etLink!)
        input.setText(imageUrl != null ? imageUrl.trim() : "");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bild per Link")
                .setMessage("Füge einen direkten Bild-Link ein (z.B. .jpg/.png).")
                .setView(input)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Übernehmen", (d, w) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty()) {
                        Toast.makeText(this, "Kein Bild-Link eingegeben", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Bildquelle = URL, Website-Link bleibt unberührt
                    imageUrl = url;
                    selectedImageUri = null;
                    loadImagePreview();
                })
                .show();
    }

    private void loadImagePreview() {
        if (selectedImageUri != null) {
            Glide.with(this).load(selectedImageUri).fitCenter().into(ivGiftImage);
            ivGiftImage.setAlpha(255);
            return;
        }

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(this).load(imageUrl.trim()).fitCenter().into(ivGiftImage);
            ivGiftImage.setAlpha(255);
            return;
        }

        ivGiftImage.setAlpha(75);
    }

    private void saveNewGift() {
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";

        if (name.isEmpty()) {
            tilName.setError("Bitte Geschenkname eingeben");
            return;
        } else {
            tilName.setError(null);
        }

        String p = etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String websiteLink = etLink.getText() != null ? etLink.getText().toString().trim() : "";
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
        data.put("title", name);
        data.put("price", price);

        // Website-Link bleibt weiterhin im Feld "link" (deine bestehende Struktur)
        data.put("link", websiteLink);

        data.put("note", n);
        data.put("bought", bought);

        // Bild getrennt
        data.put("imageUrl", (imageUrl != null && !imageUrl.trim().isEmpty()) ? imageUrl.trim() : null);
        data.put("imageUri", (selectedImageUri != null) ? selectedImageUri.toString() : null);

        data.put("createdAt", Timestamp.now());

        FirestorePaths.gifts(uid, personId)
                .add(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(this, "Geschenk gespeichert", Toast.LENGTH_SHORT).show();
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
