package com.example.geschenkplaner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPersonActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText etPersonName = findViewById(R.id.etPersonName);
        Button btnSave = findViewById(R.id.btnSavePerson);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSave.setOnClickListener(v -> {
            String name = etPersonName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Bitte Namen eingeben.", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Bitte zuerst einloggen.", Toast.LENGTH_SHORT).show();
                return;
            }

            String uid = user.getUid();

            Map<String, Object> person = new HashMap<>();
            person.put("name", name);
            person.put("createdAt", System.currentTimeMillis());

            btnSave.setEnabled(false);

            db.collection("users")
                    .document(uid)
                    .collection("persons")
                    .add(person)
                    .addOnSuccessListener(ref -> {
                        Toast.makeText(this, "Person gespeichert: " + name, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        Toast.makeText(this, "Speichern fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });
    }
}
