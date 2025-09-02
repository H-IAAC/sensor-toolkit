package br.org.eldorado.hiaac.datacollector.provider;


import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;


import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import br.org.eldorado.hiaac.datacollector.util.CsvFiles;
import br.org.eldorado.hiaac.datacollector.util.Log;

public class HiaacExtraSensoryProvider extends ContentProvider {

    private final Log log  = new Log("HiaacExtrSensoryProvider");
    public static final String AUTHORITY = "br.org.eldorado.hiaac.hiaacextrasensoryprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    // URIs
    private static final int FILE_LIST = 1;
    private static final int GET_FILE = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        uriMatcher.addURI(AUTHORITY, "files", FILE_LIST);      // List all extrasensory files
        uriMatcher.addURI(AUTHORITY, "files/*", GET_FILE);    // Get an specific file
    }

    private Context context;

    private CsvFiles csvFiles;


    @Override
    public boolean onCreate() {
        context = getContext();
        csvFiles = new CsvFiles(context);
        return true;
    }

    /**
     * List all extrasensory files filtering it on date range
     * Filters must be passed in selectionArgs parameter (format dd/MM/yyyy) like this:
     * selectionArgs[0] -> StartDate
     * selectionArgs[1] -> EndDate
     * If only the StartDate is provided, the filter will be applied between StartDate and StartDate + 24 hours.
     * @param uri
     * @param projection
     * @param selection
     * @param selectionArgs
     * @param sortOrder
     * @return
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {

        if (uriMatcher.match(uri) == FILE_LIST) {
            long startDate = 0;
            long endDate = Long.MAX_VALUE;
            String[] cols = new String[]{"timestamp", "name"};
            MatrixCursor cursor = new MatrixCursor(cols);

            List<File> files = csvFiles.listFilesFromAllConfigs();

            /* Filter files on a date range */
            if (selectionArgs != null && selectionArgs.length > 0) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                try {
                    LocalDate localDate = LocalDate.parse(selectionArgs[0], formatter);

                    startDate = localDate
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                    endDate = localDate
                            .plusDays(1)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                } catch (Exception e) {
                    throw new IllegalArgumentException("StartDate format must be dd/MM/yyyy");
                }
                if (selectionArgs.length > 1) {
                    try {
                        LocalDate localDate = LocalDate.parse(selectionArgs[1], formatter);
                        endDate = localDate
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli();
                    } catch (Exception e) {
                        throw new IllegalArgumentException("EndDate format must be dd/MM/yyyy");
                    }
                }
            }

            if (files != null) {
                for (File f : files) {
                    CsvFiles.CsvFileName fileName =  CsvFiles.decomposeFileName(f.getName());
                    if (isExtraSensoryFile(f) && Long.parseLong(fileName.devicePosition) >= startDate && Long.parseLong(fileName.devicePosition) <= endDate) {
                        cursor.addRow(new Object[]{fileName.devicePosition, f.getName()});
                    }
                }
            }
            return cursor;
        }

        return null;
    }

    /**
     * Get specific extrasensory file
     * @param uri
     * @param mode
     * @return
     * @throws FileNotFoundException
     */
    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        log.d("openAssetFile - " + uri.getLastPathSegment());
        return new AssetFileDescriptor(openFile(uri, mode), 0, -1);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        log.d("openFile - " + uri.getLastPathSegment());
        if (uriMatcher.match(uri) == GET_FILE) {
            String filename = uri.getLastPathSegment();
            for (File file : csvFiles.listFilesFromAllConfigs()) {
                if(file.getName().equals(filename)) {
                    log.d("openFile: file found! " + file.getName());
                    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                }
            }
        }
        throw new FileNotFoundException("File Not Found!: " + uri);
    }


    @Override
    public String getType(Uri uri) {
        if (uriMatcher.match(uri) == FILE_LIST) {
            return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".files";
        } else if (uriMatcher.match(uri) == GET_FILE) {
            return "application/octet-stream";
        }
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }

    private boolean isExtraSensoryFile(File file) {
        return file.getName().toLowerCase().contains("extrasensory");
    }
}
