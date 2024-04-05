package br.org.eldorado.hiaac.datacollector.service;

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
import br.org.eldorado.sensorsdk.SensorSDK;

public class ExecutionService extends Service {

    private static final String TAG = "ExecutionService";
    private Log log;
    private IBinder mBinder = new MyBinder();
    private DataTrack dataTrack;

    private PowerManager.WakeLock wakeLock;

    public class MyBinder extends Binder {
        public ExecutionService getServer() {
            return ExecutionService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log = new Log(TAG);
        initService();
        return super.onStartCommand(intent, flags, startId);
    }

    private void initService() {
        log.d("initService");
        //setRemoteTime(System.currentTimeMillis());
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = createNotificationChannel(notificationManager);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationManager.IMPORTANCE_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build();
        startForeground(9667, notification);
    }

    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "hiaac_service_channelid";
        String channelName = "HIAAC Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public DataTrack isRunning() {
        return dataTrack;
    }

    public void startExecution(ExecutionServiceListener l) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DataCollector::DataCollectorWakeLock");
        wakeLock.acquire();
        ExecutionController ctrl = ExecutionController.getInstance();
        log.d("startExecution: " + (l.getDataTrack().equals(this.dataTrack)));
        if (!ctrl.isRunning() || l.getDataTrack().equals(this.dataTrack)) {
            this.dataTrack = l.getDataTrack();

            // Each execution must have an unique identifier
            this.dataTrack.setUid(df.format(new Date(System.currentTimeMillis())));

            ctrl.setService(this);
            ctrl.setListener(l);
            ctrl.startExecution(dataTrack);
        }
    }

    public void changeExecutionServiceListener(ExecutionServiceListener l) {
        ExecutionController.getInstance().setListener(l);
    }

    public void setRemoteTime(long time) {
        SensorSDK.getInstance().setRemoteTime(time);
    }

    public void stopExecution() {
        wakeLock.release();
        if (dataTrack != null) {
            ExecutionController.getInstance().stopExecution(dataTrack);
            sendNotification(dataTrack);
            dataTrack = null;
            //setRemoteTime(System.currentTimeMillis());
            stopForeground(true);
        }
    }

    private void sendNotification(DataTrack dt) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(getString(R.string.experiment_finished_title))
                        .setContentText(getString(R.string.experiment_finished_description, dt.getLabel()));


        Intent notificationIntent = new Intent(this, DataCollectorActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);
        builder.setLights(Color.BLUE, 500, 500);
        long[] pattern = {500,500,500,500,500,500,500,500,500};
        builder.setVibrate(pattern);
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(alarmSound);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "experiment_finished";
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Experiment " + dt.getLabel() + " is finished!",
                    NotificationManager.IMPORTANCE_HIGH);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.createNotificationChannel(channel);
            builder.setChannelId(channelId);
        }

        // Add as notification
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }
}
