package br.org.eldorado.hiaac.datacollector.util;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;

public class CsvBuilder {

    private static final String TAG = "CsvBuilder";
    private Log log = new Log(TAG);

    private LabelConfigViewModel mDbView;
    private Context mContext;

    public CsvBuilder(LabelConfigViewModel dbView, Context context) {
        mDbView = dbView;
        mContext = context;
    }

    public void appendData(File csvFile, List<LabeledData> data, int type) {
        try {
            log.d("Appending data " + data.get(0).getCSVFormattedString()[1] + " - " + data.get(0).getCSVFormattedString()[3] + " " + data.size());
            CSVWriter writer = null;
            Locale l = Locale.getDefault();
            try {
                Locale.setDefault(new Locale("pt", "BR"));
                writer = new CSVWriter(new FileWriter(csvFile, true),
                        ';',
                        CSVWriter.NO_QUOTE_CHARACTER,
                        CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                        CSVWriter.DEFAULT_LINE_END);
                if (type == 0) {
                    writer.writeNext(data.get(0).getCSVHeaders());
                }
                for (LabeledData dt : data) {
                    writer.writeNext(dt.getCSVFormattedString());
                    dt.setIsDataUsed(1);
                }
                mDbView.updateLabeledData(data);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        Locale.setDefault(l);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File create(List<LabeledData> data, String timestamp) {

        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        data.get(0).getConfigId());
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File csvFile = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        data.get(0).getConfigId() +
                        File.separator +
                        composeFileName(data, timestamp));
        log.d("Creating CSV file: " + csvFile.getAbsolutePath());
        appendData(csvFile, data, 0);
        return csvFile;
    }

    private String composeFileName(List<LabeledData> data, String timestamp) {
        return data.get(0).getUserId() + "_" +
               data.get(0).getExperiment() + "_" +
               data.get(0).getActivity() + "_" +
               data.get(0).getDevicePosition() + "__" +
               timestamp + // UID
               ".csv";

    }
}
