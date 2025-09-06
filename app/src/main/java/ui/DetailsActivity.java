package ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mymedapp.R;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import data.PrescriptionWithTerm;

@RequiresApi(api = Build.VERSION_CODES.O)
public class DetailsActivity extends AppCompatActivity {

    public static final String EXTRA_UID = "uid";

    private MedViewModel vm;
    private int uid;

    // UI refs
    private TextView tvShort, tvDesc, tvTerm, tvRange, tvActive, tvToday, tvLast;
    private Button btnReceive, btnMap, btnEdit;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        uid = getIntent().getIntExtra(EXTRA_UID, -1);
        if (uid <= 0) { finish(); return; }

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        // Find views
        tvShort  = findViewById(R.id.tvShort);
        tvDesc   = findViewById(R.id.tvDesc);
        tvTerm   = findViewById(R.id.tvTerm);
        tvRange  = findViewById(R.id.tvRange);
        tvActive = findViewById(R.id.tvActive);
        tvToday  = findViewById(R.id.tvToday);
        tvLast   = findViewById(R.id.tvLast);
        btnReceive = findViewById(R.id.btnReceive);
        btnMap     = findViewById(R.id.btnMap);
        btnEdit    = findViewById(R.id.btnEdit);

        // Observe a single item and bind it to the UI
        vm.getById(uid).observe(this, this::bind);

        // Open edit form
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, uid); // pass UID for edit
            startActivity(i);
        });

        // Mark "received today"
        btnReceive.setOnClickListener(v ->
                vm.receivedToday(uid, rows -> {
                    if (rows != null && rows > 0) {
                        Toast.makeText(this, "Marked as received today", Toast.LENGTH_SHORT).show();
                        // UI refresh comes from LiveData observer
                    } else {
                        Toast.makeText(this, "No update made", Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void bind(PrescriptionWithTerm it) {
        if (it == null) return;

        // Basic fields
        tvShort.setText(it.drug.shortName);
        tvDesc.setText(emptyToDash(it.drug.description));
        tvTerm.setText("Timing: " + emptyToDash(it.termCode));

        // Dates
        String from = formatEpochDay(it.drug.startDateEpoch);
        String to   = formatEpochDay(it.drug.endDateEpoch);
        tvRange.setText("Duration: " + from + " → " + to);

        // Flags
        tvActive.setText("Active: " + (it.drug.isActive ? "YES" : "NO"));
        tvToday.setText("Received today: " + (it.drug.hasReceivedToday ? "YES" : "NO"));
        tvLast.setText("Last received: " +
                (it.drug.lastDateReceivedEpoch == null ? "-" : formatEpochDay(it.drug.lastDateReceivedEpoch)));

        // Map button (enabled only if location exists)
        String loc = it.drug.doctorLocation == null ? "" : it.drug.doctorLocation.trim();
        btnMap.setEnabled(!loc.isEmpty());
        btnMap.setOnClickListener(v -> openMapWithGeocoderOrSearch(loc));

        // (Optional duplicate safety: also set edit here if you prefer binding-time wiring)
        Button btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, it.drug.uid); // pass UID
            startActivity(i);
        });
    }

    // ------------ Helpers ------------

    // Return "-" if string is null/blank
    private String emptyToDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    // Format epoch-day to yyyy-MM-dd
    @RequiresApi(api = Build.VERSION_CODES.O)
    private String formatEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_FMT);
    }

    // Try Geocoder for exact pin; fallback to geo search
    private void openMapWithGeocoderOrSearch(String rawLocation) {
        if (rawLocation == null) return;
        final String query = rawLocation.trim();
        if (query.isEmpty()) return;

        new Thread(() -> {
            try {
                android.location.Geocoder g = new android.location.Geocoder(this, Locale.getDefault());
                List<android.location.Address> res = g.getFromLocationName(query, 1);
                runOnUiThread(() -> {
                    if (res != null && !res.isEmpty()) {
                        double lat = res.get(0).getLatitude();
                        double lng = res.get(0).getLongitude();
                        String label = query;
                        Uri uri = Uri.parse("geo:" + lat + "," + lng + "?q=" +
                                Uri.encode(lat + "," + lng + "(" + label + ")"));
                        startMapIntent(uri);
                    } else {
                        openMapSearch(query); // fallback: text search
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> openMapSearch(query));
            }
        }).start();
    }

    // Build a geo search URI; if the query is too generic, append city for better results
    private void openMapSearch(String q) {
        String query = q;
        if (!query.contains(",") && !query.matches(".*\\d.*")) {
            query = query + ", Athens";
        }
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        startMapIntent(uri);
    }

    // Prefer Google Maps if installed; otherwise any maps app
    private void startMapIntent(Uri uri) {
        Intent map = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager pm = getPackageManager();

        map.setPackage("com.google.android.apps.maps");
        if (map.resolveActivity(pm) == null) {
            map.setPackage(null); // fallback to any app
        }

        if (map.resolveActivity(pm) != null) {
            startActivity(map);
        } else {
            Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show();
        }
    }

    // Details screen overflow menu with "Edit"
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Intent i = new Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, uid);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
