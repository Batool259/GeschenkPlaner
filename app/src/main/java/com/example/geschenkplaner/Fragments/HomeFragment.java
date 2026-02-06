package com.example.geschenkplaner.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.AddPersonActivity;
import com.example.geschenkplaner.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

/**
 * Home = Personenliste + Begrüßung
 *
 * Hinweis: Dieses Fragment lädt fragment_home.xml und erwartet dort:
 * tvGreeting, rvPersons, tvEmpty, fabAddPerson
 */
public class HomeFragment extends Fragment {

    // Firebase
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Listener (muss in onStop() entfernt werden)
    private ListenerRegistration personsListener;

    // UI
    private TextView tvGreeting;
    private RecyclerView rvPersons;
    private TextView tvEmpty;
    private FloatingActionButton fabAddPerson;

    // RecyclerView
    private final List<PersonRow> items = new ArrayList<>();
    private PersonAdapter adapter;

    public HomeFragment() {
        // required empty constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Home-Screen Layout (muss Liste + Greeting enthalten)
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Views verbinden
        tvGreeting = view.findViewById(R.id.tvGreeting);
        rvPersons = view.findViewById(R.id.rvPersons);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        fabAddPerson = view.findViewById(R.id.fabAddPerson);

        // Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Begrüßung setzen
        setGreeting();

        // RecyclerView Setup
        adapter = new PersonAdapter(items);
        rvPersons.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersons.setAdapter(adapter);

        // FAB: Person hinzufügen (bei euch aktuell Activity)
        fabAddPerson.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddPersonActivity.class))
        );

        updateEmptyState();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Listener in onStart starten und in onStop entfernen ist ein übliches Lifecycle-Pattern
        // (damit keine Listener im Hintergrund weiterlaufen).
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

        String displayName = user.getDisplayName();
        String email = user.getEmail();

        if (displayName != null && !displayName.trim().isEmpty()) {
            tvGreeting.setText("Hallo, " + displayName + " 👋");
        } else if (email != null && !email.trim().isEmpty()) {
            tvGreeting.setText("Hallo, " + email + " 👋");
        } else {
            tvGreeting.setText("Hallo 👋");
        }
    }

    private void startPersonsListener() {
        stopPersonsListener();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            items.clear();
            adapter.notifyDataSetChanged();
            tvEmpty.setText("Bitte zuerst einloggen.");
            updateEmptyState();
            return;
        }

        String uid = user.getUid();

        // Realtime Listener auf persons Collection
        personsListener = db.collection("users")
                .document(uid)
                .collection("persons")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) {
                        items.clear();
                        adapter.notifyDataSetChanged();
                        tvEmpty.setText("Fehler beim Laden der Personen.");
                        updateEmptyState();
                        return;
                    }

                    items.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String name = doc.getString("name");
                        if (name == null || name.trim().isEmpty()) name = "(Ohne Name)";
                        items.add(new PersonRow(doc.getId(), name));
                    }

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                });

        // Firestore Snapshot Listener sind „persistent“ und sollten wieder entfernt werden,
        // z. B. über ListenerRegistration.remove(). :contentReference[oaicite:1]{index=1}
    }

    private void stopPersonsListener() {
        if (personsListener != null) {
            personsListener.remove();
            personsListener = null;
        }
        // remove() ist der offizielle Weg, einen Listener zu detachen. :contentReference[oaicite:2]{index=2}
    }

    private void updateEmptyState() {
        boolean isEmpty = items.isEmpty();
        rvPersons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    // ---- RecyclerView Hilfsklassen ----
    private static class PersonRow {
        final String id;
        final String name;

        PersonRow(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static class PersonAdapter extends RecyclerView.Adapter<PersonViewHolder> {

        private final List<PersonRow> data;

        PersonAdapter(List<PersonRow> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new PersonViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
            holder.bind(data.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    private static class PersonViewHolder extends RecyclerView.ViewHolder {

        private final TextView tv;

        PersonViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
        }

        void bind(PersonRow row) {
            tv.setText(row.name);
        }
    }
}
