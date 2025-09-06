package data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TimeTermDao {

    @Insert
    void insertAll(TimeTerm... terms); // bulk insert

    @Query("SELECT * FROM time_terms ORDER BY sortOrder ASC")
    LiveData<List<TimeTerm>> getAll(); // observe all terms sorted for UI (spinner/list)
}
