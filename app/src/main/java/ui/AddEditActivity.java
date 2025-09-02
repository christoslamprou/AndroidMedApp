package ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mymedapp.R;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import data.PrescriptionDrug;
import data.TimeTerm;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid"; // -1 => add, otherwise edit

    private MedViewModel vm;
    private int editUid = -1;

    // UI refs
    private android.widget.EditText etShort, etDesc, etDocName, etDocLoc;
    private android.widget.TextView tvStart, tvEnd;
    private Spinner spTerm;
    private View btnSave;

    // state
    private LocalDate start = LocalDate.now();
    private LocalDate end   = LocalDate.now().plusDays(30);
    private List<TimeTerm> terms = new ArrayList<>();
    private int selectedTermId = 1;
    private PrescriptionDrug current; // όταν είμαστε σε edit

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);



        editUid = getIntent().getIntExtra(EXTRA_UID, -1);

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        etShort = findViewById(R.id.etShort);
        etDesc  = findViewById(R.id.etDesc);
        etDocName = findViewById(R.id.etDocName);
        etDocLoc  = findViewById(R.id.etDocLoc);
        tvStart = findViewById(R.id.tvStart);
        tvEnd   = findViewById(R.id.tvEnd);
        spTerm  = findViewById(R.id.spTerm);
        btnSave = findViewById(R.id.btnSave);

        // Term spinner: παρακολούθηση όρων από DB
        vm.getTimeTerms().observe(this, list -> {
            terms = list;
            ArrayAdapter<String> ad = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    mapTermsToLabels(list));
            spTerm.setAdapter(ad);
            // αν είμαστε σε edit, θέλουμε να επιλεγεί ο σωστός όρος
            if (current != null) {
                int idx = indexOfTermId(current.timeTermId);
                if (idx >= 0) spTerm.setSelection(idx);
            }
        });

        // ημερομηνίες αρχικές
        renderDates();

        tvStart.setOnClickListener(v -> pickDate(true));
        tvEnd.setOnClickListener(v -> pickDate(false));

        btnSave.setOnClickListener(v -> onSave());

        // Αν είμαστε σε edit, γέμισε τη φόρμα
        if (editUid > 0) {
            vm.observeDrug(editUid).observe(this, d -> {
                if (d == null) return;
                current = d;
                etShort.setText(d.shortName);
                etDesc.setText(d.description);
                etDocName.setText(d.doctorName);
                etDocLoc.setText(d.doctorLocation);

                start = LocalDate.ofEpochDay(d.startDateEpoch);
                end   = LocalDate.ofEpochDay(d.endDateEpoch);
                renderDates();

                int idx = indexOfTermId(d.timeTermId);
                if (idx >= 0 && spTerm.getAdapter() != null) spTerm.setSelection(idx);
            });
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void onSave() {
        String s  = etShort.getText().toString().trim();
        String ds = etDesc.getText().toString().trim();
        String dn = etDocName.getText().toString().trim();
        String dl = etDocLoc.getText().toString().trim();

        if (s.isEmpty()) { Toast.makeText(this, "Το όνομα είναι υποχρεωτικό", Toast.LENGTH_SHORT).show(); return; }
        if (end.isBefore(start)) { Toast.makeText(this, "Η ημερομηνία λήξης δεν μπορεί να είναι πριν την έναρξη", Toast.LENGTH_SHORT).show(); return; }

        int pos = spTerm.getSelectedItemPosition();
        selectedTermId = (pos >= 0 && pos < terms.size()) ? terms.get(pos).id : 1;

        if (editUid > 0) {
            vm.saveEdit(editUid, s, ds, start, end, selectedTermId, dn, dl, rows -> {
                if (rows != null && rows > 0) {
                    Toast.makeText(this, "Ενημερώθηκε", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Αποτυχία ενημέρωσης", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            vm.saveNew(s, ds, start, end, selectedTermId, dn, dl, id -> {
                if (id != null && id > 0) {
                    Toast.makeText(this, "Αποθηκεύτηκε (UID " + id + ")", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Αποτυχία αποθήκευσης", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ---- helpers ----
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pickDate(boolean isStart) {
        LocalDate d = isStart ? start : end;
        android.app.DatePickerDialog dp = new android.app.DatePickerDialog(
                this,
                (view, y, m, day) -> {
                    LocalDate nd = LocalDate.of(y, m + 1, day);
                    if (isStart) start = nd; else end = nd;
                    renderDates();
                },
                d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth());
        dp.show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void renderDates() {
        tvStart.setText(start.toString());
        tvEnd.setText(end.toString());
    }

    private List<String> mapTermsToLabels(List<TimeTerm> list) {
        List<String> out = new ArrayList<>();
        for (TimeTerm t : list) out.add(t.code); // μπορείς να τοπικοποιήσεις αν θέλεις
        return out;
    }

    private int indexOfTermId(int termId) {
        for (int i = 0; i < terms.size(); i++) if (terms.get(i).id == termId) return i;
        return -1;
    }
}
