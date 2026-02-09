package com.example.geschenkplaner.activity;
import com.example.geschenkplaner.MainActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
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
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class EditGiftActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    private String uid;
    private String personId;
    private String giftId;

    private ImageView ivGiftImage;
    private TextInputEditText etName, etPrice, etLink, etNote, etStatus;

    private boolean bought = false;

    // Bild getrennt vom Website-Link
    private String imageUrl = "";

    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;
                imageUrl = uri.toString();
                showImage(imageUrl);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_gift);

        setupToolbar();
        readExtrasOrFinish();
        readAuthOrFinish();
        bindViews();
        setupClickListeners();

        loadGiftToFields();
    }

    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Geschenk bearbeiten");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void readExtrasOrFinish() {
        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        giftId = getIntent().getStringExtra(EXTRA_GIFT_ID);
        if (personId == null || giftId == null) {
            Toast.makeText(this, "Fehlende Daten", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void readAuthOrFinish() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    private void bindViews() {
        ivGiftImage = findViewById(R.id.ivGiftImage);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);   // Website-Link
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        makeReadOnly(etStatus);

        if (ivGiftImage != null) ivGiftImage.setAlpha(1f);
    }

    private void setupClickListeners() {
        findViewById(R.id.btnPickImage).setOnClickListener(v -> photoPicker.launch("image/*"));
        findViewById(R.id.btnImageUrl).setOnClickListener(v -> askForImageUrl());

        findViewById(R.id.btnMarkPlanned).setOnClickListener(v -> {
            bought = false;
            renderStatus();
        });

        findViewById(R.id.btnMarkBought).setOnClickListener(v -> {
            bought = true;
            renderStatus();
        });

        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void makeReadOnly(TextInputEditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(false);
        et.setCursorVisible(false);
        et.setLongClickable(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    private void askForImageUrl() {
        TextInputEditText input = new TextInputEditText(this);
        input.setHint("https://...");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);

        // Prefill mit aktueller Bild-URL (nicht Website-Link)
        input.setText(imageUrl != null ? imageUrl.trim() : "");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bild per Link")
                .setView(input)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Übernehmen", (d, w) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty()) {
                        Toast.makeText(this, "Bild-Link ist leer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    imageUrl = url;
                    showImage(imageUrl);
                })
                .show();
    }

    private void loadGiftToFields() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Geschenk nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String title = doc.getString("title");
                    Double price = doc.getDouble("price");
                    String link = doc.getString("link"); // Website-Link bleibt "link"
                    String note = doc.getString("note");
                    Boolean b = doc.getBoolean("bought");
                    String img = doc.getString("imageUrl");

                    bought = b != null && b;
                    imageUrl = img != null ? img : "";

                    if (etName != null) etName.setText(title != null ? title : "");
                    if (etPrice != null) etPrice.setText(price != null ? String.valueOf(price) : "");
                    if (etLink != null) etLink.setText(link != null ? link : "");
                    if (etNote != null) etNote.setText(note != null ? note : "");

                    renderStatus();
                    showImage(imageUrl);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void renderStatus() {
        if (etStatus != null) etStatus.setText(bought ? "Gekauft ✅" : "Geplant");
    }

    private void showImage(String uriOrUrl) {
        if (ivGiftImage == null) return;

        if (uriOrUrl == null || uriOrUrl.trim().isEmpty()) {
            ivGiftImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ivGiftImage.setImageResource(android.R.drawable.ic_menu_gallery);
            ivGiftImage.setAlpha(0.75f);
            return;
        }

        ivGiftImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ivGiftImage.setAlpha(1f);

        Glide.with(this).load(uriOrUrl).into(ivGiftImage);
    }

    private void save() {
        String name = etName != null && etName.getText() != null ? etName.getText().toString().trim() : "";
        String p = etPrice != null && etPrice.getText() != null ? etPrice.getText().toString().trim() : "";
        String websiteLink = etLink != null && etLink.getText() != null ? etLink.getText().toString().trim() : "";
        String n = etNote != null && etNote.getText() != null ? etNote.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Name darf nicht leer sein", Toast.LENGTH_SHORT).show();
            return;
        }

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
        data.put("title", name);
        data.put("price", pVal);

        // Website-Link bleibt getrennt
        data.put("link", websiteLink);

        data.put("note", n);
        data.put("bought", bought);

        // Bild getrennt
        data.put("imageUrl", imageUrl);

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
