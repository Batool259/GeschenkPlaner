package com.example.geschenkplaner;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalendarFragment extends Fragment {

    private CalendarView calendarView;
    private TextView tvDate, tvList;
    private EditText etEvent;
    private Button btnSave;

    private FirebaseFirestore db;
    private String uid;
    private String dateKey;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

        calendarView = v.findViewById(R.id.calendarView);
        tvDate = v.findViewById(R.id.tvDate);
        etEvent = v.findViewById(R.id.etEvent);
        btnSave = v.findViewById(R.id.btnSave);
        tvList = v.findViewById(R.id.tvList);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Bitte einloggen", Toast.LENGTH_SHORT).show();
            return v;
        }

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        // Startdatum = heute
        dateKey = sdf.format(Calendar.getInstance().getTime());
        tvDate.setText("Datum: " + dateKey);
        loadEvents();

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar c = Calendar.getInstance();
            c.set(year, month, dayOfMonth);
            dateKey = sdf.format(c.getTime());
            tvDate.setText("Datum: " + dateKey);
            loadEvents();
        });

        btnSave.setOnClickListener(v1 -> saveEvent());

        return v;
    }

    private void saveEvent() {
        String text = etEvent.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(getContext(), "Text eingeben", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("dateKey", dateKey);
        data.put("text", text);
        data.put("createdAt", Timestamp.now());

        db.collection("events")
                .add(data)
                .addOnSuccessListener(r -> {
                    etEvent.setText("");
                    loadEvents();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Fehler: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void loadEvents() {
        db.collection("events")
                .whereEqualTo("uid", uid)
                .whereEqualTo("dateKey", dateKey)
                .get()
                .addOnSuccessListener(qs -> {
                    if (qs.isEmpty()) {
                        tvList.setText("Keine Einträge");
                        return;
                    }
                    StringBuilder sb = new StringBuilder();
                    for (var doc : qs.getDocuments()) {
                        sb.append("• ").append(doc.getString("text")).append("\n");
                    }
                    tvList.setText(sb.toString());
                });
    }
}