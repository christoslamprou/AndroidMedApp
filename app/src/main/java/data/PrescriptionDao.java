package data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface PrescriptionDao {
    @Insert long insert(PrescriptionDrug drug);

    // B: διαγραφή με UID, επιστρέφει πόσες σειρές επηρέασε (θα το δείξουμε με Toast/Popup)
    @Query("DELETE FROM prescription_drugs WHERE uid = :uid")
    int deleteById(int uid);

    // D: λίστα Ενεργών συνταγών ταξινομημένων κατά Time Term (overview)
    @Query("SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
            "FROM prescription_drugs p " +
            "JOIN time_terms t ON p.timeTermId = t.id " +
            "WHERE p.isActive = 1 " +
            "ORDER BY t.sortOrder ASC, p.uid ASC")
    LiveData<List<PrescriptionWithTerm>> getActiveWithTerm();

    @Query("SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
            "FROM prescription_drugs p " +
            "JOIN time_terms t ON p.timeTermId = t.id " +
            "WHERE p.uid = :uid LIMIT 1")
    LiveData<PrescriptionWithTerm> getByIdWithTerm(int uid);

    @Query("UPDATE prescription_drugs " +
            "SET lastDateReceivedEpoch = :today, hasReceivedToday = 1 " +
            "WHERE uid = :uid")
    int markReceivedToday(int uid, long today);

    @androidx.room.Query(
            "UPDATE prescription_drugs " +
                    "SET isActive = CASE WHEN :today BETWEEN startDateEpoch AND endDateEpoch THEN 1 ELSE 0 END, " +
                    "    hasReceivedToday = CASE WHEN lastDateReceivedEpoch = :today THEN 1 ELSE 0 END"
    )
    void recomputeForToday(long today);

    @androidx.room.Query(
            "SELECT p.*, t.id AS term_id, t.code AS term_code, t.sortOrder AS term_order " +
                    "FROM prescription_drugs p " +
                    "JOIN time_terms t ON p.timeTermId = t.id " +
                    "WHERE p.isActive = 1 " +
            "ORDER BY t.sortOrder ASC, p.uid ASC"
    )
    java.util.List<PrescriptionWithTerm> getActiveWithTermNow();


    @androidx.room.Update
    int update(data.PrescriptionDrug drug);

    @androidx.room.Query("SELECT * FROM prescription_drugs WHERE uid = :uid LIMIT 1")
    data.PrescriptionDrug getByIdSync(int uid);

    // (ή LiveData αν προτιμάς στο form binding)
    @androidx.room.Query("SELECT * FROM prescription_drugs WHERE uid = :uid LIMIT 1")
    androidx.lifecycle.LiveData<data.PrescriptionDrug> observeById(int uid);


}
