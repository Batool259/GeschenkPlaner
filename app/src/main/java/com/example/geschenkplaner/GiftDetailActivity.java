package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private static final int REQ_EDIT_GIFT = 1001;

    private String uid;
    private String personId;
    private String giftId;

    private TextView tvTitle, tvPrice, tvLink, tvNote, tvStatus;
    private ImageView ivGiftImage;

    private String title = "—";
    private Double price = null;
    private String link = "";
    private String note = "";
    private boolean bought = false;

    private String imageUrl = ""; // Firestore Feld: "imageUrl"

    // Java-kompatibel: System-Picker (GetContent). Kann null liefern, wenn User abbricht.
    // Quelle: https://developer.android.com/training/basics/intents/result
    private final ActivityResultLauncher<String> photoPicker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) return;

                imageUrl = uri.toString();
                showImage(imageUrl);
                saveImageUrlToFirestore();
            });

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

        // Extras robust lesen
        Intent intent = getIntent();
        personId = intent.getStringExtra(EXTRA_PERSON_ID);
        giftId = intent.getStringExtra(EXTRA_GIFT_ID);

        if (personId == null || personId.trim().isEmpty() || giftId == null || giftId.trim().isEmpty()) {
            Toast.makeText(this, "Route fehlerhaft: personId/giftId fehlt", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ivGiftImage = findViewById(R.id.ivGiftImage);

        tvTitle = findViewById(R.id.tvTitle);
        tvPrice = findViewById(R.id.tvPrice);
        tvLink = findViewById(R.id.tvLink);
        tvNote = findViewById(R.id.tvNote);
        tvStatus = findViewById(R.id.tvStatus);

        // WICHTIG: Buttons sind ggf. nicht in jedem Layout vorhanden -> null-check, sonst Crash.
        // findViewById kann null liefern: https://developer.android.com/reference/android/app/Activity#findViewById(int)
        View pickBtn = findViewById(R.id.btnPickImage);
        if (pickBtn != null) pickBtn.setOnClickListener(v -> pickFromGallery());

        View urlBtn = findViewById(R.id.btnImageUrl);
        if (urlBtn != null) urlBtn.setOnClickListener(v -> askForImageUrl());

        View editBtn = findViewById(R.id.btnEdit);
        if (editBtn != null) {
            editBtn.setOnClickListener(v -> {
                Intent i = new Intent(this, EditGiftActivity.class);
                i.putExtra(EditGiftActivity.EXTRA_PERSON_ID, personId);
                i.putExtra(EditGiftActivity.EXTRA_GIFT_ID, giftId);
                startActivityForResult(i, REQ_EDIT_GIFT);
            });
        }

        View deleteBtn = findViewById(R.id.btnDeleteGift);
        if (deleteBtn != null) deleteBtn.setOnClickListener(v -> deleteGift());

        loadGift();
    }

    private void pickFromGallery() {
        photoPicker.launch("image/*");
    }

    private void askForImageUrl() {
        EditText input = new EditText(this);
        input.setHint("https://...");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Bild per Link")
                .setView(input)
                .setNegativeButton("Abbrechen", (d, w) -> d.dismiss())
                .setPositiveButton("Speichern", (d, w) -> {
                    String url = input.getText() != null ? input.getText().toString().trim() : "";
                    if (url.isEmpty()) {
                        Toast.makeText(this, "Link ist leer", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    imageUrl = url;
                    showImage(imageUrl);
                    saveImageUrlToFirestore();
                })
                .show();
    }

    private void showImage(String anyUriOrUrl) {
        if (ivGiftImage == null) return;

        if (anyUriOrUrl == null || anyUriOrUrl.trim().isEmpty()) {
            ivGiftImage.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }

        Glide.with(this)
                .load(anyUriOrUrl)
                .centerCrop()
                .into(ivGiftImage);
    }

    private void saveImageUrlToFirestore() {
        Map<String, Object> data = new HashMap<>();
        data.put("imageUrl", imageUrl);

        FirestorePaths.gift(uid, personId, giftId)
                .update(data)
                .addOnSuccessListener(v -> Toast.makeText(this, "Bild gespeichert ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private void render() {
        if (tvTitle != null) tvTitle.setText(title);

        if (tvPrice != null) {
            if (price != null) tvPrice.setText(String.format(Locale.GERMANY, "Preis: € %.2f", price));
            else tvPrice.setText("Preis: —");
        }

        if (tvLink != null) tvLink.setText("Link: " + (!link.trim().isEmpty() ? link : "—"));
        if (tvNote != null) tvNote.setText("Notiz: " + (!note.trim().isEmpty() ? note : "—"));
        if (tvStatus != null) tvStatus.setText("Status: " + (bought ? "Gekauft ✅" : "Geplant"));

        showImage(imageUrl);
    }

    private void loadGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Geschenk nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    title = doc.getString("title") != null ? doc.getString("title") : "—";
                    price = doc.getDouble("price");
                    link = doc.getString("link") != null ? doc.getString("link") : "";
                    note = doc.getString("note") != null ? doc.getString("note") : "";
                    Boolean b = doc.getBoolean("bought");
                    bought = b != null && b;

                    imageUrl = doc.getString("imageUrl") != null ? doc.getString("imageUrl") : "";

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
            loadGift();
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
