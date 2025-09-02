package ui;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import data.PrescriptionWithTerm;

public class Export {

    public static String nowStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String toHtml(List<PrescriptionWithTerm> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'><title>Active meds</title>");
        sb.append("<style>body{font-family:sans-serif}table{border-collapse:collapse;width:100%}th,td{border:1px solid #ccc;padding:6px}th{background:#f5f5f5}</style>");
        sb.append("</head><body><h2>Ενεργές συνταγές</h2>");
        sb.append("<table><tr>")
                .append("<th>UID</th><th>Όνομα</th><th>Περιγραφή</th><th>Time term</th>")
                .append("<th>Έναρξη</th><th>Λήξη</th><th>Ιατρός</th><th>Τοποθεσία</th>")
                .append("<th>IsActive</th><th>HasReceivedToday</th><th>LastDateReceived</th>")
                .append("</tr>");
        for (PrescriptionWithTerm it : list) {
            sb.append("<tr>")
                    .append(td(it.drug.uid))
                    .append(td(esc(it.drug.shortName)))
                    .append(td(esc(it.drug.description)))
                    .append(td(esc(it.termCode)))
                    .append(td(epoch(it.drug.startDateEpoch)))
                    .append(td(epoch(it.drug.endDateEpoch)))
                    .append(td(esc(it.drug.doctorName)))
                    .append(td(esc(it.drug.doctorLocation)))
                    .append(td(it.drug.isActive ? "true" : "false"))
                    .append(td(it.drug.hasReceivedToday ? "true" : "false"))
                    .append(td(it.drug.lastDateReceivedEpoch == null ? "-" : epoch(it.drug.lastDateReceivedEpoch)))
                    .append("</tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }
    private static String td(Object x){ return "<td>"+(x==null?"-":x)+"</td>"; }
    private static String esc(String s){ return s==null?"-":s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;"); }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String epoch(long d){ return java.time.LocalDate.ofEpochDay(d).toString(); }


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String toTxt(List<PrescriptionWithTerm> list) {
        StringBuilder sb = new StringBuilder();
        for (PrescriptionWithTerm it : list) {
            sb.append("UID: ").append(it.drug.uid).append('\n');
            sb.append("Όνομα: ").append(nz(it.drug.shortName)).append('\n');
            sb.append("Περιγραφή: ").append(nz(it.drug.description)).append('\n');
            sb.append("Time term: ").append(nz(it.termCode)).append('\n');
            sb.append("Έναρξη: ").append(epoch(it.drug.startDateEpoch)).append('\n');
            sb.append("Λήξη: ").append(epoch(it.drug.endDateEpoch)).append('\n');
            sb.append("Ιατρός: ").append(nz(it.drug.doctorName)).append('\n');
            sb.append("Τοποθεσία: ").append(nz(it.drug.doctorLocation)).append('\n');
            sb.append("IsActive: ").append(it.drug.isActive).append('\n');
            sb.append("HasReceivedToday: ").append(it.drug.hasReceivedToday).append('\n');
            sb.append("LastDateReceived: ")
                    .append(it.drug.lastDateReceivedEpoch==null?"-":epoch(it.drug.lastDateReceivedEpoch)).append('\n');
            sb.append("----------------------------------------\n");
        }
        return sb.toString();
    }
    private static String nz(String s){ return (s==null||s.trim().isEmpty())?"-":s; }


    @SuppressLint("InlinedApi")
    public static Uri saveToDownloads(Context ctx, String displayName, String mime, String content) {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyMedApp");

            Uri uri = ctx.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) return null;
            try (OutputStream os = ctx.getContentResolver().openOutputStream(uri)) {
                if (os == null) return null;
                os.write(content.getBytes(StandardCharsets.UTF_8));
                os.flush();
                return uri;
            } catch (Exception e) {
                return null;
            }
        } else {
            // TODO: Προσθήκη υποστήριξης για API < 29 (SAF ACTION_CREATE_DOCUMENT) αν χρειαστεί
            return null;
        }
    }

    // -- helpers --

    private static String nullToDash(String s) { return (s == null || s.trim().isEmpty()) ? "-" : s; }

    private static String escape(String s) {
        if (s == null) return "-";
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String epochToStr(long epochDay) {
        java.time.LocalDate d = java.time.LocalDate.ofEpochDay(epochDay);
        return d.toString();
    }
}
