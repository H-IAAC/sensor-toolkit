package br.org.eldorado.hiaac.datacollector.util;

import static android.content.Context.POWER_SERVICE;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.sensorsdk.SensorSDK;

public class AlarmConfig {
    private static Log log = null;
    private static Context mContext = null;
    private static AlarmManager mgr = null;
    private static PowerManager powerManager = null;
    private static PowerManager.WakeLock wakeLock;
    private static boolean isConfigured = false;
    private static long idConfigured = 0;
    private static PendingIntent pendingAlarm = null;
    private static TextView schedulerView;
    private static Boolean isInitialized = false;

    public static void init(Context context, TextView schedulerView) {
        AlarmConfig.log = new Log("AlarmConfig");
        AlarmConfig.mContext = context;
        AlarmConfig.mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmConfig.powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
        AlarmConfig.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                        "DataCollector::ScheduleWakeLock");
        AlarmConfig.schedulerView = (TextView) schedulerView;
        isInitialized = true;
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void releaseWakeLock() {
        wakeLock.release();
    }

    public static void acquireWakeLock() {
        wakeLock.acquire();
    }

    public static void cancelAlarm() {
        if (pendingAlarm != null)
            mgr.cancel(pendingAlarm);

        isConfigured = false;
    }

    public static Date configureScheduler(LabelConfig labelConfig) {

        if (isConfigured && idConfigured != labelConfig.id) {
            return null;
        }

        Intent i = new Intent(DataCollectorActivity.SCHEDULER_ACTIONS);
        i.putExtra("holder", labelConfig.experiment);
        i.putExtra("startTime", labelConfig.scheduledTime);

        PendingIntent pi = PendingIntent.getBroadcast(mContext,
                0,
                i,
                PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (labelConfig.scheduledTime > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(labelConfig.scheduledTime);

            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());
            if (c.after(now)) {
                acquireWakeLock();

                log.i("Scheduler: " + labelConfig.experiment + " to start at [" + c.getTime() + "] id: " + labelConfig.id);

                long startsTime = labelConfig.scheduledTime - SensorSDK.getInstance().getRemoteTime() - 7000;
                mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        SensorSDK.getInstance().getRemoteTime() + startsTime,
                        pi);

                isConfigured = true;
                idConfigured = labelConfig.id;

                setScheduler(c.getTime());
                return c.getTime();
            }
        }

        setScheduler(null);
        return null;
    }

    private static void setScheduler(Date date) {
        if (schedulerView == null)
            return;

        if (date == null) {
            schedulerView.setText("");
        } else {
            DateFormat dt = new SimpleDateFormat("HH:mm:ss");
            schedulerView.setText("Scheduler: " + dt.format(date));
        }
    }
}
