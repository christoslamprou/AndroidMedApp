package data;


import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "time_terms")
public class TimeTerm {
    @PrimaryKey public int id;
    public String code;
    public int sortOrder;
    public TimeTerm(int id, String code, int sortOrder) {
        this.id = id; this.code = code; this.sortOrder = sortOrder;
    }
}

