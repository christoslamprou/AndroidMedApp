package ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class MyApp extends Application {

    // Schedule a periodic worker that recomputes flags once every hour
    private void scheduleHourlyRecompute() {
        java.util.concurrent.TimeUnit H = java.util.concurrent.TimeUnit.HOURS;

        androidx.work.PeriodicWorkRequest req =
                new androidx.work.PeriodicWorkRequest.Builder(ui.RecomputeWorker.class, 1, H)
                        .addTag("recompute-hourly")
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recomputeHourly",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP, // keep existing schedule if present
                req
        );
    }

    @Override public void onCreate() {
        super.onCreate();
        // Start hourly schedule
        scheduleHourlyRecompute();
        // Also run one immediate recompute on app start
        androidx.work.WorkManager.getInstance(this)
                .enqueue(androidx.work.OneTimeWorkRequest.from(ui.RecomputeWorker.class));
    }

    // Utility: millis until next specific HH:mm
    private long millisUntilNext(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        long now = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.getTimeInMillis() <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return cal.getTimeInMillis() - now;
    }
}
