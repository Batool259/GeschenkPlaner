package com.example.geschenkplaner.Fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class CalendarFragment extends Fragment implements ToolbarConfig {


    @Override
    public String getToolbarTitle() {
        return "Kalender";
    }

    // UI-Elemente
    private MaterialCalendarView calendarView;
    private TextView tvToggleCalendar;
    private RecyclerView rvEvents;


    private EventAdapter adapter;

    private String uid;
    private String selectedDateKey;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final Set<CalendarDay> markedDays = new HashSet<>();

    // Layout für das Fragment laden
    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_calendar, container, false);
    }


    @Override
    public void onViewCreated(
            @NonNull View v,
            @Nullable Bundle savedInstanceState
    ) {
        super.onViewCreated(v, savedInstanceState);

        // Views aus dem Layout holen
        calendarView = v.findViewById(R.id.calendarView);
        tvToggleCalendar = v.findViewById(R.id.tvToggleCalendar);
        rvEvents = v.findViewById(R.id.rvEvents);

        //  ohne Login keine Kalenderdaten laden
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bitte einloggen", Toast.LENGTH_SHORT).show();
            return;
        }

        // UID speichern, damit Firestore-Pfade unter
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();


        adapter = new EventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        // Standard: heutiges Datum als ausgewähltes Datum setzen
        Calendar today = Calendar.getInstance();
        selectedDateKey = sdf.format(today.getTime());

        calendarView.setSelectedDate(CalendarDay.today());
        calendarView.setCurrentDate(CalendarDay.today());

        // Daten laden:
        loadEventsSorted();     // Events in Liste anzeigen (selected zuerst)
        loadMarkedDays();      // Tage mit Events als Punkte markieren

        // Toggle: Kalender-Ansicht ein-/ausblenden
        tvToggleCalendar.setOnClickListener(x -> {
            if (calendarView.getVisibility() == View.VISIBLE) {
                calendarView.setVisibility(View.GONE);
                tvToggleCalendar.setText("Kalender anzeigen");
            } else {
                calendarView.setVisibility(View.VISIBLE);
                tvToggleCalendar.setText("Kalender ausblenden");
            }
        });


        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Calendar c = Calendar.getInstance();

            c.set(date.getYear(), date.getMonth() - 1, date.getDay());
            selectedDateKey = sdf.format(c.getTime());

            loadEventsSorted();
            showAddEventDialog();
        });
    }


    private void loadEventsSorted() {
        FirestorePaths.events(uid)
                .orderBy("dateKey")
                .get()
                .addOnSuccessListener(qs -> {

                    List<EventRow> selected = new ArrayList<>();
                    List<EventRow> others = new ArrayList<>();

                    for (QueryDocumentSnapshot d : qs) {
                        String dateKey = d.getString("dateKey");
                        if (dateKey == null) continue;

                        // Titel des Eintrags
                        String title = d.getString("text");
                        if (title == null || title.trim().isEmpty()) title = "—";

                        // Optional: Personname (für Subtitle)
                        String subtitle = d.getString("personName");
                        if (subtitle == null) subtitle = "";

                        // Anzeige im UI: Tag + Monatsname kurz
                        String day = "—";
                        String monthShort = "—";

                        // dateKey soll "yyyy-MM-dd" sein -> daraus Tag/Monat extrahieren
                        if (dateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            String[] p = dateKey.split("-");
                            day = p[2];

                            int month = Integer.parseInt(p[1]);
                            String[] months = new DateFormatSymbols(Locale.GERMAN).getShortMonths();
                            monthShort = months[month - 1];

                            // Monat schön formatieren (z.B. "Mär." -> groß anfangen)
                            if (monthShort != null && !monthShort.isEmpty()) {
                                monthShort = monthShort.substring(0, 1).toUpperCase() + monthShort.substring(1);
                            }
                        }

                        // Row-Objekt für RecyclerView bauen
                        EventRow row = new EventRow(
                                d.getId(),   // Firestore-Dokument-ID
                                day,
                                monthShort,
                                title,
                                subtitle,
                                dateKey
                        );

                        // Reihenfolge: selected Datum oben, Rest darunter
                        if (dateKey.equals(selectedDateKey)) selected.add(row);
                        else others.add(row);
                    }

                    // Finale Liste: selected zuerst, dann others
                    List<EventRow> all = new ArrayList<>();
                    all.addAll(selected);
                    all.addAll(others);

                    // Adapter aktualisieren -> UI aktualisiert sich
                    adapter.setItems(all);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    /**
     * Öffnet einen Dialog, um für das aktuell ausgewählte Datum einen Eintrag hinzuzufügen.
     * - Text eingeben
     * - optional eine Person auswählen (Spinner)
     * - speichern -> Firestore /events
     */
    private void showAddEventDialog() {
        // Dialog-Layout laden
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_event, null, false);

        EditText et = content.findViewById(R.id.etEventText);
        android.widget.Spinner sp = content.findViewById(R.id.spPerson);

        // Spinner-Liste vorbereiten: zuerst Platzhalter
        List<PersonOption> persons = new ArrayList<>();
        persons.add(new PersonOption("", "— Person auswählen —"));

        android.widget.ArrayAdapter<PersonOption> spAdapter =
                new android.widget.ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, persons);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(spAdapter);

        // Personen aus Firestore laden und in Spinner anzeigen
        FirestorePaths.persons(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    persons.clear();
                    persons.add(new PersonOption("", "— Person auswählen —"));
                    for (QueryDocumentSnapshot d : qs) {
                        String name = d.getString("name");
                        if (name == null || name.isEmpty()) name = "(Ohne Name)";
                        persons.add(new PersonOption(d.getId(), name));
                    }
                    spAdapter.notifyDataSetChanged();
                });

        // Dialog bauen
        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag hinzufügen")
                .setView(content)
                .setPositiveButton("Speichern", (d, w) -> {
                    // Text auslesen
                    String text = et.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) return; // wenn leer, nichts speichern


                    PersonOption p = (PersonOption) sp.getSelectedItem();

                    // Daten für Firestore bauen
                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("dateKey", selectedDateKey); // Datum, zu dem der Eintrag gehört
                    data.put("text", text);
                    data.put("personId", p != null ? p.id : "");
                    data.put("personName", p != null ? p.name : "");
                    data.put("createdAt", Timestamp.now());

                    // Eintrag speichern, danach Liste + Punkte aktualisieren
                    FirestorePaths.events(uid).add(data)
                            .addOnSuccessListener(x -> {
                                loadEventsSorted();
                                loadMarkedDays();
                            });
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    //Dialog zum Bearbeiten eines vorhandenen Eintrags
    private void showEditEventDialog(EventRow row) {
        EditText et = new EditText(requireContext());
        et.setText(row.title);

        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag bearbeiten")
                .setView(et)

                // Löschen: Firestore-Dokument entfernen
                .setNeutralButton("Löschen", (d, w) -> {
                    FirestorePaths.event(uid, row.id)
                            .delete()
                            .addOnSuccessListener(x -> {
                                loadEventsSorted();
                                loadMarkedDays();
                            });
                })

                .setPositiveButton("Speichern", (d, w) -> {
                    String t = et.getText().toString().trim();
                    if (TextUtils.isEmpty(t)) return;

                    Map<String, Object> up = new HashMap<>();
                    up.put("text", t);
                    up.put("updatedAt", Timestamp.now());

                    FirestorePaths.event(uid, row.id)
                            .update(up)
                            .addOnSuccessListener(x -> {
                                loadEventsSorted();
                                loadMarkedDays();
                            });
                })

                .setNegativeButton("Abbrechen", null)
                .show();
    }

    //Lädt alle Events und markiert deren Tage im Kalender (Punkte)
    private void loadMarkedDays() {
        FirestorePaths.events(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    markedDays.clear();

                    for (QueryDocumentSnapshot d : qs) {
                        CalendarDay cd = parseCalendarDay(d.getString("dateKey"));
                        if (cd != null) markedDays.add(cd);
                    }

                    // Decorators neu setzen, damit Punkte aktualisiert werden
                    calendarView.removeDecorators();
                    calendarView.addDecorator(new DotDecorator(markedDays));
                });
    }

    /**
     * Hilfsmethode: macht aus "yyyy-MM-dd" ein CalendarDay-Objekt.
     * Gibt null zurück, wenn das Format nicht passt.
     */
    private CalendarDay parseCalendarDay(String dk) {
        try {
            String[] p = dk.split("-");
            return CalendarDay.from(
                    Integer.parseInt(p[0]),
                    Integer.parseInt(p[1]),
                    Integer.parseInt(p[2])
            );
        } catch (Exception e) {
            return null;
        }
    }

    //markiert alle Tage in "dates" mit einem Punkt
    private static class DotDecorator implements DayViewDecorator {
        private final Set<CalendarDay> dates;
        DotDecorator(Set<CalendarDay> d) { dates = d; }

        @Override public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override public void decorate(DayViewFacade view) {
            // Punkt (Dot) hinzufügen (Größe 8f, Farbe als Hex)
            view.addSpan(new DotSpan(8f, 0xFF7B61FF));
        }
    }


    //Datenobjekt für einen Eintrag in der Liste

    private static class EventRow {
        final String id, day, month, title, subtitle, dateKey;

        EventRow(String id, String day, String month, String title, String subtitle, String dateKey) {
            this.id = id;
            this.day = day;
            this.month = month;
            this.title = title;
            this.subtitle = subtitle;
            this.dateKey = dateKey;
        }
    }

    //Adapter: verwaltet die Liste und bindet Daten an ViewHolder
    private class EventAdapter extends RecyclerView.Adapter<EventVH> {
        private final List<EventRow> items = new ArrayList<>();

        void setItems(List<EventRow> l) {
            items.clear();
            items.addAll(l);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public EventVH onCreateViewHolder(@NonNull ViewGroup p, int v) {
            View view = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_calendar_entry, p, false);
            return new EventVH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull EventVH h, int i) {
            EventRow r = items.get(i);
            h.bind(r);

            // Klick auf Listeneintrag -> Bearbeiten/Löschen Dialog
            h.itemView.setOnClickListener(v -> showEditEventDialog(r));
        }

        @Override public int getItemCount() { return items.size(); }
    }

    private static class EventVH extends RecyclerView.ViewHolder {
        final TextView tvDay, tvMonth, tvTitle, tvSubtitle;

        EventVH(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
            tvMonth = v.findViewById(R.id.tvMonth);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvSubtitle = v.findViewById(R.id.tvSubtitle);
        }

        void bind(EventRow r) {
            tvDay.setText(r.day);
            tvMonth.setText(r.month);
            tvTitle.setText(r.title);


            if (r.subtitle != null && !r.subtitle.isEmpty()) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(r.subtitle);
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }
        }
    }

    private static class PersonOption {
        final String id, name;
        PersonOption(String i, String n) { id = i; name = n; }
        @Override public String toString() { return name; }
    }
}
