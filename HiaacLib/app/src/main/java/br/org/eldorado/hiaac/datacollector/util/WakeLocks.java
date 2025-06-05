package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.os.PowerManager;

public class WakeLocks {

    private static PowerManager powerManager;
    private static PowerManager.WakeLock collectWakeLock;
    private static PowerManager.WakeLock executionWakeLock;

    public static void collectAcquire(Context context) {
        powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        collectWakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                                                   "HIAACApp::WakeLock");
        collectWakeLock.acquire();
    }

    public static void collectRelease() {
        if (collectWakeLock != null && collectWakeLock.isHeld())
            collectWakeLock.release();
    }

    public static void executionAcquire(Context context) {
        powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        executionWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                     "DataCollector::DataCollectorWakeLock");
        executionWakeLock.acquire();
     }

    public static void executionRelease() {
        if (executionWakeLock != null & executionWakeLock.isHeld())
            executionWakeLock.release();
    }

}
