package com.example.geschenkplaner;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class PersonDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PERSON_ID = "personId";

    private FirebaseFirestore db;
    private String uid;
    private String personId;

    private TextView tvName, tvBirthdayHint;
    private GiftAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_detail);

        // 1) User check
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        uid = user.getUid();
        db = FirebaseFirestore.getInstance();

        // 2) personId holen
        personId = getIntent().getStringExtra(EXTRA_PERSON_ID);
        if (personId == null || personId.isEmpty()) {
            Toast.makeText(this, "personId fehlt", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3) Views
        tvName = findViewById(R.id.tvPersonName);
        tvBirthdayHint = findViewById(R.id.tvBirthdayHint);

        RecyclerView rv = findViewById(R.id.rvGifts);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new GiftAdapter();
        rv.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAddGift);
        fab.setOnClickListener(v -> {
            Intent i = new Intent(this, AddGiftActivity.class);
            i.putExtra(AddGiftActivity.EXTRA_PERSON_ID, personId);
            startActivity(i);
        });

        // 4) Laden
        loadPerson();
        loadGifts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadGifts();
    }

    private void loadPerson() {
        db.collection("persons")
                .document(personId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Person nicht gefunden", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String owner = doc.getString("uid");
                    if (owner != null && !owner.equals(uid)) {
                        Toast.makeText(this, "Kein Zugriff", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    String name = doc.getString("name");
                    tvName.setText(name != null ? name : "—");

                    // erstmal einfach
                    tvBirthdayHint.setText("🎂 Geburtstag: —");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler Person: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loadGifts() {
        db.collection("gifts")
                .whereEqualTo("uid", uid)
                .whereEqualTo("personId", personId)
                .get()
                .addOnSuccessListener(qs -> {
                    ArrayList<GiftItem> list = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : qs) {
                        String id = doc.getId();
                        String title = doc.getString("title");
                        Double price = doc.getDouble("price");
                        Boolean bought = doc.getBoolean("bought");

                        list.add(new GiftItem(
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
}
