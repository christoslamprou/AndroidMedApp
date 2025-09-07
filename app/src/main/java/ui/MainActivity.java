package ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.mymedapp.R;

import data.PrescriptionWithTerm;

/**
 * Main screen that shows ACTIVE prescriptions and core actions.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "PROV"; // Log tag for provider demo

    private MedViewModel vm;
    private PrescriptionAdapter adapter;

    // Set up list, adapter, FAB, and LiveData observer
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        // Adapter opens Details on item tap
        adapter = new PrescriptionAdapter(this::openDetails);

        RecyclerView recycler = findViewById(R.id.recycler);
        recycler.setAdapter(adapter);

        // FAB opens Add/Edit for a new item
        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(this, AddEditActivity.class)));

        // Observe ACTIVE items; UI updates automatically
        vm.getActive().observe(this, list -> adapter.submitList(list));
    }

    // Open Details for the selected item
    private void openDetails(PrescriptionWithTerm it) {
        Intent i = new Intent(this, DetailsActivity.class);
        i.putExtra(DetailsActivity.EXTRA_UID, it.drug.uid);
        startActivity(i);
    }

    // Delete-by-UID dialog (shows affected rows)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showDeleteByUidDialog() {
        EditText uidEdit = new EditText(this);
        uidEdit.setHint("UID");
        uidEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(this)
                .setTitle("Delete by UID")
                .setView(uidEdit)
                .setPositiveButton("Delete", (d, w) -> {
                    String s = uidEdit.getText().toString().trim();
                    try {
                        int uid = Integer.parseInt(s);
                        vm.deleteByUid(uid, rows -> runOnUiThread(() ->
                                Toast.makeText(this, "Deleted " + rows + " row(s)", Toast.LENGTH_SHORT).show()));
                    } catch (NumberFormatException ex) {
                        Toast.makeText(this, "Please enter a valid UID number", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Inflate overflow menu
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Menu actions: delete-by-UID, recompute, export, provider demo
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_delete_uid) {
            showDeleteByUidDialog();
            return true;

        } else if (id == R.id.action_recompute) {
            WorkManager.getInstance(this)
                    .enqueue(OneTimeWorkRequest.from(RecomputeWorker.class));
            Toast.makeText(this, "Recompute scheduled", Toast.LENGTH_SHORT).show();
            return true;

        } else if (id == R.id.action_export_html) {
            vm.exportActive(true, uri -> {
                if (uri == null) {
                    Toast.makeText(this, "Export failed (Android < 10 needs a different flow)", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/html");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Open export with"));
            });
            return true;

        } else if (id == R.id.action_export_txt) {
            vm.exportActive(false, uri -> {
                if (uri == null) {
                    Toast.makeText(this, "Export failed (Android < 10 needs a different flow)", Toast.LENGTH_LONG).show();
                    return;
                }
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(uri, "text/plain");
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivity(Intent.createChooser(i, "Open export with"));
            });
            return true;

        } else if (id == R.id.action_provider_demo) {
            runProviderDemo();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // In-app ContentProvider demo: INSERT → verify → toast (keeps the row)
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void runProviderDemo() {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                java.time.LocalDate now = java.time.LocalDate.now();
                android.content.ContentValues v = new android.content.ContentValues();
                v.put(provider.MedContract.Prescriptions.COL_SHORT, "ProviderTest 1");
                v.put(provider.MedContract.Prescriptions.COL_START, now.toEpochDay());
                v.put(provider.MedContract.Prescriptions.COL_END,   now.plusDays(7).toEpochDay());
                v.put(provider.MedContract.Prescriptions.COL_TERM,  1);            // requires seeded term id=1
                v.put(provider.MedContract.Prescriptions.COL_ACTIVE, 1);           // show in ACTIVE list
                v.put(provider.MedContract.Prescriptions.COL_TODAY,  0);
                v.put(provider.MedContract.Prescriptions.COL_LAST,   (Long) null);

                android.net.Uri pUri = provider.MedContract.Prescriptions.CONTENT_URI;
                android.net.Uri rowUri = getContentResolver().insert(pUri, v);
                if (rowUri == null) throw new IllegalStateException("Insert failed (rowUri == null)");

                long uid = android.content.ContentUris.parseId(rowUri);
                android.util.Log.d(TAG, "insert -> " + rowUri);

                // Optional: trigger recompute
                androidx.work.WorkManager.getInstance(this)
                        .enqueue(androidx.work.OneTimeWorkRequest.from(ui.RecomputeWorker.class));

                // Verify the inserted row
                try (android.database.Cursor c = getContentResolver().query(
                        pUri, null, "uid=?", new String[]{ String.valueOf(uid) }, null)) {
                    if (c != null && c.moveToFirst()) {
                        int active = c.getInt(c.getColumnIndexOrThrow("isActive"));
                        android.util.Log.d(TAG, "uid=" + uid + " isActive=" + active);
                    }
                }

                // Keep the row so it appears in the list
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Inserted via Provider (UID " + uid + ")",
                                Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                android.util.Log.e(TAG, "error", e);
                runOnUiThread(() ->
                        Toast.makeText(this,
                                "Provider demo failed: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}
