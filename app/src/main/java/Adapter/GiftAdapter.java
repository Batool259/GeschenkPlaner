package Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geschenkplaner.GiftDetailActivity;
import model.GiftItem;
import com.example.geschenkplaner.R;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.VH> {

    private final List<GiftItem> items = new ArrayList<>();
    private String personId; // ✅ wichtig

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public void setItems(List<GiftItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gift, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        GiftItem g = items.get(position);

        String title = (g != null && g.title != null && !g.title.trim().isEmpty())
                ? g.title.trim()
                : "—";
        h.tvTitle.setText(title);

        boolean bought = g != null && g.bought;
        h.tvNote.setText(bought ? "✅ Gekauft" : "🟡 Geplant");

        if (g != null && g.price != null) {
            h.chipPrice.setText(String.format(Locale.getDefault(), "€ %.2f", g.price));
        } else {
            h.chipPrice.setText("€ —");
        }

        h.itemView.setOnClickListener(v -> {
            if (g == null || g.id == null) return;

            Intent i = new Intent(v.getContext(), GiftDetailActivity.class);
            i.putExtra(GiftDetailActivity.EXTRA_PERSON_ID, personId); // ✅ dazu
            i.putExtra(GiftDetailActivity.EXTRA_GIFT_ID, g.id);
            v.getContext().startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvNote;
        Chip chipPrice;
        ImageView imgGift;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGiftTitle);
            tvNote = itemView.findViewById(R.id.tvGiftNote);
            chipPrice = itemView.findViewById(R.id.chipPrice);
            imgGift = itemView.findViewById(R.id.imgGift);
        }
    }
}
