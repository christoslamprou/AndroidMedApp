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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @NonNull @Override
    public Result doWork() {
        try {
            long today = LocalDate.now().toEpochDay(); // αν δεν θες java.time, δες εναλλακτική πιο κάτω
            AppDatabase.getInstance(getApplicationContext())
                    .prescriptionDao()
                    .recomputeForToday(today);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
