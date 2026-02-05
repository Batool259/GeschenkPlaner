package Fragments;

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

public class PersonListFragment extends Fragment {

    private RecyclerView rvPersons;
    private TextView tvEmpty;
    private FloatingActionButton fabAddPerson;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private ListenerRegistration personsListener;

    private final List<PersonRow> items = new ArrayList<>();
    private PersonAdapter adapter;

    public PersonListFragment() {
        // Leerer Konstruktor erforderlich
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_persons, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvPersons = view.findViewById(R.id.rvPersons);
        tvEmpty = view.findViewById(R.id.tvEmpty);
        fabAddPerson = view.findViewById(R.id.fabAddPerson);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        adapter = new PersonAdapter(items);
        rvPersons.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvPersons.setAdapter(adapter);

        // FAB: Person hinzufügen
        fabAddPerson.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddPersonActivity.class))
        );

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

        // Realtime-Listener auf Collection
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
    }

    private void stopPersonsListener() {
        if (personsListener != null) {
            personsListener.remove();
            personsListener = null;
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = items.isEmpty();
        rvPersons.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        tvEmpty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

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
