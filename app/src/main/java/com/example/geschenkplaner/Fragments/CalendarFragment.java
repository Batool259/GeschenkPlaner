package com.example.geschenkplaner.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.R;
import com.example.geschenkplaner.data.FirestorePaths;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment implements ToolbarConfig {

    @Override
    public String getToolbarTitle() {
        return "Kalender";
    }

    private CalendarView calendarView;
    private TextView tvSelectedDate;
    private TextView tvMarkedDays;
    private RecyclerView rvEvents;

    private EventAdapter adapter;

    private FirebaseFirestore db;
    private String uid;
    private String dateKey;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    public CalendarFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        calendarView = v.findViewById(R.id.calendarView);
        tvSelectedDate = v.findViewById(R.id.tvDate);
        tvMarkedDays = v.findViewById(R.id.tvMarkedDays);
        rvEvents = v.findViewById(R.id.rvEvents);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bitte einloggen", Toast.LENGTH_SHORT).show();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        adapter = new EventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        // Startdatum = heute
        dateKey = sdf.format(Calendar.getInstance().getTime());
        tvSelectedDate.setText("🟣 " + dateKey);

        loadEventsForSelectedDate();
        loadMarkedDays();

        // Datum klicken -> anzeigen + Dialog zum Hinzufügen
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);

            dateKey = sdf.format(c.getTime());
            tvSelectedDate.setText("🟣 " + dateKey);

            loadEventsForSelectedDate();
            showAddEventDialog(); // 👈 Eingabe direkt beim Klick
        });
    }

    // ----------------------------
    // Dialog: neuen Eintrag hinzufügen
    // ----------------------------
    private void showAddEventDialog() {
        EditText input = new EditText(requireContext());
        input.setHint("Eintrag (z.B. Geschenk kaufen)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag für " + dateKey)
                .setView(input)
                .setPositiveButton("Speichern", (d, w) -> {
                    String text = input.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) {
                        Toast.makeText(getContext(), "Text eingeben", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    saveEventText(text);
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void saveEventText(String text) {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("dateKey", dateKey);
        data.put("text", text);
        data.put("createdAt", Timestamp.now());
        data.put("updatedAt", Timestamp.now());

        FirestorePaths.events(uid)
                .add(data)
                .addOnSuccessListener(r -> {
                    loadEventsForSelectedDate();
                    loadMarkedDays();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadEventsForSelectedDate() {
        FirestorePaths.events(uid)
                .whereEqualTo("dateKey", dateKey)
                .get()
                .addOnSuccessListener(qs -> {
                    ArrayList<EventRow> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String txt = doc.getString("text");
                        list.add(new EventRow(doc.getId(), txt != null ? txt : "—"));
                    }
                    adapter.setItems(list);
                });
    }

    private void loadMarkedDays() {
        FirestorePaths.events(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    ArrayList<String> days = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : qs) {
                        String dk = doc.getString("dateKey");
                        if (dk != null && !days.contains(dk)) days.add(dk);
                    }

                    if (days.isEmpty()) {
                        tvMarkedDays.setText("Keine markierten Tage");
                    } else {
                        StringBuilder sb = new StringBuilder("Markiert: ");
                        for (int i = 0; i < days.size(); i++) {
                            sb.append("🟣 ").append(days.get(i));
                            if (i < days.size() - 1) sb.append(" · ");
                        }
                        tvMarkedDays.setText(sb.toString());
                    }
                });
    }

    // ----------------------------
    // RecyclerView: Model + Adapter
    // ----------------------------
    private static class EventRow {
        final String id;
        final String text;

        EventRow(String id, String text) {
            this.id = id;
            this.text = text;
        }
    }

    private class EventAdapter extends RecyclerView.Adapter<EventVH> {
        private final ArrayList<EventRow> items = new ArrayList<>();

        void setItems(ArrayList<EventRow> list) {
            items.clear();
            items.addAll(list);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new EventVH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull EventVH holder, int position) {
            EventRow row = items.get(position);
            holder.bind(row);

            holder.itemView.setOnClickListener(v -> showEditDialog(row));
            holder.itemView.setOnLongClickListener(v -> {
                showDeleteDialog(row);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private static class EventVH extends RecyclerView.ViewHolder {
        final TextView tv;

        EventVH(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(android.R.id.text1);
        }

        void bind(EventRow row) {
            tv.setText("• " + row.text);
        }
    }

    private void showEditDialog(EventRow row) {
        EditText input = new EditText(requireContext());
        input.setText(row.text);

        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag bearbeiten")
                .setView(input)
                .setPositiveButton("Speichern", (d, w) -> {
                    String newText = input.getText().toString().trim();
                    if (TextUtils.isEmpty(newText)) return;

                    Map<String, Object> update = new HashMap<>();
                    update.put("text", newText);
                    update.put("updatedAt", Timestamp.now());

                    FirestorePaths.event(uid, row.id)
                            .update(update)
                            .addOnSuccessListener(x -> {
                                loadEventsForSelectedDate();
                                loadMarkedDays();
                            });
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showDeleteDialog(EventRow row) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag löschen?")
                .setMessage(row.text)
                .setPositiveButton("Löschen", (d, w) -> {
                    FirestorePaths.event(uid, row.id)
                            .delete()
                            .addOnSuccessListener(x -> {
                                loadEventsForSelectedDate();
                                loadMarkedDays();
                            });
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }
}
