package br.org.eldorado.hiaac.profiling;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private String csvFileName;
    private String extra;
    private Thread profilingThread;
    private String[] csvHeader = {"Timestamp", "Elapsed Time", "Used Memory from Application (bytes)", "Used System`s RAM (MB)",
            "Used System`s RAM (%)", "Used System`s CPU (%)", "Battery Level", "Profiling Type"};

    private Intent batteryStatus;

    protected ProfilingController() {
        initialTime = System.currentTimeMillis();
        isRunning = false;
        shouldFinish = false;
        data = new ArrayList<>(120);
        log = new Log(TAG);
        frequency = 1000;
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        setCsvFileName(df.format(System.currentTimeMillis()));
        extra = "";
    }

    protected void setContext(Context ctx) {
        mContext = ctx;

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = ctx.registerReceiver(null, ifilter);
    }

    protected void setFrequency(long t) {
        frequency = t;
    }

    protected void setCsvFileName(String str) {
        csvFileName = str;
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

    protected File finishProfiling() {
        log.d("Finishing profiling . . .");
        shouldFinish = true;
        checkPoint();
        stop();
        File f = createCSVFile();
        showData();
        data.clear();
        initialTime = -1;
        return f;
    }

    private File createCSVFile() {
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        "profiling");
        if (!directory.exists()) {
            directory.mkdir();
        }

        File csvFile = new File(
                directory.getAbsolutePath() +
                        File.separator +
                        csvFileName +
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
            writer.writeNext(csvHeader);
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (data) {
                    data.add(new ProfilingData(initialTime, mContext, type, batteryStatus, extra));
                }
            }
        }).start();
    }

    protected void checkPoint(String... extra) {
        this.extra = extra.length > 0 ? extra[0] : "";
        createData(ProfilingData.TYPE_CHECKPOINT);
    }
}
