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

    // Entry point: sets up list, adapter, FAB, and LiveData observers
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        // Adapter opens details on item click
        adapter = new PrescriptionAdapter(this::openDetailsLater);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setAdapter(adapter);

        // FAB: open the Add/Edit form for a new item
        findViewById(R.id.fabAdd).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, ui.AddEditActivity.class));
        });

        // Observe active items; list updates automatically via LiveData
        vm.getActive().observe(this, list -> adapter.submitList(list));
    }

    // Quick-add dialog used earlier; kept for convenience/testing
    @androidx.annotation.RequiresApi(api = android.os.Build.VERSION_CODES.O)
    private void showQuickAddDialog() {
        android.widget.EditText shortName = new android.widget.EditText(this);
        shortName.setHint("Short name (e.g., Amoxicillin 500mg)");

        android.widget.EditText location = new android.widget.EditText(this);
        location.setHint("Doctor location (e.g., Ermou 1, Athens or HYGEIA, Marousi)");

        // Simple vertical container with padding
        androidx.appcompat.widget.LinearLayoutCompat layout =
                new androidx.appcompat.widget.LinearLayoutCompat(this);
        layout.setOrientation(androidx.appcompat.widget.LinearLayoutCompat.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.addView(shortName);
        layout.addView(location);

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("New prescription")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    String s   = shortName.getText().toString().trim();
                    String loc = location.getText().toString().trim();

                    if (s.isEmpty()) {
                        android.widget.Toast.makeText(this, "Short name is required", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // If user typed only a name append a default city to help map search
                    if (!loc.isEmpty() && !loc.contains(",") && !loc.matches(".*\\d.*")) {
                        loc = loc + ", Athens"; // change default city if you prefer
                    }

                    vm.addDrug(
                            s,
                            "", // description
                            java.time.LocalDate.now(),
                            java.time.LocalDate.now().plusDays(30),
                            1,  // timeTermId = before-breakfast
                            "", // doctorName
                            loc // save location
                    );
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Open details screen for the selected item
    private void openDetailsLater(data.PrescriptionWithTerm it) {
        android.content.Intent i = new android.content.Intent(this, ui.DetailsActivity.class);
        i.putExtra(DetailsActivity.EXTRA_UID, it.drug.uid);
        startActivity(i);
    }

    // Delete-by-UID dialog; shows affected rows in a Toast
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showDeleteByUidDialog() {
        EditText uidEdit = new EditText(this);
        uidEdit.setHint("UID");
        uidEdit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this)
                .setTitle("Delete by UID")
                .setView(uidEdit)
                .setPositiveButton("Delete", (d, w) -> {
                    int uid = Integer.parseInt(uidEdit.getText().toString());
                    vm.deleteByUid(uid, rows -> runOnUiThread(() ->
                            Toast.makeText(this, "Deleted " + rows + " row(s)", Toast.LENGTH_SHORT).show()));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Inflate the main menu (⋮)
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu actions: recompute, export, delete-by-UID
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
            android.widget.Toast.makeText(this, "Recompute scheduled", android.widget.Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.action_export_html) {
            vm.exportActive(true, uri -> {
                if (uri == null) {
                    android.widget.Toast.makeText(this, "Export failed (Android < 10 needs a different flow)", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/html");
                i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(i, "Open export with"));
            });
            return true;

        } else if (id == R.id.action_export_txt) {
            vm.exportActive(false, uri -> {
                if (uri == null) {
                    android.widget.Toast.makeText(this, "Export failed (Android < 10 needs a different flow)", android.widget.Toast.LENGTH_LONG).show();
                    return;
                }
                android.content.Intent i = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/plain");
                i.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(android.content.Intent.createChooser(i, "Open export with"));
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
