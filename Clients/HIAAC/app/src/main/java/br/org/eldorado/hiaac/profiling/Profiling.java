package br.org.eldorado.hiaac.profiling;

import android.content.Context;
import java.io.File;
import br.org.eldorado.sensorsdk.SensorSDKContext;

public class Profiling {

    private Context mContext;
    private ProfilingController controller;
    private boolean isManualOnly;
    private static Profiling inst;

    public static Profiling getInstance() {
        if (inst == null) {
            inst = new Profiling(SensorSDKContext.getInstance().getContext());
        }
        return inst;
    }

    private Profiling(Context ctx) {
        mContext = ctx;
        isManualOnly = false;
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
        controller.setCsvFileName(name);
    }

    /**
     * Sets the frequency of the profiling collection in seconds
     * @param seconds Data will be collected every parameter seconds
     */
    public void setFrequency(int seconds) {
        controller.setFrequency(seconds*1000);
    }

    public void start() {
        controller = new ProfilingController();
        controller.setContext(mContext);
        if (!isManualOnly) {
            controller.start();
        }
    }

    public void checkPoint() {
        if (controller == null) {
            controller = new ProfilingController();
            controller.setContext(mContext);
        }
        controller.checkPoint();
    }

    public File finishProfiling() {
        File csv = controller.finishProfiling();
        controller = null;
        return csv;
    }
}
