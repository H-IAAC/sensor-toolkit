package br.org.eldorado.hiaac.datacollector.util;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvFiles {
    private static final String TAG = "CsvFiles";
    private Log log = new Log(TAG);
    private final Context mContext;

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
        List<File> filesList = new ArrayList<>();
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
}
