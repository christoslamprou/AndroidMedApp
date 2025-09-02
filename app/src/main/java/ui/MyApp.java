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

    private void scheduleHourlyRecompute() {
        java.util.concurrent.TimeUnit H = java.util.concurrent.TimeUnit.HOURS;

        androidx.work.PeriodicWorkRequest req =
                new androidx.work.PeriodicWorkRequest.Builder(ui.RecomputeWorker.class, 1, H)
                        .addTag("recompute-hourly")
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "recomputeHourly",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                req
        );
    }

    @Override public void onCreate() {
        super.onCreate();
        scheduleHourlyRecompute();
        // άμεσος υπολογισμός στην εκκίνηση:
        androidx.work.WorkManager.getInstance(this)
                .enqueue(androidx.work.OneTimeWorkRequest.from(ui.RecomputeWorker.class));
    }


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
