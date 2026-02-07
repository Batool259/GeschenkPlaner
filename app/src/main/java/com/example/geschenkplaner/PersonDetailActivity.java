package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.data.FirestorePaths;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";

    private String uid;
    private String personId;

    private TextView tvPersonName;
    private TextView tvBirthdayHint;
    private TextView tvPersonInitial;

    private GiftAdapterSimple adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Personendetails");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // User check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        uid = user.getUid();

        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null || personId.trim().isEmpty()) {
            Toast.makeText(this, "personId fehlt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvPersonName = findViewById(R.id.tvPersonName);
        tvBirthdayHint = findViewById(R.id.tvBirthdayHint);
        tvPersonInitial = findViewById(R.id.tvPersonInitial);

        RecyclerView rv = findViewById(R.id.rvGifts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GiftAdapterSimple();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddGift);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, AddGiftActivity.class);
            i.putExtra(AddGiftActivity.EXTRA_PERSON_ID, personId);
            startActivity(i);
        });

        loadPerson();
        loadGifts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGifts();
    }

    private void loadPerson() {
        FirestorePaths.person(uid, personId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Person nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = doc.getString("name");
                    String birthday = doc.getString("birthday");

                    String safeName = (name != null && !name.trim().isEmpty()) ? name.trim() : "—";
                    tvPersonName.setText(safeName);

                    if (birthday != null && !birthday.trim().isEmpty()) {
                        tvBirthdayHint.setText("🎂 Geburtstag: " + birthday.trim());
                    } else {
                        tvBirthdayHint.setText("🎂 Geburtstag: —");
                    }

                    tvPersonInitial.setText(getInitial(safeName));
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler Person: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String getInitial(String name) {
        if (name == null) return "?";
        String n = name.trim();
        if (n.isEmpty() || n.equals("—") || n.equals("(Ohne Name)")) return "?";
        return ("" + Character.toUpperCase(n.charAt(0)));
    }

    private void loadGifts() {
        FirestorePaths.gifts(uid, personId)
                .get()
                .addOnSuccessListener(qs -> {
                    ArrayList<GiftRow> list = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : qs) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        Double price = doc.getDouble("price");
                        Boolean bought = doc.getBoolean("bought");

                        list.add(new GiftRow(
                                id,
                                title != null ? title : "—",
                                price,
                                bought != null && bought
                        ));
                    }

                    adapter.setItems(list);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler Gifts: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---- Simple Adapter ----
    private static class GiftRow {
        final String id;
        final String title;
        final Double price;
        final boolean bought;

        GiftRow(String id, String title, Double price, boolean bought) {
            this.id = id;
            this.title = title;
            this.price = price;
            this.bought = bought;
        }
    }

    private class GiftAdapterSimple extends RecyclerView.Adapter<GiftVH> {

        private final ArrayList<GiftRow> items = new ArrayList<>();

        void setItems(ArrayList<GiftRow> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public GiftVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_gift, parent, false);
            return new GiftVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull GiftVH holder, int position) {
            GiftRow row = items.get(position);
            holder.bind(row);

            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(PersonDetailActivity.this, GiftDetailActivity.class);
                i.putExtra(GiftDetailActivity.EXTRA_GIFT_ID, row.id);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class GiftVH extends RecyclerView.ViewHolder {

        private final TextView tvTitle;
        private final TextView tvNote;
        private final Chip chipPrice;

        GiftVH(@NonNull android.view.View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGiftTitle);
            tvNote = itemView.findViewById(R.id.tvGiftNote);
            chipPrice = itemView.findViewById(R.id.chipPrice);
        }

        void bind(GiftRow row) {
            tvTitle.setText(row.title);
            tvNote.setText(row.bought ? "Gekauft ✅" : "Geplant");
            chipPrice.setText("€ " + (row.price != null ? row.price : "—"));
        }
    }
}
