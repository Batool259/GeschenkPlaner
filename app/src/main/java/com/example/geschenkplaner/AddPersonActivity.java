package com.example.geschenkplaner;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AddPersonActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_person);

        EditText etPersonName = findViewById(R.id.etPersonName);
        Button btnSave = findViewById(R.id.btnSavePerson);

        btnSave.setOnClickListener(v -> {
            String name = etPersonName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Bitte Namen eingeben.", Toast.LENGTH_SHORT).show();
                return;
            }
            // bestätigen, Speichern kommt später (Firebase).
            Toast.makeText(this, "Person gespeichert: " + name, Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
