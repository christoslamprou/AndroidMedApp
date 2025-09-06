package data;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "prescription_drugs",
        foreignKeys = @ForeignKey(
                entity = TimeTerm.class,
                parentColumns = "id",
                childColumns = "timeTermId",
                onDelete = ForeignKey.RESTRICT // do not allow deleting a term that is in use
        ),
        indices = {@Index("timeTermId")} // faster JOIN/filter by term
)
public class PrescriptionDrug {

    @PrimaryKey(autoGenerate = true)
    public int uid;                     // auto id

    public String shortName;            // required (validated in UI)
    public String description;          // optional

    public long startDateEpoch;         // LocalDate.toEpochDay()
    public long endDateEpoch;           // LocalDate.toEpochDay()

    public int timeTermId;              // FK -> TimeTerm.id

    public String doctorName;           // optional
    public String doctorLocation;       // optional (used for map)

    // Stored flags for quick queries/export
    public boolean isActive;            // true if today in [start, end]
    public Long lastDateReceivedEpoch;  // nullable; last day taken
    public boolean hasReceivedToday;    // true if lastDateReceivedEpoch == today
}
