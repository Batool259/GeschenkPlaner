package com.example.geschenkplaner.Fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geschenkplaner.MainActivity;
import com.example.geschenkplaner.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddPersonFragment extends Fragment implements ToolbarConfig {

    @Override
    public String getToolbarTitle() {
        return "Person hinzufügen";
    }

    private EditText etName, etBirthday, etNote;
    private FirebaseFirestore db;
    private String uid;

    public AddPersonFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_person, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etName = view.findViewById(R.id.etPersonName);
        etBirthday = view.findViewById(R.id.etPersonBirthday);
        etNote = view.findViewById(R.id.etPersonNote);

        Button btnSave = view.findViewById(R.id.btnSavePerson);
        Button btnCancel = view.findViewById(R.id.btnCancelPerson);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bitte einloggen.", Toast.LENGTH_SHORT).show();
            goHome();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        //  Abbrechen -> Home
        btnCancel.setOnClickListener(v -> goHome());

        //  Speichern -> danach Home
        btnSave.setOnClickListener(v -> savePerson());
    }

    private void goHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openHome();
        }
    }

    private void savePerson() {
        String name = etName.getText().toString().trim();
        String birthday = etBirthday.getText().toString().trim();
        String note = etNote.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            Toast.makeText(getContext(), "Name fehlt.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("birthday", birthday); // simple String (z.B. 12.03.2003)
        data.put("note", note);
        data.put("createdAt", Timestamp.now());

        db.collection("users")
                .document(uid)
                .collection("persons")
                .add(data)
                .addOnSuccessListener(r -> {
                    Toast.makeText(getContext(), "Person gespeichert ✅", Toast.LENGTH_SHORT).show();
                    goHome();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
