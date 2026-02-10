package com.example.geschenkplaner.Fragments;

import android.app.AlertDialog; // Dialoge für Bearbeiten/Löschen
import android.content.Intent; // Für Activity-Wechsel
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher; // Reagiert auf Texteingaben
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu; // 3-Punkte-Menü
import androidx.fragment.app.Fragment; // Fragment = Teil eines Screens
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView; // Liste für viele Einträge

import com.example.geschenkplaner.activity.PersonDetailActivity;
import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.firebase.Timestamp; // Zeitstempel für Firestore
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration; // Zum Abmelden des Listeners
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements ToolbarConfig {
    // implements ToolbarConfig = dieses Fragment liefert einen Toolbar-Titel

    @Override
    public String getToolbarTitle() {
        // Titel, der oben in der Toolbar angezeigt wird
        return "GeschenkPlaner";
    }

    private FirebaseAuth auth; // Zugriff auf eingeloggten User
    private ListenerRegistration personsListener; // Firestore Listener merken

    private TextView tvGreeting; // Begrüßungstext
    private EditText etSearch; // Suchfeld
    private RecyclerView rvPersons; // Personenliste
    private TextView tvEmpty; // Text wenn Liste leer ist

    private final List<PersonRow> allItems = new ArrayList<>(); // Alle Personen aus Firestore
    private final List<PersonRow> filteredItems = new ArrayList<>(); // Gefilterte Personen
    private PersonAdapter adapter; // Adapter für RecyclerView

    private boolean demoSeededThisSession = false; // Demo-Daten nur einmal pro Session

    public HomeFragment() {} // Leerer Konstruktor für Fragment

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Layout für dieses Fragment laden
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views aus dem Layout holen
        tvGreeting = view.findViewById(R.id.tvGreeting);
        etSearch = view.findViewById(R.id.etSearch);
        rvPersons = view.findViewById(R.id.rvPersons);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        auth = FirebaseAuth.getInstance(); // FirebaseAuth initialisieren

        setGreeting(); // Begrüßung setzen

        adapter = new PersonAdapter(
                filteredItems,
                row -> {
                    // Klick auf Person -> Detailansicht öffnen
                    Intent i = new Intent(requireContext(), PersonDetailActivity.class);
                    i.putExtra(PersonDetailActivity.EXTRA_PERSON_ID, row.id);
                    startActivity(i);
                },
                (anchorView, row) -> showRowMenu(anchorView, row) // 3-Punkte-Menü öffnen
        );

        rvPersons.setLayoutManager(new LinearLayoutManager(requireContext())); // Normale Listenansicht
        rvPersons.setAdapter(adapter); // Adapter setzen

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Bei jeder Texteingabe Liste filtern
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateEmptyState(); // Prüfen ob Liste leer ist
    }

    @Override
    public void onStart() {
        super.onStart();
        // Listener starten wenn Fragment sichtbar wird
        startPersonsListener();
    }

    @Override
    public void onStop() {
        super.onStop();
        // Listener stoppen wenn Fragment nicht mehr sichtbar ist
        stopPersonsListener();
    }

    private void setGreeting() {
        // Begrüßung abhängig vom eingeloggten User setzen
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
        // Sicherheit: alten Listener beenden
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
                .orderBy("createdAt", Query.Direction.DESCENDING) // Neueste zuerst
                .addSnapshotListener((snap, err) -> {
                    allItems.clear(); // Liste neu aufbauen

                    if (err != null || snap == null) {
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

                    // Demo-Daten nur wenn noch keine Personen existieren
                    if (!demoSeededThisSession && allItems.isEmpty()) {
                        demoSeededThisSession = true;
                        seedDemo(uid);
                    }

                    applyFiltered(etSearch.getText() != null ? etSearch.getText().toString() : "");
                    updateEmptyState();
                });
    }

    private void stopPersonsListener() {
        // Firestore Listener sauber entfernen
        if (personsListener != null) {
            personsListener.remove();
            personsListener = null;
        }
    }

    private void filter(String query) {
        // Suchfilter anwenden
        applyFiltered(query);
        updateEmptyState();
    }

    private void applyFiltered(String query) {
        filteredItems.clear(); // Gefilterte Liste neu aufbauen
        String q = query != null ? query.trim().toLowerCase() : "";

        if (q.isEmpty()) {
            filteredItems.addAll(allItems); // Keine Suche -> alles anzeigen
        } else {
            for (PersonRow p : allItems) {
                if (p.name.toLowerCase().contains(q)) filteredItems.add(p);
            }
        }

        adapter.notifyDataSetChanged(); // RecyclerView neu zeichnen
    }

    private void updateEmptyState() {
        // Liste oder Leermeldung anzeigen
        boolean isEmpty = filteredItems.isEmpty();
        rvPersons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    private void showRowMenu(View anchor, PersonRow row) {
        // 3-Punkte-Menü anzeigen
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenuInflater().inflate(R.menu.person_row_menu, menu.getMenu());

        menu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                showEditDialog(row);
                return true;
            } else if (item.getItemId() == R.id.action_delete) {
                showDeleteDialog(row);
                return true;
            }
            return false;
        });

        menu.show();
    }

    private void showEditDialog(PersonRow row) {
        // Dialog zum Bearbeiten einer Person
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_person, null, false);

        EditText etName = content.findViewById(R.id.etEditName);
        EditText etBirthday = content.findViewById(R.id.etEditBirthday);
        EditText etNote = content.findViewById(R.id.etEditNote);

        etName.setText(row.name);
        etBirthday.setText(row.birthday);
        etNote.setText(row.note);

        new AlertDialog.Builder(requireContext())
                .setTitle(row.name != null && !row.name.trim().isEmpty() ? row.name : "Person bearbeiten")
                .setView(content)
                .setPositiveButton("Speichern", (d, w) -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    Map<String, Object> update = new HashMap<>();
                    update.put("name", etName.getText().toString().trim());
                    update.put("birthday", etBirthday.getText().toString().trim());
                    update.put("note", etNote.getText().toString().trim());
                    update.put("updatedAt", Timestamp.now());

                    FirestorePaths.person(user.getUid(), row.id).update(update);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showDeleteDialog(PersonRow row) {
        // Sicherheitsabfrage vor dem Löschen
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

    private void seedDemo(String uid) {
        // Demo-Personen anlegen
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

    private static class PersonRow {
        // Datenobjekt für eine Person
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
            // Ersten Buchstaben für Initiale zurückgeben
            if (name == null || name.trim().isEmpty()) return "?";
            return String.valueOf(Character.toUpperCase(name.trim().charAt(0)));
        }
    }

    private interface OnPersonClick {
        // Callback für Klick auf Person
        void onClick(PersonRow row);
    }

    private interface OnMoreClick {
        // Callback für Klick auf 3-Punkte
        void onMoreClick(View anchorView, PersonRow row);
    }

    private static class PersonAdapter extends RecyclerView.Adapter<PersonVH> {
        // Adapter verbindet Personenliste mit RecyclerView
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
            // Layout für eine Zeile laden
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_person, parent, false);
            return new PersonVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PersonVH holder, int position) {
            // Daten an ViewHolder binden
            PersonRow row = data.get(position);
            holder.bind(row);

            holder.itemView.setOnClickListener(v -> onClick.onClick(row));
            holder.btnMore.setOnClickListener(v -> onMoreClick.onMoreClick(holder.btnMore, row));
        }

        @Override
        public int getItemCount() {
            return data.size(); // Anzahl der Einträge
        }
    }

    private static class PersonVH extends RecyclerView.ViewHolder {
        // Hält die Views einer einzelnen Listenzeile
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
            // Daten in die Views setzen
            tvName.setText(row.name);
            tvInfo.setText(row.birthday == null || row.birthday.isEmpty()
                    ? "Geburtstag: —"
                    : "Geburtstag: " + row.birthday);
            tvInitial.setText(row.initial());
        }
    }
}
