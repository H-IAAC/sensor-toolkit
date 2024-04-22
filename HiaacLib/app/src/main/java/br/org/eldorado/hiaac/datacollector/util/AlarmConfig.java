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
import java.util.concurrent.TimeUnit;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.sensorsdk.SensorSDK;

public class AlarmConfig {
    private static final Log log = new Log("AlarmConfig");
    private static Context mContext = null;
    private static AlarmManager mgr = null;
    private static PowerManager powerManager = null;
    private static PowerManager.WakeLock wakeLock;
    private static boolean isConfigured = false;
    private static long idConfigured = -1;
    private static PendingIntent pendingAlarm = null;
    private static Boolean isInitialized = false;
    private static TextView schedulerView;

    public static void init(Context context, TextView view) {
        if (!isInitialized) {
            AlarmConfig.mContext = context;
            AlarmConfig.mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            AlarmConfig.powerManager = (PowerManager) mContext.getSystemService(POWER_SERVICE);
            AlarmConfig.wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                                                            "DataCollector::ScheduleWakeLock");
        }
        schedulerView = view;

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
        if (pendingAlarm != null) {
            mgr.cancel(pendingAlarm);
            setScheduler(null);
        }
        isConfigured = false;
        idConfigured = -1;
    }

    public static Date configureScheduler(LabelConfig labelConfig, String holderKey) {

        if (isConfigured && idConfigured != labelConfig.id) {
            return null;
        }

        Intent i = new Intent(DataCollectorActivity.SCHEDULER_ACTIONS);
        i.putExtra("holder", holderKey);
        i.putExtra("startTime", labelConfig.scheduledTime);

        pendingAlarm = PendingIntent.getBroadcast(mContext,
                                                  0,
                                                  i,
                                                  PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        if (labelConfig.scheduledTime > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(labelConfig.scheduledTime);

            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());

            // If configured to saved after the current date, then set the scheduler to next day
            if (!c.after(now))
                labelConfig.scheduledTime = labelConfig.scheduledTime + TimeUnit.HOURS.toMillis(24);

            acquireWakeLock();

            log.i("Scheduler: " + labelConfig.experiment + " to start at [" + c.getTime() + "] id: " + labelConfig.id);

            long startsTime = labelConfig.scheduledTime - SensorSDK.getInstance().getRemoteTime() - 7000;

            mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    SensorSDK.getInstance().getRemoteTime() + startsTime,
                    pendingAlarm);

            isConfigured = true;
            idConfigured = labelConfig.id;

            setScheduler(c.getTime());
            return c.getTime();
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
