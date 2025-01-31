package br.org.eldorado.hiaac.profiling;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashMap;

import br.org.eldorado.hiaac.datacollector.util.Log;

class ProfilingController {

    private static final String TAG = "ProfilingController";
    private Log log;
    private long initialTime;
    private boolean isRunning;
    private boolean shouldFinish;
    private Context mContext;
    private List<ProfilingData> data;
    private long frequency;
    private Thread profilingThread;

    private ArrayList<String> csvHeader = new ArrayList<>(Arrays.asList("Timestamp",
            "Elapsed Time",
            "App VM Used RAM (MB)",
            "App Native Used RAM (MB)",
            "App Total Used RAM (MB)",
            "App VM Heap Size (MB)",
            "App Native Heap Size (MB)",
            "System Used RAM (MB)",
            "System Used RAM (%)",
            "System RAM threshold (MB)",
            "System in low memory",
            "System CPU (%)",
            "Battery Level",
            "Profiling Type"));

    private Intent batteryStatus;

    protected ProfilingController() {
        initialTime = System.currentTimeMillis();
        isRunning = false;
        shouldFinish = false;
        data = new ArrayList<>(120);
        log = new Log(TAG);
        frequency = 1000;
    }

    protected void setContext(Context ctx) {
        mContext = ctx;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = ctx.registerReceiver(null, ifilter);
    }

    protected void setFrequency(long t) {
        frequency = t;
    }

    protected void start() {
        if (!isRunning) {
            isRunning = true;
            startProfiling();
        }
    }

    private void stop() {
        isRunning = false;
        try {
            if (profilingThread != null) {
                log.d("Joining thread");
                profilingThread.join();
            }
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        profilingThread = null;
    }

    protected File finishProfiling(String path, String filename) {
        log.d("Finishing profiling . . .");
        shouldFinish = true;
        checkPoint(new HashMap<String, String>());
        stop();
        File f = createCSVFile(path, filename);
        showData();
        data.clear();
        initialTime = -1;
        return f;
    }

    private File createCSVFile(String path, String filename) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File csvFile = new File(
                directory.getAbsolutePath() +
                        File.separator +
                        filename +
                        ".csv");
        try {
            log.d("Creating  profiling CSV . . .");
            Locale l = Locale.getDefault();
            Locale.setDefault(new Locale("pt", "BR"));
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile),
                    ';',
                    CSVWriter.NO_QUOTE_CHARACTER,
                    CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                    CSVWriter.DEFAULT_LINE_END);
            writer.writeNext(csvHeader.toArray(new String[0]));
            for (ProfilingData dt : data) {
                writer.writeNext(dt.getCSVFormattedString());
            }
            writer.close();
            Locale.setDefault(l);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvFile;
    }

    private void showData() {
        synchronized (data) {
            for (ProfilingData d : data) {
                log.d(d.toString());
            }
        }
    }

    private void startProfiling() {
        profilingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                log.d("Starting profiling . . .");
                while (isRunning && !shouldFinish) {
                    try {
                        createData(ProfilingData.TYPE_AUTOMATIC);

                        long timer = 0;
                        while (timer < frequency && !shouldFinish) {
                            timer+=1000;
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.d("Error - " + e.toString());
                        stop();
                    }
                }
                shouldFinish = false;
            }
        });
        profilingThread.start();
    }

    private void createData(String type) {
        createData(type, new ArrayList<>());
    }

    private void createData(String type, List<String> extraValues) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (data) {
                    data.add(new ProfilingData(initialTime, mContext, type, batteryStatus, extraValues));
                }
            }
        }).start();
    }

    protected void checkPoint(HashMap<String, String> extra) {
        // Add all keys as part of the CSV header
        for (String key : extra.keySet()) {
            if (!csvHeader.contains(key))
                csvHeader.add(key);
        }

        // Get all values as a values for Extra fields
        ArrayList<String> extraValues = new ArrayList<>();
        for (String value : extra.values()) {
            extraValues.add(value);
        }

        createData(ProfilingData.TYPE_CHECKPOINT, extraValues);
    }
}
