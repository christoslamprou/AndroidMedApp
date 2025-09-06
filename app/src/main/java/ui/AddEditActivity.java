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
public class  AddEditActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid"; // -1 => add, otherwise edit

    private MedViewModel vm;
    private int editUid = -1;

    // UI references
    private android.widget.EditText etShort, etDesc, etDocName, etDocLoc;
    private android.widget.TextView tvStart, tvEnd;
    private Spinner spTerm;
    private View btnSave;

    // Local state for the form
    private LocalDate start = LocalDate.now();
    private LocalDate end   = LocalDate.now().plusDays(30);
    private List<TimeTerm> terms = new ArrayList<>();
    private int selectedTermId = 1;
    private PrescriptionDrug current; // when editing, holds the current row

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit);

        editUid = getIntent().getIntExtra(EXTRA_UID, -1);

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        etShort   = findViewById(R.id.etShort);
        etDesc    = findViewById(R.id.etDesc);
        etDocName = findViewById(R.id.etDocName);
        etDocLoc  = findViewById(R.id.etDocLoc);
        tvStart   = findViewById(R.id.tvStart);
        tvEnd     = findViewById(R.id.tvEnd);
        spTerm    = findViewById(R.id.spTerm);
        btnSave   = findViewById(R.id.btnSave);

        // Observe time terms for the spinner
        vm.getTimeTerms().observe(this, list -> {
            terms = list;
            ArrayAdapter<String> ad = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_dropdown_item,
                    mapTermsToLabels(list) // display the term code
            );
            spTerm.setAdapter(ad);
            // If we are editing, select the current item’s term
            if (current != null) {
                int idx = indexOfTermId(current.timeTermId);
                if (idx >= 0) spTerm.setSelection(idx);
            }
        });

        // Initial date rendering
        renderDates();

        // Date pickers
        tvStart.setOnClickListener(v -> pickDate(true));
        tvEnd.setOnClickListener(v -> pickDate(false));

        // Save handler
        btnSave.setOnClickListener(v -> onSave());

        // If editing: load and prefill the form
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

        // Basic validation
        if (s.isEmpty()) { Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show(); return; }
        if (end.isBefore(start)) { Toast.makeText(this, "End date cannot be before start date", Toast.LENGTH_SHORT).show(); return; }

        // Resolve selected term id (fallback to 1 if something is off)
        int pos = spTerm.getSelectedItemPosition();
        selectedTermId = (pos >= 0 && pos < terms.size()) ? terms.get(pos).id : 1;

        if (editUid > 0) {
            // Update existing row
            vm.saveEdit(editUid, s, ds, start, end, selectedTermId, dn, dl, rows -> {
                if (rows != null && rows > 0) {
                    Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Insert new row
            vm.saveNew(s, ds, start, end, selectedTermId, dn, dl, id -> {
                if (id != null && id > 0) {
                    Toast.makeText(this, "Saved (UID " + id + ")", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ---- helpers ----

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void pickDate(boolean isStart) {
        // Opens a DatePickerDialog and updates the local start/end date
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
        // Display ISO-8601 dates (yyyy-MM-dd)
        tvStart.setText(start.toString());
        tvEnd.setText(end.toString());
    }

    private List<String> mapTermsToLabels(List<TimeTerm> list) {
        // Map TimeTerm to a simple label for the spinner
        List<String> out = new ArrayList<>();
        for (TimeTerm t : list) out.add(t.code);
        return out;
    }

    private int indexOfTermId(int termId) {
        // Find the spinner index for the given term id
        for (int i = 0; i < terms.size(); i++) if (terms.get(i).id == termId) return i;
        return -1;
    }
}
