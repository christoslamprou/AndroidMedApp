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

    private TextView tvShort, tvDesc, tvTerm, tvRange, tvActive, tvToday, tvLast;
    private Button btnReceive, btnMap, btnEdit;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault());

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        uid = getIntent().getIntExtra(EXTRA_UID, -1);
        if (uid <= 0) { finish(); return; }

        vm = new ViewModelProvider(this).get(MedViewModel.class);

        tvShort = findViewById(R.id.tvShort);
        tvDesc  = findViewById(R.id.tvDesc);
        tvTerm  = findViewById(R.id.tvTerm);
        tvRange = findViewById(R.id.tvRange);
        tvActive= findViewById(R.id.tvActive);
        tvToday = findViewById(R.id.tvToday);
        tvLast  = findViewById(R.id.tvLast);
        btnReceive = findViewById(R.id.btnReceive);
        btnMap     = findViewById(R.id.btnMap);

        // Παρακολούθηση μίας εγγραφής
        vm.getById(uid).observe(this, this::bind);

        btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, uid); // περνάμε το UID για edit
            startActivity(i);
        });


        // «Έλαβα σήμερα»
        btnReceive.setOnClickListener(v ->
                vm.receivedToday(uid, rows -> {
                    if (rows != null && rows > 0) {
                        Toast.makeText(this, "Καταχωρήθηκε: Έλαβα σήμερα", Toast.LENGTH_SHORT).show();
                        // Το UI θα ανανεωθεί έτσι κι αλλιώς από το LiveData observe
                    } else {
                        Toast.makeText(this, "Δεν έγινε ενημέρωση", Toast.LENGTH_SHORT).show();
                    }
                })
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void bind(PrescriptionWithTerm it) {
        if (it == null) return;

        tvShort.setText(it.drug.shortName);
        tvDesc.setText(emptyToDash(it.drug.description));
        tvTerm.setText("Χρονισμός: " + emptyToDash(it.termCode));

        String from = formatEpochDay(it.drug.startDateEpoch);
        String to   = formatEpochDay(it.drug.endDateEpoch);
        tvRange.setText("Διάρκεια: " + from + " → " + to);

        tvActive.setText("Ενεργό: " + (it.drug.isActive ? "ΝΑΙ" : "ΟΧΙ"));
        tvToday.setText("Έλαβα σήμερα: " + (it.drug.hasReceivedToday ? "ΝΑΙ" : "ΟΧΙ"));
        tvLast.setText("Τελευταία λήψη: " +
                (it.drug.lastDateReceivedEpoch == null ? "-" : formatEpochDay(it.drug.lastDateReceivedEpoch)));

        String loc = it.drug.doctorLocation == null ? "" : it.drug.doctorLocation.trim();
        btnMap.setEnabled(!loc.isEmpty());
        btnMap.setOnClickListener(v -> openMapWithGeocoderOrSearch(loc));

        Button btnEdit = findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(v -> {
            android.content.Intent i = new android.content.Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, it.drug.uid); // ΠΕΡΝΑΜΕ ΤΟ UID
            startActivity(i);
        });

    }

    // ------------ Helpers ------------

    private String emptyToDash(String s) {
        return (s == null || s.trim().isEmpty()) ? "-" : s.trim();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String formatEpochDay(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).format(DATE_FMT);
    }

    /**
     * Προσπαθεί Geocoder για ακριβές pin· αν δεν βρει, ανοίγει search με geo:0,0?q=
     */
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
                        // fallback: search
                        openMapSearch(query);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> openMapSearch(query));
            }
        }).start();
    }

    private void openMapSearch(String q) {
        // Αν δεν δώσεις πλήρη διεύθυνση, πρόσθεσε πόλη για ακρίβεια (προαιρετικό)
        String query = q;
        if (!query.contains(",") && !query.matches(".*\\d.*")) {
            query = query + ", Αθήνα";
        }
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
        startMapIntent(uri);
    }

    private void startMapIntent(Uri uri) {
        Intent map = new Intent(Intent.ACTION_VIEW, uri);
        PackageManager pm = getPackageManager();

        // Προτίμησε Google Maps αν υπάρχει
        map.setPackage("com.google.android.apps.maps");
        if (map.resolveActivity(pm) == null) {
            map.setPackage(null); // οποιοδήποτε app χαρτών
        }

        if (map.resolveActivity(pm) != null) {
            startActivity(map);
        } else {
            Toast.makeText(this, "Δεν βρέθηκε εφαρμογή χαρτών", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@androidx.annotation.NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            android.content.Intent i = new android.content.Intent(this, ui.AddEditActivity.class);
            i.putExtra(ui.AddEditActivity.EXTRA_UID, uid);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
