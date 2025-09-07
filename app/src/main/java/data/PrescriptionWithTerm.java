package data;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

// DTO for a JOIN result: the prescription plus selected fields from time_terms
public class PrescriptionWithTerm {

    // All columns from prescription_drugs
    @Embedded public PrescriptionDrug drug;

    // Joined term fields (aliased in the query)
    @ColumnInfo(name = "term_id")    public int termId;    // TimeTerm.id
    @ColumnInfo(name = "term_code")  public String termCode;  // e.g., "before-breakfast"
    @ColumnInfo(name = "term_order") public int termOrder; // sortOrder used for list ordering
}
