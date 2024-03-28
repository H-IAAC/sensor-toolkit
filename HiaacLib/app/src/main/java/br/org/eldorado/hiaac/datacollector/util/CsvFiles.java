package br.org.eldorado.hiaac.datacollector.util;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import android.content.Context;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CsvFiles {
    private static final String TAG = "CsvFiles";
    private Log log = new Log(TAG);
    private Context mContext;

    public CsvFiles(Context context) {
        mContext = context;
    }

    public List<File> getFiles(long id) {
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        id);
        List<File> filesList = new ArrayList<File>();
        if (directory.exists()) {
            filesList = new ArrayList<>(Arrays.asList(directory.listFiles()));
        }
        return filesList;
    }

    public void deleteDirectory(long id) {
        try {
            File directory = new File(
                    mContext.getFilesDir().getAbsolutePath() +
                            File.separator +
                            FOLDER_NAME +
                            File.separator +
                            id);
            if (directory.exists()) {
                for (File f : directory.listFiles()) {
                    f.delete();
                }
                directory.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CsvFileName decomposeFileName(String fileName) {

        CsvFileName file = new CsvFileName();

        if ("csv".equals(Tools.getFileExtension(fileName))) {
            // File name example: user1_experiment1_activity1_Perna_20230626.103628.csv
            String[] fileContent = fileName.split("_");
            file.user = fileContent[0];
            file.experiment = fileContent[1];
            file.activity = fileContent[2];
            file.devicePosition = fileContent[3];
            file.startTime = fileContent[5].substring(0, fileContent[5].lastIndexOf("."));
        }

        return file;
    }

    public static String CsvFileNameConvertTimestamp(String startTime) {

        if (startTime == null || "0".equals(startTime) || "null".equals(startTime)) return "time";

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");

        try {
            Date date = df.parse(startTime);
            return date.getTime() + "";
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static class CsvFileName {
        public String experiment;
        public String activity;
        public String user;
        public String devicePosition;
        public String startTime;
    }
}
