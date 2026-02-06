package com.example.geschenkplaner.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.MainActivity;
import com.example.geschenkplaner.PersonDetailActivity;
import com.example.geschenkplaner.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.widget.AppCompatTextView;

public class HomeFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration personsListener;

    private TextView tvGreeting;
    private EditText etSearch;
    private RecyclerView rvPersons;
    private TextView tvEmpty;
    private FloatingActionButton fabAddPerson;

    private final List<PersonRow> allItems = new ArrayList<>();
    private final List<PersonRow> filteredItems = new ArrayList<>();
    private PersonAdapter adapter;

    private boolean demoSeededThisSession = false;

    public HomeFragment() { }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvGreeting = view.findViewById(R.id.tvGreeting);
        etSearch = view.findViewById(R.id.etSearch);
        rvPersons = view.findViewById(R.id.rvPersons);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        fabAddPerson = view.findViewById(R.id.fabAddPerson);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setGreeting();

        adapter = new PersonAdapter(filteredItems, row -> {
            Intent i = new Intent(requireContext(), PersonDetailActivity.class);
            i.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, row.id);
            startActivity(i);
        });

        rvPersons.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersons.setAdapter(adapter);

        fabAddPerson.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).navigateToAddPerson();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filter(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateEmptyState();
    }

    @Override
    public void onStart() {
        super.onStart();
        startPersonsListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopPersonsListener();
    }

    private void setGreeting() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            tvGreeting.setText("Hallo 👋");
            return;
        }
        String name = user.getDisplayName();
        if (name != null && !name.trim().isEmpty()) {
            tvGreeting.setText("Hallo, " + name + " 👋");
        } else if (user.getEmail() != null) {
            tvGreeting.setText("Hallo, " + user.getEmail() + " 👋");
        } else {
            tvGreeting.setText("Hallo 👋");
        }
    }

    private void startPersonsListener() {
        stopPersonsListener();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            allItems.clear();
            applyFiltered("");
            tvEmpty.setText("Bitte zuerst einloggen.");
            updateEmptyState();
            return;
        }

        String uid = user.getUid();

        personsListener = db.collection("users")
                .document(uid)
                .collection("persons")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    allItems.clear();

                    if (err != null || snap == null) {
                        applyFiltered("");
                        tvEmpty.setText("Fehler beim Laden der Personen.");
                        updateEmptyState();
                        return;
                    }

                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        String birthday = doc.getString("birthday");
                        if (name == null || name.trim().isEmpty()) name = "(Ohne Name)";
                        allItems.add(new PersonRow(doc.getId(), name, birthday != null ? birthday : ""));
                    }

                    // Demo seed: nur wenn wirklich leer (Uni-Projekt Demo)
                    if (!demoSeededThisSession && allItems.isEmpty()) {
                        demoSeededThisSession = true;
                        seedDemo(uid);
                    }

                    applyFiltered(etSearch.getText() != null ? etSearch.getText().toString() : "");
                    updateEmptyState();
                });
    }

    private void seedDemo(String uid) {
        // 3 Demo-Personen + je 2 Demo-Geschenke (minimal)
        String[] demoNames = {"Patrick Schmidt", "Lena Müller", "Tom Braun"};
        String[] demoBirth = {"12.03.2003", "01.11.2002", "26.07.2003"};

        for (int i = 0; i < demoNames.length; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("name", demoNames[i]);
            p.put("birthday", demoBirth[i]);
            p.put("note", "");
            p.put("createdAt", Timestamp.now());

            db.collection("users")
                    .document(uid)
                    .collection("persons")
                    .add(p)
                    .addOnSuccessListener(personRef -> {
                        // Demo gifts
                        addDemoGift(uid, personRef.getId(), "Parfum", 39.99);
                        addDemoGift(uid, personRef.getId(), "Buch", 14.99);
                    });
        }
    }

    private void addDemoGift(String uid, String personId, String title, double price) {
        Map<String, Object> g = new HashMap<>();
        g.put("uid", uid);
        g.put("personId", personId);
        g.put("title", title);
        g.put("price", price);
        g.put("link", "");
        g.put("bought", false);
        g.put("createdAt", Timestamp.now());

        db.collection("gifts").add(g);
    }

    private void stopPersonsListener() {
        if (personsListener != null) {
            personsListener.remove();
            personsListener = null;
        }
    }

    private void filter(String query) {
        applyFiltered(query);
        updateEmptyState();
    }

    private void applyFiltered(String query) {
        filteredItems.clear();
        String q = query != null ? query.trim().toLowerCase() : "";

        if (q.isEmpty()) {
            filteredItems.addAll(allItems);
        } else {
            for (PersonRow p : allItems) {
                if (p.name.toLowerCase().contains(q)) filteredItems.add(p);
            }
        }

        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredItems.isEmpty();
        rvPersons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ---- RecyclerView ----
    private static class PersonRow {
        final String id;
        final String name;
        final String birthday;

        PersonRow(String id, String name, String birthday) {
            this.id = id;
            this.name = name;
            this.birthday = birthday;
        }
    }

    private interface OnPersonClick {
        void onClick(PersonRow row);
    }

    private static class PersonAdapter extends RecyclerView.Adapter<PersonVH> {
        private final List<PersonRow> data;
        private final OnPersonClick onClick;

        PersonAdapter(List<PersonRow> data, OnPersonClick onClick) {
            this.data = data;
            this.onClick = onClick;
        }

        @NonNull
        @Override
        public PersonVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_person, parent, false);
            return new PersonVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PersonVH holder, int position) {
            PersonRow row = data.get(position);
            holder.bind(row);
            holder.itemView.setOnClickListener(v -> onClick.onClick(row));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static class PersonVH extends RecyclerView.ViewHolder {
        private final AppCompatTextView tvName;
        private final AppCompatTextView tvInfo;

        PersonVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);
        }

        void bind(PersonRow row) {
            tvName.setText(row.name);
            if (row.birthday == null || row.birthday.trim().isEmpty()) {
                tvInfo.setText("Geburtstag: —");
            } else {
                tvInfo.setText("Geburtstag: " + row.birthday);
            }
        }
    }
}
