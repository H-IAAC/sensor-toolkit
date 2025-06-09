package br.org.eldorado.hiaac.profiling;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import java.io.File;
import java.util.HashMap;

import br.org.eldorado.sensorsdk.SensorSDKContext;

public class Profiling {

    private Context mContext;
    private ProfilingController controller;
    private boolean isManualOnly;
    private static Profiling inst;
    private int frequency;
    private String csvFileName;


    public static Profiling getInstance() {
        if (inst == null) {
            inst = new Profiling(SensorSDKContext.getInstance().getContext());
        }
        return inst;
    }

    private Profiling(Context ctx) {
        mContext = ctx;
        isManualOnly = false;
        frequency = 1;
        csvFileName = "";
    }

    /**
     * If true, data will only be collected when 'checkPoint' method is called
     * If false, data will be collected every 'frequency' seconds
     * @param manualOnly
     */
    public void setManualOnly(boolean manualOnly) {
        isManualOnly = manualOnly;
    }

    public void setCsvFileName(String name) {
        csvFileName = name;
    }

    /**
     * Sets the frequency of the profiling collection in seconds
     * @param seconds Data will be collected every parameter seconds
     */
    public void setFrequency(int seconds) {
        if (frequency < 1 || frequency > 3600) {
            throw new IllegalArgumentException("Frequency should be a value between 1 and 3600");
        }
        frequency = seconds*1000;
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
        if (!csvFileName.isEmpty()) {
            controller.setCsvFileName(csvFileName);
        }
        File csv = controller.finishProfiling();
        controller = null;
        return csv;
    }
}
