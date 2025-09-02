package data;


import androidx.room.ColumnInfo;
import androidx.room.Embedded;

public class PrescriptionWithTerm {
    @Embedded public PrescriptionDrug drug;

    @ColumnInfo(name = "term_id") public int termId;
    @ColumnInfo(name = "term_code") public String termCode;
    @ColumnInfo(name = "term_order") public int termOrder;
}

