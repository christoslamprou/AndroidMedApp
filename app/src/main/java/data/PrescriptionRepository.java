package data;

import android.app.Application;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PrescriptionRepository {
    // DAOs used by the repository
    public final PrescriptionDao pDao;
    private final TimeTermDao tDao;

    // Application context (used by DB/Exporter)
    private final Application app;

    public PrescriptionRepository(Application app) {
        this.app = app;
        AppDatabase db = AppDatabase.getInstance(app);
        pDao = db.prescriptionDao();
        tDao = db.timeTermDao();
    }

    // Live list of time terms (for spinner)
    public LiveData<List<TimeTerm>> getTimeTerms() { return tDao.getAll(); }

    // Live list of active prescriptions joined with term (for the main list)
    public LiveData<List<PrescriptionWithTerm>> getActive() { return pDao.getActiveWithTerm(); }

    // Insert on a background thread
    public void insert(PrescriptionDrug d) {
        Executors.newSingleThreadExecutor().execute(() -> pDao.insert(d));
    }

    // Delete by UID on a background thread
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void deleteById(int uid, Consumer<Integer> onResult) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.deleteById(uid);
            if (onResult != null) onResult.accept(rows);
        });
    }

    // One item with term (LiveData) for details screen
    public LiveData<PrescriptionWithTerm> getById(int uid) {
        return pDao.getByIdWithTerm(uid);
    }

    // Mark "received today" on a background thread; callback posted to main thread
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void markReceivedToday(int uid, long today, Consumer<Integer> onResult) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.markReceivedToday(uid, today);
            if (onResult != null) {
                new Handler(Looper.getMainLooper()).post(() -> onResult.accept(rows));
            }
        });
    }

    // Export active items to Downloads (HTML/TXT) on a background thread;
    // result Uri is posted back to the main thread
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void exportActive(boolean asHtml, Consumer<Uri> onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PrescriptionWithTerm> list = pDao.getActiveWithTermNow();

            String stamp = ui.Export.nowStamp();
            String mime  = asHtml ? "text/html" : "text/plain";
            String name  = "meds_active_" + stamp + (asHtml ? ".html" : ".txt");
            String body  = asHtml ? ui.Export.toHtml(list) : ui.Export.toTxt(list);

            Uri uri = ui.Export.saveToDownloads(app, name, mime, body);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.accept(uri);
            });
        });
    }

    // Synchronous single item (used by edit form)
    public PrescriptionDrug getByIdSync(int uid) {
        return pDao.getByIdSync(uid);
    }

    // Live single item (used by edit form binding)
    public LiveData<PrescriptionDrug> observeById(int uid) {
        return pDao.observeById(uid);
    }

    // Update on a background thread; callback posted to main thread
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void update(PrescriptionDrug d, Consumer<Integer> onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.update(d);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.accept(rows);
            });
        });
    }
}
