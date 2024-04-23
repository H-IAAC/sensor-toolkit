package br.org.eldorado.hiaac.datacollector.util;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import android.content.Context;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;

public class CsvBuilder {
    private final Log log = new Log("CsvBuilder");
    private final Locale l = Locale.getDefault();
    private final LabelConfigViewModel mDbView;
    private final Context mContext;

    public CsvBuilder(LabelConfigViewModel dbView, Context context) {
        mDbView = dbView;
        mContext = context;
    }

    public void appendHeader(File csvFile) {
        CSVWriter writer = null;

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
            log.d("Appending CSV header failed: " + e.getMessage());
        } finally {
            if (writer != null) {
                Locale.setDefault(l);

                try {
                    writer.close();
                } catch (IOException e) {
                    log.d("Appending CSV header, file failed: " + e.getMessage());
                }
            }
        }
    }

    public synchronized void appendData(File csvFile, List<LabeledData> data, boolean createHeader) {

        if (csvFile == null) return;

        log.d("Appending " + data.size() + " data");
        CSVWriter writer = null;

        try {
            Locale.setDefault(new Locale("pt", "BR"));
            writer = new CSVWriter(new FileWriter(csvFile, true),
                                   ';',
                                   CSVWriter.NO_QUOTE_CHARACTER,
                                   CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                                   CSVWriter.DEFAULT_LINE_END);

            if (createHeader)
                writer.writeNext(data.get(0).getCSVHeaders());

            for (LabeledData dt : data) {
                writer.writeNext(dt.getCSVFormattedString());
                dt.setIsDataUsed(1);
            }

            // Update db to flag data written to the csv file.
            mDbView.updateLabeledData(data);
        } catch (Exception e) {
            log.d("Appending CSV data failed: " + e.getMessage());
        } finally {
            if (writer != null) {
                Locale.setDefault(l);

                try {
                    writer.close();
                } catch (IOException e) {
                    log.d("Appending CSV data, file failed: " + e.getMessage());
                }
            }
        }
    }

    public synchronized File getCsvFile(LabeledData data, String timestamp, boolean includeHeader) {
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

    /* Used by firebase, as it is always sending timestamp as '0' */
    public synchronized File create(List<LabeledData> data) {

        File csvFile = getCsvFile(data.get(0), "0", false);

        log.d("Creating CSV file: " + csvFile.getAbsolutePath());
        appendData(csvFile, data, true);
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
