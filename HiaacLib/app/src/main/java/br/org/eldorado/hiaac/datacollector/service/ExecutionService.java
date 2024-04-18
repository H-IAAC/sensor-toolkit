package br.org.eldorado.hiaac.datacollector.service;

import static br.org.eldorado.hiaac.datacollector.service.ForegroundNotification.NOTIFICATION_CHANNEL_ID;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.controller.ExecutionController;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.util.Utils;
import br.org.eldorado.sensorsdk.SensorSDK;

public class ExecutionService extends Service {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_CHECK_FOREGROUND_SERVICE = "ACTION_CHECK_FOREGROUND_SERVICE";
    public static final String ACTION_START_ANOTHER_FOREGROUND_SERVICE = "ACTION_START_ANOTHER_FOREGROUND_SERVICE";
    private static final String TAG = "ExecutionService";
    private Log log;
    private final IBinder mBinder = new MyBinder();
    private DataTrack dataTrack;
    private PowerManager.WakeLock wakeLock;

    public class MyBinder extends Binder {
        public ExecutionService getServer() {
            return ExecutionService.this;
        }
    }

    @Override
    public void onCreate() {
        log = new Log(TAG);
        ForegroundNotification.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (intent != null) {
            String nTitle = "H-IAAC";
            String action = intent.getAction();
            Bundle extras = intent.getExtras();

            if (extras != null)
                nTitle = extras.getString("Title");

            if (action != null) {
                switch (action) {
                    case ACTION_START_FOREGROUND_SERVICE:
                        startForeground(ForegroundNotification.NOTIFICATION_SERVICE_ID,
                                        ForegroundNotification.getNotification(getApplicationContext(), nTitle,  "Running..."));
                        break;
                    case ACTION_CHECK_FOREGROUND_SERVICE:
                        startForeground(ForegroundNotification.NOTIFICATION_SERVICE_ID,
                                        ForegroundNotification.getNotification(getApplicationContext(), nTitle, "Checking..."));
                        break;
                    case ACTION_START_ANOTHER_FOREGROUND_SERVICE:
                        startForeground(ForegroundNotification.NOTIFICATION_SERVICE_ID,
                                        ForegroundNotification.getNotification(getApplicationContext(), nTitle, "Another..."));
                        break;
                    default:
                        log.d("Invalid action");
                }
            } else {
                log.d("ExecutionService: onStartCommand no action to execute");
            }
        } else {
            log.d("ExecutionService: onStartCommand without intent");
        }

        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public DataTrack isRunning() {
        log.d("ExecutionService: isRunning() = " + dataTrack);
        return dataTrack;
    }

    public void startExecution(ExecutionServiceListener l) {
        log.d("ExecutionService: startExecution");

        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DataCollector::DataCollectorWakeLock");
        wakeLock.acquire();
        ExecutionController ctrl = ExecutionController.getInstance();
        log.d("ExecutionService: startExecution: " + (l.getDataTrack().equals(this.dataTrack)));
        if (!ctrl.isRunning() || l.getDataTrack().equals(this.dataTrack)) {
            this.dataTrack = l.getDataTrack();

            // Each execution must have an unique identifier
            this.dataTrack.setUid(df.format(new Date(System.currentTimeMillis())));

            ctrl.setService(this);
            ctrl.setListener(l);
            ctrl.startExecution(dataTrack);
        } else {
            if (!ctrl.isRunning())
                log.d("ExecutionService: startExecution failed - ExecutionController not running.");
            else
                log.d("ExecutionService: startExecution failed - Invalid DataTrack.");
        }
    }

    public void changeExecutionServiceListener(ExecutionServiceListener l) {
        ExecutionController.getInstance().setListener(l);
    }

    public void stopExecution() {
        wakeLock.release();

        if (dataTrack != null) {
            log.d("ExecutionService: stopForeground " +  dataTrack.getLabel());
            Utils.emitStopBeep();

            ExecutionController.getInstance().stopExecution(dataTrack);
            stopForeground(true);
            dataTrack = null;

            Preferences.setToRunChecking(true);
        }
    }
}
