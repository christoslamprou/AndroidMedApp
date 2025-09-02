package ui;

import android.app.Application;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import data.*;
import java.time.LocalDate;
import java.util.List;

public class MedViewModel extends AndroidViewModel {
    private final PrescriptionRepository repo;
    private final LiveData<List<PrescriptionWithTerm>> active;
    private final LiveData<List<TimeTerm>> terms;

    public MedViewModel(@NonNull Application app) {
        super(app);
        repo = new PrescriptionRepository(app);
        active = repo.getActive();
        terms  = repo.getTimeTerms();
    }

    public LiveData<List<PrescriptionWithTerm>> getActive() { return active; }
    public LiveData<List<TimeTerm>> getTimeTerms() { return terms; }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void addDrug(String shortName, String description,
                        LocalDate start, LocalDate end,
                        int timeTermId, String doctorName, String doctorLocation) {
        if (shortName == null || shortName.trim().isEmpty()) return;
        if (end.isBefore(start)) return;

        PrescriptionDrug d = new PrescriptionDrug();
        d.shortName = shortName.trim();
        d.description = description == null ? "" : description.trim();
        d.startDateEpoch = start.toEpochDay();
        d.endDateEpoch = end.toEpochDay();
        d.timeTermId = timeTermId;
        d.doctorName = doctorName == null ? "" : doctorName.trim();
        d.doctorLocation = doctorLocation == null ? "" : doctorLocation.trim();

        // αρχικά (Comment #1): isActive/hasReceivedToday false, lastDateReceived null
        d.isActive = false;
        d.hasReceivedToday = false;
        d.lastDateReceivedEpoch = null;

        long today = LocalDate.now().toEpochDay();
        d.isActive = (today >= d.startDateEpoch && today <= d.endDateEpoch);

        repo.insert(d);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void deleteByUid(int uid, java.util.function.Consumer<Integer> onResult) {
        repo.deleteById(uid, onResult);
    }

    public androidx.lifecycle.LiveData<data.PrescriptionWithTerm> getById(int uid) {
        return repo.getById(uid);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void receivedToday(int uid, java.util.function.Consumer<Integer> onResult) {
        long today = java.time.LocalDate.now().toEpochDay();
        repo.markReceivedToday(uid, today, onResult);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void exportActive(boolean asHtml, java.util.function.Consumer<android.net.Uri> onDone) {
        repo.exportActive(asHtml, onDone);
    }

    public androidx.lifecycle.LiveData<data.PrescriptionDrug> observeDrug(int uid) {
        return repo.observeById(uid);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveNew(String shortName, String desc,
                        java.time.LocalDate start, java.time.LocalDate end,
                        int timeTermId, String doctorName, String doctorLocation,
                        java.util.function.Consumer<Long> onInserted) {

        if (shortName == null || shortName.trim().isEmpty()) { if (onInserted!=null) onInserted.accept(-1L); return; }
        if (end.isBefore(start)) { if (onInserted!=null) onInserted.accept(-1L); return; }

        data.PrescriptionDrug d = new data.PrescriptionDrug();
        d.shortName = shortName.trim();
        d.description = desc == null ? "" : desc.trim();
        d.startDateEpoch = start.toEpochDay();
        d.endDateEpoch   = end.toEpochDay();
        d.timeTermId = timeTermId;
        d.doctorName = doctorName == null ? "" : doctorName.trim();
        d.doctorLocation = doctorLocation == null ? "" : doctorLocation.trim();

        long today = java.time.LocalDate.now().toEpochDay();
        d.isActive = (today >= d.startDateEpoch && today <= d.endDateEpoch);
        d.hasReceivedToday = false;
        d.lastDateReceivedEpoch = null;

        // insert με callback (αν δεν έχεις overload στο repo, βάλε το απλό insert χωρίς callback)
        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            long id = repo.pDao.insert(d); // ή repo.insert(d) αν έτσι το έχεις
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onInserted != null) onInserted.accept(id);
            });
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveEdit(int uid, String shortName, String desc,
                         java.time.LocalDate start, java.time.LocalDate end,
                         int timeTermId, String doctorName, String doctorLocation,
                         java.util.function.Consumer<Integer> onUpdated) {
        if (shortName == null || shortName.trim().isEmpty()) { if (onUpdated!=null) onUpdated.accept(0); return; }
        if (end.isBefore(start)) { if (onUpdated!=null) onUpdated.accept(0); return; }

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            data.PrescriptionDrug cur = repo.getByIdSync(uid);
            if (cur == null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (onUpdated != null) onUpdated.accept(0);
                });
                return;
            }
            cur.shortName = shortName.trim();
            cur.description = desc == null ? "" : desc.trim();
            cur.startDateEpoch = start.toEpochDay();
            cur.endDateEpoch   = end.toEpochDay();
            cur.timeTermId = timeTermId;
            cur.doctorName = doctorName == null ? "" : doctorName.trim();
            cur.doctorLocation = doctorLocation == null ? "" : doctorLocation.trim();

            long today = java.time.LocalDate.now().toEpochDay();
            cur.isActive = (today >= cur.startDateEpoch && today <= cur.endDateEpoch);
            // ΔΕΝ πειράζουμε lastDateReceived/hasReceivedToday εδώ

            int rows = repo.pDao.update(cur);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                if (onUpdated != null) onUpdated.accept(rows);
            });
        });
    }


}
