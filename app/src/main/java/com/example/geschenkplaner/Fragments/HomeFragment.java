package com.example.geschenkplaner.Fragments;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.activity.PersonDetailActivity;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements ToolbarConfig {

    @Override
    public String getToolbarTitle() {
        return "GeschenkPlaner";
    }

    private FirebaseAuth auth;
    private ListenerRegistration personsListener;

    private TextView tvGreeting;
    private EditText etSearch;
    private RecyclerView rvPersons;
    private TextView tvEmpty;

    private final List<PersonRow> allItems = new ArrayList<>();
    private final List<PersonRow> filteredItems = new ArrayList<>();
    private PersonAdapter adapter;

    // Wenn du Demo nicht mehr willst: auf false lassen und seedDemo-Block unten entfernen
    private boolean demoSeededThisSession = false;

    public HomeFragment() {}

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

        auth = FirebaseAuth.getInstance();

        setGreeting();

        adapter = new PersonAdapter(filteredItems,
                row -> {
                    Intent i = new Intent(requireContext(), PersonDetailActivity.class);
                    i.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, row.id);
                    startActivity(i);
                },
                (anchorView, row) -> showRowMenu(anchorView, row)
        );

        rvPersons.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersons.setAdapter(adapter);


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

        personsListener = FirestorePaths.persons(uid)
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
                        String note = doc.getString("note");

                        if (name == null || name.trim().isEmpty()) name = "(Ohne Name)";
                        allItems.add(new PersonRow(
                                doc.getId(),
                                name,
                                birthday != null ? birthday : "",
                                note != null ? note : ""
                        ));
                    }

                    // Demo seed: nur wenn wirklich leer
                    if (!demoSeededThisSession && allItems.isEmpty()) {
                        demoSeededThisSession = true;
                        seedDemo(uid);
                    }

                    applyFiltered(etSearch.getText() != null ? etSearch.getText().toString() : "");
                    updateEmptyState();
                });
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

    // --- 3-Punkte Menü ---
    private void showRowMenu(View anchor, PersonRow row) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.person_row_menu, menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                showEditDialog(row);
                return true;
            } else if (id == R.id.action_delete) {
                showDeleteDialog(row);
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void showEditDialog(PersonRow row) {
        View content = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_person, null, false);

        EditText etName = content.findViewById(R.id.etEditName);
        EditText etBirthday = content.findViewById(R.id.etEditBirthday);
        EditText etNote = content.findViewById(R.id.etEditNote);

        etName.setText(row.name);
        etBirthday.setText(row.birthday);
        etNote.setText(row.note);

        new AlertDialog.Builder(requireContext())
                .setTitle((row.name != null && !row.name.trim().isEmpty()) ? row.name.trim() : "Person bearbeiten")
                .setView(content)
                .setPositiveButton("Speichern", (d, w) -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String newName = etName.getText().toString().trim();
                    String newBirthday = etBirthday.getText().toString().trim();
                    String newNote = etNote.getText().toString().trim();

                    if (newName.isEmpty()) newName = "(Ohne Name)";

                    Map<String, Object> update = new HashMap<>();
                    update.put("name", newName);
                    update.put("birthday", newBirthday);
                    update.put("note", newNote);
                    update.put("updatedAt", Timestamp.now());

                    FirestorePaths.person(user.getUid(), row.id).update(update);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showDeleteDialog(PersonRow row) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Person löschen?")
                .setMessage(row.name)
                .setPositiveButton("Löschen", (d, w) -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;
                    FirestorePaths.person(user.getUid(), row.id).delete();
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    // --- Demo (optional) ---
    private void seedDemo(String uid) {
        String[] demoNames = {"Patrick Schmidt", "Lena Müller", "Tom Braun"};
        String[] demoBirth = {"12.03.2003", "01.11.2002", "26.07.2003"};

        for (int i = 0; i < demoNames.length; i++) {
            Map<String, Object> p = new HashMap<>();
            p.put("name", demoNames[i]);
            p.put("birthday", demoBirth[i]);
            p.put("note", "");
            p.put("createdAt", Timestamp.now());

            FirestorePaths.persons(uid).add(p);
        }
    }

    // ---- RecyclerView ----
    private static class PersonRow {
        final String id;
        final String name;
        final String birthday;
        final String note;

        PersonRow(String id, String name, String birthday, String note) {
            this.id = id;
            this.name = name;
            this.birthday = birthday;
            this.note = note;
        }

        String initial() {
            String n = name != null ? name.trim() : "";
            if (n.isEmpty()) return "?";
            return ("" + Character.toUpperCase(n.charAt(0)));
        }
    }

    private interface OnPersonClick {
        void onClick(PersonRow row);
    }

    private interface OnMoreClick {
        void onMoreClick(View anchorView, PersonRow row);
    }

    private static class PersonAdapter extends RecyclerView.Adapter<PersonVH> {
        private final List<PersonRow> data;
        private final OnPersonClick onClick;
        private final OnMoreClick onMoreClick;

        PersonAdapter(List<PersonRow> data, OnPersonClick onClick, OnMoreClick onMoreClick) {
            this.data = data;
            this.onClick = onClick;
            this.onMoreClick = onMoreClick;
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
            holder.btnMore.setOnClickListener(v -> onMoreClick.onMoreClick(holder.btnMore, row));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static class PersonVH extends RecyclerView.ViewHolder {

        private final TextView tvName;
        private final TextView tvInfo;
        private final TextView tvInitial;
        private final ImageButton btnMore;




        PersonVH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvInfo = itemView.findViewById(R.id.tvInfo);

            tvInitial = itemView.findViewById(R.id.tvInitial);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        void bind(PersonRow row) {
            tvName.setText(row.name);

            if (row.birthday == null || row.birthday.trim().isEmpty()) {
                tvInfo.setText("Geburtstag: —");
            } else {
                tvInfo.setText("Geburtstag: " + row.birthday);
            }

            tvInitial.setText(row.initial());
        }
    }
}
