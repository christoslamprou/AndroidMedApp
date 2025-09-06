package provider;

import android.net.Uri;
import android.provider.BaseColumns;

// Contract for the ContentProvider: authority, base URIs, paths, and column names
public final class MedContract {
    private MedContract() {}

    // Unique authority for this provider
    public static final String AUTHORITY = "com.example.mymedapp.provider";
    public static final Uri BASE_URI = Uri.parse("content://" + AUTHORITY);

    // Path segments
    public static final String PATH_PRESCRIPTIONS = "prescriptions";
    public static final String PATH_TIME_TERMS    = "time_terms";

    // Prescriptions table/columns and content URI
    public static final class Prescriptions implements BaseColumns {
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH_PRESCRIPTIONS).build();
        public static final String TABLE  = "prescription_drugs";
        public static final String COL_UID = "uid";                 // primary key (int)
        public static final String COL_SHORT = "shortName";
        public static final String COL_DESC  = "description";
        public static final String COL_START = "startDateEpoch";
        public static final String COL_END   = "endDateEpoch";
        public static final String COL_TERM  = "timeTermId";
        public static final String COL_DOC   = "doctorName";
        public static final String COL_LOC   = "doctorLocation";
        public static final String COL_ACTIVE= "isActive";
        public static final String COL_TODAY = "hasReceivedToday";
        public static final String COL_LAST  = "lastDateReceivedEpoch";
    }

    // Time terms table/columns and content URI
    public static final class TimeTerms {
        public static final Uri CONTENT_URI = BASE_URI.buildUpon().appendPath(PATH_TIME_TERMS).build();
        public static final String TABLE = "time_terms";
        public static final String COL_ID   = "id";
        public static final String COL_CODE = "code";
        public static final String COL_ORDER= "sortOrder";
    }
}
