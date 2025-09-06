package data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface PrescriptionDao {

    // Insert a new prescription; returns the new row id (uid)
    @Insert
    long insert(PrescriptionDrug drug);

    // Delete by UID; returns affected row count
    @Query("DELETE FROM prescription_drugs WHERE uid = :uid")
    int deleteById(int uid);

    // Live list of ACTIVE prescriptions joined with time terms, sorted by term then uid
    @Query("SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
            "FROM prescription_drugs p " +
            "JOIN time_terms t ON p.timeTermId = t.id " +
            "WHERE p.isActive = 1 " +
            "ORDER BY t.sortOrder ASC, p.uid ASC")
    LiveData<List<PrescriptionWithTerm>> getActiveWithTerm();

    // Live single item (by uid) joined with its time term
    @Query("SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
            "FROM prescription_drugs p " +
            "JOIN time_terms t ON p.timeTermId = t.id " +
            "WHERE p.uid = :uid LIMIT 1")
    LiveData<PrescriptionWithTerm> getByIdWithTerm(int uid);

    // Mark as received today (sets lastDateReceivedEpoch and hasReceivedToday)
    @Query("UPDATE prescription_drugs " +
            "SET lastDateReceivedEpoch = :today, hasReceivedToday = 1 " +
            "WHERE uid = :uid")
    int markReceivedToday(int uid, long today);

    // Recompute flags for 'today' (isActive, hasReceivedToday)
    @Query("UPDATE prescription_drugs " +
            "SET isActive = CASE WHEN :today BETWEEN startDateEpoch AND endDateEpoch THEN 1 ELSE 0 END, " +
            "    hasReceivedToday = CASE WHEN lastDateReceivedEpoch = :today THEN 1 ELSE 0 END")
    void recomputeForToday(long today);

    // Synchronous fetch of ACTIVE items for export (no LiveData)
    @Query("SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
            "FROM prescription_drugs p " +
            "JOIN time_terms t ON p.timeTermId = t.id " +
            "WHERE p.isActive = 1 " +
            "ORDER BY t.sortOrder ASC, p.uid ASC")
    List<PrescriptionWithTerm> getActiveWithTermNow();

    // Update an existing prescription; returns affected row count
    @Update
    int update(PrescriptionDrug drug);

    // Synchronous single item (by uid)
    @Query("SELECT * FROM prescription_drugs WHERE uid = :uid LIMIT 1")
    PrescriptionDrug getByIdSync(int uid);

    // Live single item (by uid) – useful for form binding
    @Query("SELECT * FROM prescription_drugs WHERE uid = :uid LIMIT 1")
    LiveData<PrescriptionDrug> observeById(int uid);
}
