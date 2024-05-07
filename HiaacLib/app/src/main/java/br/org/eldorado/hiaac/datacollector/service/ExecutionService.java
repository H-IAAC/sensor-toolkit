package br.org.eldorado.hiaac.datacollector.service;

import android.app.Service;
import android.content.Intent;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import br.org.eldorado.hiaac.datacollector.controller.ExecutionController;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.hiaac.datacollector.util.Utils;
import br.org.eldorado.hiaac.datacollector.util.WakeLocks;

public class ExecutionService extends Service {
    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    public static final String ACTION_CHECK_FOREGROUND_SERVICE = "ACTION_CHECK_FOREGROUND_SERVICE";
    public static final String ACTION_START_ANOTHER_FOREGROUND_SERVICE = "ACTION_START_ANOTHER_FOREGROUND_SERVICE";
    private final Log log = new Log("ExecutionService");
    private final IBinder mBinder = new MyBinder();
    private DataTrack dataTrack;

    public class MyBinder extends Binder {
        public ExecutionService getServer() {
            return ExecutionService.this;
        }
    }

    @Override
    public void onCreate() {
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

        WakeLocks.executionAcquire(getApplicationContext());

        ExecutionController ctrl = ExecutionController.getInstance();
        log.d("ExecutionService: startExecution: " + (l.getDataTrack().equals(this.dataTrack)));
        if (!ctrl.isRunning() || l.getDataTrack().equals(this.dataTrack)) {
            this.dataTrack = l.getDataTrack();

            // Each execution must have an unique identifier
            this.dataTrack.setUid(df.format(new Date(System.currentTimeMillis())));

            // Set time diff from server to local clocks
            this.dataTrack.setHowMuchServerTimeIsDifferentFromLocalTime(TimeSync.getTimestampDiffFromServerAndLocal());

            // Set if is execution is considering the 'server time'
            this.dataTrack.setUsingServerTime(TimeSync.isUsingServerTime());
            log.d("ExecutionService: Considering time diff of " + dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime() + "ms");

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
        if (dataTrack != null) {
            log.d("ExecutionService: stopForeground " +  dataTrack.getLabel());
            Utils.emitStopBeep();

            ExecutionController.getInstance().stopExecution(dataTrack);
            stopForeground(true);
            dataTrack = null;

            Preferences.setToRunChecking(true);
        }

        WakeLocks.collectRelease();
        WakeLocks.executionRelease();
    }
}
