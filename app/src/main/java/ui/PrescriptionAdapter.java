package ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mymedapp.R;
import data.PrescriptionWithTerm;

public class PrescriptionAdapter extends ListAdapter<PrescriptionWithTerm, PrescriptionAdapter.VH> {

    public interface OnClick { void onClick(PrescriptionWithTerm item); }
    private final OnClick onClick;
    public PrescriptionAdapter(OnClick onClick) { super(DIFF); this.onClick = onClick; }

    private static final DiffUtil.ItemCallback<PrescriptionWithTerm> DIFF =
            new DiffUtil.ItemCallback<PrescriptionWithTerm>() {
                @Override public boolean areItemsTheSame(@NonNull PrescriptionWithTerm o, @NonNull PrescriptionWithTerm n) {
                    return o.drug.uid == n.drug.uid;
                }
                @Override public boolean areContentsTheSame(@NonNull PrescriptionWithTerm o, @NonNull PrescriptionWithTerm n) {
                    return o.drug.shortName.equals(n.drug.shortName)
                            && o.termCode.equals(n.termCode)
                            && o.drug.isActive == n.drug.isActive;
                }
            };

    static class VH extends RecyclerView.ViewHolder {
        TextView uid, shortName, term;
        VH(@NonNull View v) {
            super(v);
            uid = v.findViewById(R.id.txtUid);
            shortName = v.findViewById(R.id.txtShort);
            term = v.findViewById(R.id.txtTerm);
        }
    }

    @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_prescription, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        PrescriptionWithTerm it = getItem(position);
        h.uid.setText("UID: " + it.drug.uid);
        h.shortName.setText(it.drug.shortName);
        h.term.setText("Time: " + it.termCode);
        h.itemView.setOnClickListener(v -> { if (onClick != null) onClick.onClick(it); });
    }
}
