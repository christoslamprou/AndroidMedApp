package data;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.util.concurrent.Executors;

@Database(entities = {PrescriptionDrug.class, TimeTerm.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    // Single Room DB instance for the app
    private static volatile AppDatabase INSTANCE;

    public abstract PrescriptionDao prescriptionDao();
    public abstract TimeTermDao timeTermDao();

    // Get the DB instance (build it once)
    public static AppDatabase getInstance(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            ctx.getApplicationContext(),
                            AppDatabase.class,
                            "meds.db"
                    ).addCallback(new Callback() {
                        @Override public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            super.onCreate(db);
                            // Seed time_terms on first creation (run off the main thread)
                            Executors.newSingleThreadExecutor().execute(() -> {
                                TimeTermDao dao = getInstance(ctx).timeTermDao();
                                dao.insertAll(
                                        new TimeTerm(1, "before-breakfast", 1),
                                        new TimeTerm(2, "at-breakfast", 2),
                                        new TimeTerm(3, "after-breakfast", 3),
                                        new TimeTerm(4, "before-lunch", 4),
                                        new TimeTerm(5, "at-lunch", 5),
                                        new TimeTerm(6, "after-lunch", 6),
                                        new TimeTerm(7, "before-dinner", 7),
                                        new TimeTerm(8, "at-dinner", 8),
                                        new TimeTerm(9, "after-dinner", 9)
                                );
                            });
                        }
                    }).build();
                }
            }
        }
        return INSTANCE;
    }
}
