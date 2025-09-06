package data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Time term row (e.g., "before-breakfast"), used to group/order prescriptions
@Entity(tableName = "time_terms")
public class TimeTerm {
    @PrimaryKey public int id;     // stable id
    public String code;            // machine-friendly label
    public int sortOrder;          // list ordering

    // Simple constructor for seeding/inserts
    public TimeTerm(int id, String code, int sortOrder) {
        this.id = id;
        this.code = code;
        this.sortOrder = sortOrder;
    }
}
