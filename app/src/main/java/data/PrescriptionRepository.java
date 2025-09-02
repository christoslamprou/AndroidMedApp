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
    public final PrescriptionDao pDao;
    private final TimeTermDao tDao;

    private final android.app.Application app;

    public PrescriptionRepository(android.app.Application app) {
        this.app = app;
        AppDatabase db = AppDatabase.getInstance(app);
        pDao = db.prescriptionDao();
        tDao = db.timeTermDao();
    }

    public LiveData<List<TimeTerm>> getTimeTerms() { return tDao.getAll(); }
    public LiveData<List<PrescriptionWithTerm>> getActive() { return pDao.getActiveWithTerm(); }

    public void insert(PrescriptionDrug d) {
        Executors.newSingleThreadExecutor().execute(() -> pDao.insert(d));
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void deleteById(int uid, java.util.function.Consumer<Integer> onResult) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.deleteById(uid);
            if (onResult != null) onResult.accept(rows);
        });
    }

    public LiveData<PrescriptionWithTerm> getById(int uid) {
        return pDao.getByIdWithTerm(uid);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void markReceivedToday(int uid, long today, java.util.function.Consumer<Integer> onResult) {
        Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.markReceivedToday(uid, today);
            if (onResult != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> onResult.accept(rows));
            }
        });


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void exportActive(boolean asHtml, Consumer<Uri> onDone) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<PrescriptionWithTerm> list = pDao.getActiveWithTermNow();

            String stamp = ui.Export.nowStamp(); // <-- ΟΧΙ ui.Export
            String mime  = asHtml ? "text/html" : "text/plain";
            String name  = "meds_active_" + stamp + (asHtml ? ".html" : ".txt");
            String body  = asHtml ? ui.Export.toHtml(list) : ui.Export.toTxt(list);

            Uri uri = ui.Export.saveToDownloads(app, name, mime, body);

            new Handler(Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.accept(uri);
            });
        });
    }

    public data.PrescriptionDrug getByIdSync(int uid) {
        return pDao.getByIdSync(uid);
    }

    public androidx.lifecycle.LiveData<data.PrescriptionDrug> observeById(int uid) {
        return pDao.observeById(uid);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void update(data.PrescriptionDrug d, java.util.function.Consumer<Integer> onDone) {
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            int rows = pDao.update(d);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onDone != null) onDone.accept(rows);
            });
        });
    }




}
