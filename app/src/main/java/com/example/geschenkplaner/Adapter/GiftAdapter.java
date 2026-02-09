package com.example.geschenkplaner.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.R;
import com.example.geschenkplaner.activity.GiftDetailActivity;
import com.example.geschenkplaner.model.GiftItem;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.EventVH> {

    private final List<GiftItem> items = new ArrayList<>();
    private String personId;

    /**
     * giftId -> dateKey ("yyyy-MM-dd")
     * weil GiftItem kein Datum hat
     */
    private final Map<String, String> giftDateKey = new HashMap<>();

    /**
     * giftId -> "Lisa Müller, Tom Bauer"
     * weil GiftItem keine Personenliste hat
     */
    private final Map<String, String> giftPeopleText = new HashMap<>();

    // Public API
    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public void setItems(List<GiftItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void setGiftDateKey(Map<String, String> map) {
        giftDateKey.clear();
        if (map != null) giftDateKey.putAll(map);
        notifyDataSetChanged();
    }

    public void setGiftPeopleText(Map<String, String> map) {
        giftPeopleText.clear();
        if (map != null) giftPeopleText.putAll(map);
        notifyDataSetChanged();
    }

    // Adapter
    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_calendar_entry, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH holder, int position) {
        GiftItem g = items.get(position);
        if (g == null) return;

        // -------- Titel --------
        String title = (g.title != null && !g.title.trim().isEmpty())
                ? g.title.trim()
                : "—";

        // -------- Subtitle (mehrere Personen) --------
        String subtitle = "";
        if (g.id != null && giftPeopleText.containsKey(g.id)) {
            subtitle = giftPeopleText.get(g.id);
            if (subtitle == null) subtitle = "";
        }

        // -------- Datum links (Tag + Monat) --------
        String day = "—";
        String monthShort = "—";

        String dateKey = (g.id != null) ? giftDateKey.get(g.id) : null; // "yyyy-MM-dd"
        if (dateKey != null && dateKey.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] p = dateKey.split("-");
            day = p[2];

            int month = Integer.parseInt(p[1]); // 1..12
            String[] months = new DateFormatSymbols(Locale.GERMAN).getShortMonths();
            monthShort = months[month - 1];
            if (monthShort != null && !monthShort.isEmpty()) {
                monthShort = monthShort.substring(0, 1).toUpperCase() + monthShort.substring(1);
            }
        }

        holder.bind(day, monthShort, title, subtitle);

        // -------- Klick -> Detail --------
        holder.itemView.setOnClickListener(v -> {
            if (g.id == null) return;

            Intent i = new Intent(v.getContext(), GiftDetailActivity.class);
            if (personId != null) {
                i.putExtra(GiftDetailActivity.EXTRA_PERSON_ID, personId);
            }
            i.putExtra(GiftDetailActivity.EXTRA_GIFT_ID, g.id);
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder
    static class EventVH extends RecyclerView.ViewHolder {

        final TextView tvDay;
        final TextView tvMonth;
        final TextView tvTitle;
        final TextView tvSubtitle;

        EventVH(@NonNull View itemView) {
            super(itemView);
            tvDay = itemView.findViewById(R.id.tvDay);
            tvMonth = itemView.findViewById(R.id.tvMonth);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
        }

        void bind(String day, String month, String title, String subtitle) {
            tvDay.setText(day != null && !day.isEmpty() ? day : "—");
            tvMonth.setText(month != null && !month.isEmpty() ? month : "—");

            tvTitle.setText(
                    title != null && !title.trim().isEmpty()
                            ? title.trim()
                            : "—"
            );

            if (subtitle != null && !subtitle.trim().isEmpty()) {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(subtitle.trim());
            } else {
                tvSubtitle.setVisibility(View.GONE);
            }
        }
    }

    // Helper: mehrere Namen zusammenführen
    public static String joinNames(List<String> names) {
        if (names == null) return "";

        Set<String> dedup = new HashSet<>();
        List<String> out = new ArrayList<>();

        for (String n : names) {
            if (n == null) continue;
            n = n.trim();
            if (n.isEmpty()) continue;
            if (dedup.add(n)) out.add(n);
        }
        return String.join(", ", out);
    }
}
