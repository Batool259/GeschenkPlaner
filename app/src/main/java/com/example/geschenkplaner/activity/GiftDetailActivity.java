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

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public class GiftDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";
    public static final String EXTRA_GIFT_ID = "giftId";

    private static final int REQ_EDIT_GIFT = 1001;

    private static final String EXTRA_OPEN_FRAGMENT = "open_fragment";
    private static final String FRAG_HOME = "home";
    private static final String FRAG_ADD_PERSON = "add_person";
    private static final String FRAG_CALENDAR = "calendar";
    private static final String FRAG_SETTINGS = "settings";

    private String uid;
    private String personId;
    private String giftId;

    private ImageView ivGiftImage;
    private EditText etName, etPrice, etLink, etNote, etStatus;

    private String title = "";
    private Double price = null;
    private String link = "";     // Website-Link
    private String note = "";
    private boolean bought = false;
    private String imageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gift_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Intent intent = getIntent();
        personId = intent.getStringExtra(EXTRA_PERSON_ID);
        giftId = intent.getStringExtra(EXTRA_GIFT_ID);

        if (personId == null || giftId == null) {
            Toast.makeText(this, "Fehlende Daten", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        ivGiftImage = findViewById(R.id.ivGiftImage);
        if (ivGiftImage != null) ivGiftImage.setAlpha(1f);

        etName = findViewById(R.id.etName);
        etPrice = findViewById(R.id.etPrice);
        etLink = findViewById(R.id.etLink);
        etNote = findViewById(R.id.etNote);
        etStatus = findViewById(R.id.etStatus);

        makeReadOnly(etName);
        makeReadOnly(etPrice);

        // Link: NICHT komplett deaktivieren, weil er klickbar sein soll
        makeReadOnlyButClickable(etLink);
        etLink.setOnClickListener(v -> openWebsiteLink(link));

        makeReadOnly(etNote);
        makeReadOnly(etStatus);

        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent i = new Intent(this, EditGiftActivity.class);
            i.putExtra(EditGiftActivity.EXTRA_PERSON_ID, personId);
            i.putExtra(EditGiftActivity.EXTRA_GIFT_ID, giftId);
            startActivityForResult(i, REQ_EDIT_GIFT);
        });

        findViewById(R.id.btnDeleteGift).setOnClickListener(v -> deleteGift());

        loadGift();
    }

    private void makeReadOnly(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(false);
        et.setLongClickable(false);
        et.setCursorVisible(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    private void makeReadOnlyButClickable(EditText et) {
        if (et == null) return;
        et.setFocusable(false);
        et.setClickable(true);
        et.setLongClickable(true);
        et.setCursorVisible(false);
        et.setInputType(InputType.TYPE_NULL);
        et.setKeyListener(null);
    }

    private void openMainFragment(String which) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(EXTRA_OPEN_FRAGMENT, which);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
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

                    title = doc.getString("title") != null ? doc.getString("title") : "";
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

    private void render() {
        if (etName != null) etName.setText(title);

        if (etPrice != null) {
            if (price != null) etPrice.setText(String.format(Locale.GERMANY, "%.2f", price));
            else etPrice.setText("");
        }

        if (etLink != null) etLink.setText(link);
        if (etNote != null) etNote.setText(note);
        if (etStatus != null) etStatus.setText(bought ? "Gekauft ✅" : "Geplant");

        showImage(imageUrl);
    }

    private String normalizeUrl(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.isEmpty()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        return "https://" + s;
    }

    private void openWebsiteLink(String rawUrl) {
        String url = normalizeUrl(rawUrl);
        if (url.isEmpty()) {
            Toast.makeText(this, "Kein Website-Link gespeichert", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Link kann nicht geöffnet werden", Toast.LENGTH_SHORT).show();
        }
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

        Glide.with(this)
                .load(uriOrUrl)
                .into(ivGiftImage);
    }

    private void deleteGift() {
        FirestorePaths.gift(uid, personId, giftId)
                .delete()
                .addOnSuccessListener(v -> {
                    Toast.makeText(this, "Geschenk gelöscht", Toast.LENGTH_SHORT).show();
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
