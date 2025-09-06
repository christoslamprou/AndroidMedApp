package ui;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.LocalDate;

import data.AppDatabase;

public class RecomputeWorker extends Worker {
    public RecomputeWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    // Periodic job: recompute isActive/hasReceivedToday flags for "today"
    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull @Override
    public Result doWork() {
        try {
            long today = LocalDate.now().toEpochDay(); // epoch-day (yyyy-MM-dd)
            AppDatabase.getInstance(getApplicationContext())
                    .prescriptionDao()
                    .recomputeForToday(today);
            return Result.success();
        } catch (Exception e) {
            // If something transient fails, ask WorkManager to retry
            return Result.retry();
        }
    }
}
