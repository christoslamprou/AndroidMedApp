package ui;

import android.os.Build;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mymedapp.R;
import data.PrescriptionWithTerm;

import java.time.LocalDate;

public class MainActivity extends AppCompatActivity {
    private MedViewModel vm;
    private PrescriptionAdapter adapter;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MedViewModel.class);
        adapter = new PrescriptionAdapter(this::openDetailsLater); // θα το υλοποιήσουμε στο Στάδιο 2

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setAdapter(adapter);

        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, ui.AddEditActivity.class));
        });

        // Παρατηρούμε τους Ενεργούς (θα ενημερώνονται όταν προσθέσουμε WorkManager)
        vm.getActive().observe(this, list -> adapter.submitList(list));
    }

    @androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.O)
    private void showQuickAddDialog() {
        android.widget.EditText shortName = new android.widget.EditText(this);
        shortName.setHint("Short name (π.χ. Amoxicillin 500mg)");

        android.widget.EditText location = new android.widget.EditText(this);
        location.setHint("Τοποθεσία ιατρού (π.χ. Ερμού 1, Αθήνα ή ΥΓΕΙΑ, Μαρούσι)");

        // Στοίχιση/περιθώρια
        androidx.appcompat.widget.LinearLayoutCompat layout =
                new androidx.appcompat.widget.LinearLayoutCompat(this);
        layout.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(shortName);
        layout.addView(location);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Νέα συνταγή")
                .setView(layout)
                .setPositiveButton("Αποθήκευση", (d, w) -> {
                    String s   = shortName.getText().toString().trim();
                    String loc = location.getText().toString().trim();

                    if (s.isEmpty()) {
                        android.widget.Toast.makeText(this, "Το Short name είναι υποχρεωτικό", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Προαιρετικό: αν ο χρήστης έβαλε μόνο όνομα χωρίς διεύθυνση/αριθμούς, πρόσθεσε default πόλη για να βοηθήσεις την αναζήτηση χάρτη
                    if (!loc.isEmpty() && !loc.contains(",") && !loc.matches(".*\\d.*")) {
                        loc = loc + ", Αθήνα"; // άλλαξέ το αν θέλεις άλλη default πόλη
                    }

                    vm.addDrug(
                            s,
                            "", // description
                            java.time.LocalDate.now(),
                            java.time.LocalDate.now().plusDays(30),
                            1,  // timeTermId = before-breakfast (προς το παρόν)
                            "", // doctorName (προαιρετικό)
                            loc // <-- αποθήκευση τοποθεσίας
                    );
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }


    private void openDetailsLater(data.PrescriptionWithTerm it) {
        android.content.Intent i = new android.content.Intent(this, ui.DetailsActivity.class);
        i.putExtra(DetailsActivity.EXTRA_UID, it.drug.uid);
        startActivity(i);
    }


    // Προαιρετικό: παράδειγμα delete-by-UID (B) με popup για affected rows
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showDeleteByUidDialog() {
        EditText uidEdit = new EditText(this);
        uidEdit.setHint("UID");
        uidEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("Διαγραφή με UID")
                .setView(uidEdit)
                .setPositiveButton("Διαγραφή", (d, w) -> {
                    int uid = Integer.parseInt(uidEdit.getText().toString());
                    vm.deleteByUid(uid, rows -> runOnUiThread(() ->
                            Toast.makeText(this, "Διαγράφηκαν " + rows + " γραμμές", Toast.LENGTH_SHORT).show()));
                })
                .setNegativeButton("Άκυρο", null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete_uid) {
            showDeleteByUidDialog();
            return true;

        } else if (id == R.id.action_recompute) {
            androidx.work.WorkManager.getInstance(this)
                    .enqueue(androidx.work.OneTimeWorkRequest.from(ui.RecomputeWorker.class));
            android.widget.Toast.makeText(this, "Επανυπολογισμός προγραμματίστηκε", android.widget.Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.action_export_html) {
            vm.exportActive(true, uri -> {
                if (uri == null) {
                    android.widget.Toast.makeText(this, "Αποτυχία export (Android < 10 θέλει άλλο χειρισμό)", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/html");
                i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(i, "Άνοιγμα εξαγωγής"));
            });
            return true;

        } else if (id == R.id.action_export_txt) {
            vm.exportActive(false, uri -> {
                if (uri == null) {
                    android.widget.Toast.makeText(this, "Αποτυχία export (Android < 10 θέλει άλλο χειρισμό)", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/plain");
                i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(i, "Άνοιγμα εξαγωγής"));
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




}
