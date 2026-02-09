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

    private MaterialCalendarView calendarView;
    private TextView tvToggleCalendar;
    private RecyclerView rvEvents;

    private EventAdapter adapter;

    private String uid;
    private String selectedDateKey;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private final Set<CalendarDay> markedDays = new HashSet<>();

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

        calendarView = v.findViewById(R.id.calendarView);
        tvToggleCalendar = v.findViewById(R.id.tvToggleCalendar);
        rvEvents = v.findViewById(R.id.rvEvents);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bitte einloggen", Toast.LENGTH_SHORT).show();
            return;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        adapter = new EventAdapter();
        rvEvents.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvEvents.setAdapter(adapter);

        Calendar today = Calendar.getInstance();
        selectedDateKey = sdf.format(today.getTime());

        calendarView.setSelectedDate(CalendarDay.today());
        calendarView.setCurrentDate(CalendarDay.today());

        loadEventsSorted();
        loadMarkedDays();

        // ✅ Toggle: Kalender aus-/einblenden
        tvToggleCalendar.setOnClickListener(x -> {
            if (calendarView.getVisibility() == View.VISIBLE) {
                calendarView.setVisibility(View.GONE);
                tvToggleCalendar.setText("Kalender anzeigen");
            } else {
                calendarView.setVisibility(View.VISIBLE);
                tvToggleCalendar.setText("Kalender ausblenden");
            }
        });

        // Klick auf Datum -> neu sortieren + Add Dialog
        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            Calendar c = Calendar.getInstance();
            c.set(date.getYear(), date.getMonth() - 1, date.getDay());
            selectedDateKey = sdf.format(c.getTime());

            loadEventsSorted();
            showAddEventDialog();
        });
    }

    // Events laden (selected oben, Rest darunter)
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

                        String title = d.getString("text");
                        if (title == null || title.trim().isEmpty()) title = "—";

                        String subtitle = d.getString("personName");
                        if (subtitle == null) subtitle = "";

                        String day = "—";
                        String monthShort = "—";

                        if (dateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            String[] p = dateKey.split("-");
                            day = p[2];

                            int month = Integer.parseInt(p[1]);
                            String[] months = new DateFormatSymbols(Locale.GERMAN).getShortMonths();
                            monthShort = months[month - 1];
                            if (monthShort != null && !monthShort.isEmpty()) {
                                monthShort = monthShort.substring(0, 1).toUpperCase() + monthShort.substring(1);
                            }
                        }

                        EventRow row = new EventRow(
                                d.getId(),
                                day,
                                monthShort,
                                title,
                                subtitle,
                                dateKey
                        );

                        if (dateKey.equals(selectedDateKey)) selected.add(row);
                        else others.add(row);
                    }

                    List<EventRow> all = new ArrayList<>();
                    all.addAll(selected);
                    all.addAll(others);

                    adapter.setItems(all);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Add / Edit / Delete
    private void showAddEventDialog() {
        View content = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_event, null, false);

        EditText et = content.findViewById(R.id.etEventText);
        android.widget.Spinner sp = content.findViewById(R.id.spPerson);

        List<PersonOption> persons = new ArrayList<>();
        persons.add(new PersonOption("", "— Person auswählen —"));

        android.widget.ArrayAdapter<PersonOption> spAdapter =
                new android.widget.ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_item, persons);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(spAdapter);

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

        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag hinzufügen")
                .setView(content)
                .setPositiveButton("Speichern", (d, w) -> {
                    String text = et.getText().toString().trim();
                    if (TextUtils.isEmpty(text)) return;

                    PersonOption p = (PersonOption) sp.getSelectedItem();

                    Map<String, Object> data = new HashMap<>();
                    data.put("uid", uid);
                    data.put("dateKey", selectedDateKey);
                    data.put("text", text);
                    data.put("personId", p != null ? p.id : "");
                    data.put("personName", p != null ? p.name : "");
                    data.put("createdAt", Timestamp.now());

                    FirestorePaths.events(uid).add(data)
                            .addOnSuccessListener(x -> {
                                loadEventsSorted();
                                loadMarkedDays();
                            });
                })
                .setNegativeButton("Abbrechen", null)
                .show();
    }

    private void showEditEventDialog(EventRow row) {
        EditText et = new EditText(requireContext());
        et.setText(row.title);

        new AlertDialog.Builder(requireContext())
                .setTitle("Eintrag bearbeiten")
                .setView(et)

                // ✅ Löschen direkt im Bearbeiten-Dialog (kein Longpress nötig)
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

    // Dots
    private void loadMarkedDays() {
        FirestorePaths.events(uid)
                .get()
                .addOnSuccessListener(qs -> {
                    markedDays.clear();
                    for (QueryDocumentSnapshot d : qs) {
                        CalendarDay cd = parseCalendarDay(d.getString("dateKey"));
                        if (cd != null) markedDays.add(cd);
                    }
                    calendarView.removeDecorators();
                    calendarView.addDecorator(new DotDecorator(markedDays));
                });
    }

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

    private static class DotDecorator implements DayViewDecorator {
        private final Set<CalendarDay> dates;
        DotDecorator(Set<CalendarDay> d) { dates = d; }

        @Override public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(8f, 0xFF7B61FF));
        }
    }

    // RecyclerView
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
