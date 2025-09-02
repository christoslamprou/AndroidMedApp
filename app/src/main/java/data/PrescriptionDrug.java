package data;


import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "prescription_drugs",
        foreignKeys = @ForeignKey(
                entity = TimeTerm.class,
                parentColumns = "id",
                childColumns = "timeTermId",
                onDelete = ForeignKey.RESTRICT
        ),
        indices = {@Index("timeTermId")})
public class PrescriptionDrug {
    @PrimaryKey(autoGenerate = true) public int uid;          // UID (auto)
    public String shortName;                                  // υποχρεωτικό
    public String description;
    public long startDateEpoch;                               // LocalDate.toEpochDay()
    public long endDateEpoch;
    public int timeTermId;                                    // FK σε TimeTerm
    public String doctorName;
    public String doctorLocation;

    // Παραγόμενα/ενημερούμενα πεδία (βλ. σχόλιο #3)
    public boolean isActive;                                  // υπολογίζεται
    public Long lastDateReceivedEpoch;                        // nullable
    public boolean hasReceivedToday;                          // υπολογίζεται
}

