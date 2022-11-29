package br.org.eldorado.hiaac.profiling;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

class ProfilingData {

    protected static final String TYPE_CHECKPOINT = "CHECKPOINT";
    protected static final String TYPE_AUTOMATIC = "AUTOMATIC";

    private long currentTime;
    private long startTime;
    private Context mContext;
    private String type;

    private String ramMB;
    private String ramPercentage;
    private String usedMemory;
    private String batteryLevel;
    private String cpuUsage;

    private Intent battery;

    protected ProfilingData(long st, Context ctx, String tp, Intent bat) {
        battery = bat;
        type = tp;
        startTime = st;
        currentTime = System.currentTimeMillis();
        mContext = ctx;
        setRAMInfo();
        setBatteryLevel();
        setApplicationUsedMemory();
        setCpuUsage();
    }

    protected String getElapsedTime() {
        DateFormat dt = new SimpleDateFormat("HH:mm:ss.SSS");
        dt.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dt.format(currentTime-startTime);
    }

    protected String getTimestamp() {
        return  String.valueOf(currentTime);
    }

    protected String getRAMMB() {
        return ramMB;
    }

    protected String getRAMPercentage() {
        return ramPercentage;
    }

    protected String getUsedMemory() {
        return usedMemory;
    }

    protected String getBatteryLevel() {
        return batteryLevel;
    }

    protected String getCpuUsage() {
        return cpuUsage;
    }

    protected String getType() {
        return type;
    }

    protected String[] getCSVFormattedString() {
        String str[] = {getTimestamp(), getElapsedTime(), getUsedMemory(), getRAMMB(), getRAMPercentage(), getCpuUsage(),
                        getBatteryLevel(), getType()};
        return str;
    }

    private void setCpuUsage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                cpuUsage = String.valueOf(CpuInfo.getCpuUsageFromFreq());
            }
        }).start();
    }

    private void setBatteryLevel() {
        BatteryManager bm = (BatteryManager) mContext.getSystemService(Context.BATTERY_SERVICE);
        batteryLevel = String.valueOf(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
    }

    private void setApplicationUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        usedMemory = String.valueOf(runtime.totalMemory() - runtime.freeMemory());
    }

    private void setRAMInfo() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mContext.getSystemService(mContext.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        double usedMegs = (mi.totalMem - mi.availMem) / 0x100000L;

        double percentUsed = (mi.totalMem - mi.availMem) / (double)mi.totalMem * 100.0;
        ramMB = String.valueOf(usedMegs);
        ramPercentage = String.valueOf((int)percentUsed);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Elapsed Time: ").append(getElapsedTime()).append(", ")
                .append("Application Used Memory: ").append(getUsedMemory()).append(" bytes, ")
                .append("Used RAM Memory: ").append(getRAMMB()).append("MB, ")
                .append("Used RAM Memory: ").append(getRAMPercentage()).append("%, ")
                .append("Used CPU: ").append(getCpuUsage()).append("%, ")
                .append("Battery Level: ").append(getBatteryLevel());

        return sb.toString();
    }
}
