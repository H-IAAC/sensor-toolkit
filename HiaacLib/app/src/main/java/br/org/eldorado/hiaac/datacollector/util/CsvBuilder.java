package br.org.eldorado.hiaac.datacollector.util;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import android.content.Context;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;

public class CsvBuilder {

    private static final String TAG = "CsvBuilder";
    private final Log log = new Log(TAG);

    private final LabelConfigViewModel mDbView;
    private final Context mContext;

    public CsvBuilder(LabelConfigViewModel dbView, Context context) {
        mDbView = dbView;
        mContext = context;
    }

    public void appendHeader(File csvFile) {
        CSVWriter writer = null;
        Locale l = Locale.getDefault();

        if(csvFile.length() != 0) {
            log.d("CSV file is not empty! Ignoring header append.");
            return;
        }

        try {
            writer = new CSVWriter(new FileWriter(csvFile, false),
                    ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);

            Locale.setDefault(new Locale("pt", "BR"));

            writer.writeNext(LabeledData.getCSVHeaders());

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
    }

    public void appendData(File csvFile, List<LabeledData> data, int type) {

        if (csvFile == null) return;

        try {
            log.d("Appending " + data.size() + " data");
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

    public File getCsvFile(LabeledData data, String timestamp, boolean includeHeader) {
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        data.getConfigId());

        if (!directory.exists())
            directory.mkdirs();

        File csvFile = new File(directory.getAbsolutePath() +
                                File.separator +
                                composeFileName(data, timestamp));

        if (includeHeader)
            appendHeader(csvFile);

        return csvFile;
    }

    public File create(List<LabeledData> data, String timestamp) {

        File csvFile = getCsvFile(data.get(0), timestamp, false);

        log.d("Creating CSV file: " + csvFile.getAbsolutePath());
        appendData(csvFile, data, 0);
        return csvFile;
    }

    private String composeFileName(LabeledData data, String timestamp) {
        return data.getUserId() + "_" +
               data.getExperiment() + "_" +
               data.getActivity() + "_" +
               data.getDevicePosition() + "__" +
               timestamp + // UID
               ".csv";

    }
}
