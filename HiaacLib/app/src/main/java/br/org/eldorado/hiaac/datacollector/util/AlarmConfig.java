package br.org.eldorado.hiaac.datacollector.util;

import static android.content.Context.POWER_SERVICE;

import static br.org.eldorado.hiaac.datacollector.util.TimeSync.isUsingServerTime;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.receiver.SchedulerReceiver;
import br.org.eldorado.sensorsdk.SensorSDK;

public class AlarmConfig {
    private static final Log log = new Log("AlarmConfig");
    private static Context mContext = null;
    private static AlarmManager mgr = null;
    private static PowerManager powerManager = null;
    private static PowerManager.WakeLock wakeLock;
    private static PendingIntent pendingAlarm = null;
    private static Boolean isInitialized = false;
    private static TextView schedulerView;
    private static Configuration configuration = new Configuration();
    public static final String SCHEDULER_ACTIONS = "br.org.eldorado.hiaac.datacollector.SCHEDULER";

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

    public static void releaseWakeLock() {
        wakeLock.release();
    }

    public static void acquireWakeLock() {
        wakeLock.acquire();
    }

    public static void cancelAlarm() {
        if (pendingAlarm != null) {
            pendingAlarm.cancel();
            mgr.cancel(pendingAlarm);
            setScheduler(null);
            log.i("Scheduler: " + configuration.experiment + " " + configuration.activity + "" + configuration.userId + " cancelled.");
        }

        configuration = new Configuration();
    }

    private static long getSchedulerTimeWithCorrectDate(long scheduledTime) {
        Calendar scheduledDate = Calendar.getInstance();
        scheduledDate.setTimeInMillis(scheduledTime);

        Calendar now = Calendar.getInstance();

        if (isUsingServerTime())
            //now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());
            now.setTimeInMillis(TimeSync.getTimestamp());

        // If configured to saved after the current date, then set the scheduler to next day
        if (now.after(scheduledDate))
            scheduledTime = scheduledTime + TimeUnit.HOURS.toMillis(24);

        return scheduledTime;
    }

    public static Date configureScheduler(LabelConfig labelConfig, String holderKey) {

        // Do not set the scheduler when device location is 'video'
        if (labelConfig.deviceLocation.toLowerCase().equals("video") ||
            labelConfig.deviceLocation.toLowerCase().equals("vídeo")) {
            if (configuration.idConfigured == labelConfig.id)
                cancelAlarm();

            return null;
        }

        if (labelConfig.scheduledTime == 0) {
            if (labelConfig.id == configuration.idConfigured)
                cancelAlarm();

            log.i("Scheduler: " + labelConfig.experiment + " has no scheduler configured.");
            return null;
        }

        long scheduledTime = getSchedulerTimeWithCorrectDate(labelConfig.scheduledTime);

        if (scheduledTime < configuration.scheduledTime) {
            log.i("Scheduler: " + labelConfig.experiment + " has priority over " + configuration.experiment);
        } else if (configuration.isConfigured &&
                   configuration.idConfigured != labelConfig.id) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(scheduledTime);
            log.i("Scheduler: Ignoring scheduler from " + labelConfig.experiment + " to " + c.getTime());
            return null;
        }

        // Set action and className to comply with the new Android security requirements...
        Intent i = new Intent(mContext, SchedulerReceiver.class);
        i.setAction(AlarmConfig.SCHEDULER_ACTIONS);
        i.putExtra("holder", holderKey);
        i.putExtra("configId", labelConfig.id);

        pendingAlarm = PendingIntent.getBroadcast(mContext,
                                                  0,
                                                  i,
                                                  PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        acquireWakeLock();

        long alarmStartTime = TimeSync.convertTime(scheduledTime);
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(alarmStartTime);
        log.i("Scheduler: " + labelConfig.experiment + " to start at [" + c.getTime() + "] id: " + labelConfig.id);

        mgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                alarmStartTime,
                pendingAlarm);

        configuration = new Configuration(true, labelConfig.id, alarmStartTime, labelConfig.experiment, labelConfig.activity, labelConfig.userId);

        setScheduler(c.getTime());
        return c.getTime();
    }

    public static Configuration getActiveConfigure() {
        return configuration;
    }

    private static void setScheduler(Date date) {
        if (schedulerView == null)
            return;

        if (date == null) {
            schedulerView.setText("");
        } else {
            DateFormat dt = new SimpleDateFormat("HH:mm:ss:SSS");
            schedulerView.setText("Scheduler: " + dt.format(date));
        }
    }

    public static class Configuration {
        public boolean isConfigured;
        public long idConfigured;
        public long scheduledTime;
        public String experiment;
        public String activity;
        public String userId;

        public Configuration() {
            this.isConfigured = false;
            this.idConfigured = -1;
            this.scheduledTime = 0;
            this.activity = "";
            this.userId = "";
            this.experiment = "";
        }

        public Configuration(boolean isConfigured, long idConfigured, long scheduledTime, String experiment, String activity, String userId) {
            this.isConfigured = isConfigured;
            this.idConfigured = idConfigured;
            this.scheduledTime = scheduledTime;
            this.experiment = experiment;
            this.activity = activity;
            this.userId = userId;
        }

    }
}
