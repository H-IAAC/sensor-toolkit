package br.org.eldorado.hiaac.profiling;

import android.content.Context;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import br.org.eldorado.sensorsdk.SensorSDKContext;

public class Profiling {

    private Context mContext;
    private ProfilingController controller;
    private boolean isManualOnly;
    private static Profiling inst;
    private int frequency;
    private String csvPath;
    private String csvFileName;


    public static Profiling getInstance() {
        if (inst == null) {
            inst = new Profiling(SensorSDKContext.getInstance().getContext());
        }
        return inst;
    }

    public Profiling(Context ctx) {
        mContext = ctx;
        isManualOnly = false;
        frequency = 5000;
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        csvFileName = df.format(System.currentTimeMillis());
        csvPath = ctx.getFilesDir().getAbsolutePath() + File.separator + "profiling";
    }

    /**
     * If true, data will only be collected when 'checkPoint' method is called
     * If false, data will be collected every 'frequency' seconds
     * @param manualOnly
     */
    public void setManualOnly(boolean manualOnly) {
        isManualOnly = manualOnly;
    }

    public void setCsvFileName(String path, String filename) {
        csvPath = path;
        csvFileName = filename;
    }

    /**
     * Sets the frequency of the profiling collection in seconds
     * @param seconds Data will be collected every parameter seconds
     */
    public void setFrequency(int seconds) {
        if (seconds < 1 || seconds > 5) {
            throw new IllegalArgumentException("Frequency should be a value between 1 and 5 seconds");
        }
        frequency = seconds*1000;
    }

    /**
     * Sets the frequency of the profiling collection in milliseconds
     * @param milliseconds Data will be collected every parameter milliseconds
     */
    public void setFrequencyInMilliseconds(int milliseconds) {
        if (milliseconds < 1000 || milliseconds > 5000) {
            throw new IllegalArgumentException("Frequency should be a value between 1000 and 5000 milliseconds");
        }
        frequency = milliseconds;
    }

    public void start() {
        controller = new ProfilingController();
        controller.setContext(mContext);
        controller.setFrequency(frequency);
        if (!isManualOnly) {
            controller.start();
        }
    }

    public void checkPoint(HashMap<String, String> extra) {
        if (controller == null) {
            controller = new ProfilingController();
            controller.setContext(mContext);
        }
        controller.checkPoint(extra);
    }

    public File finishProfiling() {
        File csv = controller.finishProfiling(csvPath, csvFileName);
        controller = null;
        return csv;
    }
}
