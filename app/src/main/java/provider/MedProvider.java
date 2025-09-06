package provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.sqlite.db.SupportSQLiteDatabase;

import data.AppDatabase;

public class MedProvider extends ContentProvider {

    // URI match codes
    private static final int P_ALL = 1;
    private static final int P_ID  = 2;
    private static final int T_ALL = 3;
    private static final int T_ID  = 4;

    // Map incoming URIs to match codes
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        MATCHER.addURI(MedContract.AUTHORITY, MedContract.PATH_PRESCRIPTIONS,        P_ALL);
        MATCHER.addURI(MedContract.AUTHORITY, MedContract.PATH_PRESCRIPTIONS + "/#", P_ID);
        MATCHER.addURI(MedContract.AUTHORITY, MedContract.PATH_TIME_TERMS,           T_ALL);
        MATCHER.addURI(MedContract.AUTHORITY, MedContract.PATH_TIME_TERMS + "/#",    T_ID);
    }

    // Low-level DB (via Room)
    private SupportSQLiteDatabase db;

    @Override public boolean onCreate() {
        // Open the database for direct SQL queries used by the provider
        db = AppDatabase.getInstance(getContext()).getOpenHelper().getWritableDatabase();
        return true;
    }

    @Nullable @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        int m = MATCHER.match(uri);
        String sql;
        switch (m) {
            case P_ALL:
                // Alias uid as _id for CursorAdapter compatibility
                sql = "SELECT " +
                        "uid AS _id, " +
                        "uid, shortName, description, startDateEpoch, endDateEpoch, timeTermId, " +
                        "doctorName, doctorLocation, isActive, hasReceivedToday, lastDateReceivedEpoch " +
                        "FROM prescription_drugs";
                if (selection != null && !selection.isEmpty()) sql += " WHERE " + selection;
                if (sortOrder != null && !sortOrder.isEmpty()) sql += " ORDER BY " + sortOrder;
                break;

            case P_ID:
                long id = ContentUris.parseId(uri);
                sql = "SELECT uid AS _id, uid, shortName, description, startDateEpoch, endDateEpoch, timeTermId, " +
                        "doctorName, doctorLocation, isActive, hasReceivedToday, lastDateReceivedEpoch " +
                        "FROM prescription_drugs WHERE uid = " + id + (selection!=null? " AND ("+selection+")":"");
                break;

            case T_ALL:
                sql = "SELECT id AS _id, id, code, sortOrder FROM time_terms";
                if (selection != null && !selection.isEmpty()) sql += " WHERE " + selection;
                if (sortOrder != null && !sortOrder.isEmpty()) sql += " ORDER BY " + sortOrder;
                break;

            case T_ID:
                long tid = ContentUris.parseId(uri);
                sql = "SELECT id AS _id, id, code, sortOrder FROM time_terms WHERE id = " + tid +
                        (selection!=null? " AND ("+selection+")":"");
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        Cursor c = db.query(sql, selectionArgs);
        // Let observers know which URI this cursor is tied to
        if (getContext() != null) c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable @Override
    public String getType(@NonNull Uri uri) {
        // MIME types for list/item of each path
        switch (MATCHER.match(uri)) {
            case P_ALL: return "vnd.android.cursor.dir/vnd." + MedContract.AUTHORITY + ".prescription";
            case P_ID:  return "vnd.android.cursor.item/vnd." + MedContract.AUTHORITY + ".prescription";
            case T_ALL: return "vnd.android.cursor.dir/vnd." + MedContract.AUTHORITY + ".time_term";
            case T_ID:  return "vnd.android.cursor.item/vnd." + MedContract.AUTHORITY + ".time_term";
            default:    throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Nullable @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int m = MATCHER.match(uri);
        long rowId;
        switch (m) {
            case P_ALL:
                if (values == null) values = new ContentValues();
                // Defaults for flags if not provided
                if (!values.containsKey("isActive")) values.put("isActive", 0);
                if (!values.containsKey("hasReceivedToday")) values.put("hasReceivedToday", 0);
                rowId = db.insert("prescription_drugs", SQLiteDatabase.CONFLICT_ABORT, values);
                break;
            case T_ALL:
                rowId = db.insert("time_terms", SQLiteDatabase.CONFLICT_ABORT, values);
                break;
            default:
                throw new IllegalArgumentException("Insert not supported on " + uri);
        }
        if (rowId == -1) return null;

        Uri out = ContentUris.withAppendedId(uri, rowId);
        notify(uri); // notify observers
        return out;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int m = MATCHER.match(uri);
        int rows;
        switch (m) {
            case P_ALL:
                rows = db.delete("prescription_drugs", selection, selectionArgs);
                break;
            case P_ID:
                long id = ContentUris.parseId(uri);
                rows = db.delete("prescription_drugs",
                        "uid = ?" + (selection!=null? " AND ("+selection+")":""), new String[]{ String.valueOf(id) });
                break;
            case T_ALL:
                rows = db.delete("time_terms", selection, selectionArgs);
                break;
            case T_ID:
                long tid = ContentUris.parseId(uri);
                rows = db.delete("time_terms",
                        "id = ?" + (selection!=null? " AND ("+selection+")":""), new String[]{ String.valueOf(tid) });
                break;
            default:
                throw new IllegalArgumentException("Delete not supported on " + uri);
        }
        if (rows > 0) notify(uri);
        return rows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int m = MATCHER.match(uri);
        int rows;
        switch (m) {
            case P_ALL:
                rows = db.update("prescription_drugs", SQLiteDatabase.CONFLICT_ABORT, values, selection, selectionArgs);
                break;
            case P_ID:
                long id = ContentUris.parseId(uri);
                rows = db.update("prescription_drugs", SQLiteDatabase.CONFLICT_ABORT, values,
                        "uid = ?" + (selection!=null? " AND ("+selection+")":""), new String[]{ String.valueOf(id) });
                break;
            case T_ALL:
                rows = db.update("time_terms", SQLiteDatabase.CONFLICT_ABORT, values, selection, selectionArgs);
                break;
            case T_ID:
                long tid = ContentUris.parseId(uri);
                rows = db.update("time_terms", SQLiteDatabase.CONFLICT_ABORT, values,
                        "id = ?" + (selection!=null? " AND ("+selection+")":""), new String[]{ String.valueOf(tid) });
                break;
            default:
                throw new IllegalArgumentException("Update not supported on " + uri);
        }
        if (rows > 0) notify(uri);
        return rows;
    }

    // Helper: fire content change notifications
    private void notify(Uri uri) {
        if (getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
    }
}
