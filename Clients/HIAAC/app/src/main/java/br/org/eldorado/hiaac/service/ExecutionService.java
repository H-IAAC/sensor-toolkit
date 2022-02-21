package br.org.eldorado.hiaac.service;

import android.app.Application;
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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import br.org.eldorado.hiaac.MainActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.controller.ExecutionController;
import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.util.Log;

public class ExecutionService extends Service {

    private static final String TAG = "ExecutionService";
    private Log log;
    private IBinder mBinder = new MyBinder();
    private DataTrack dataTrack;

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
        ExecutionController ctrl = ExecutionController.getInstance();
        log.d("startExecution: " + (l.getDataTrack().equals(this.dataTrack)));
        if (!ctrl.isRunning() || l.getDataTrack().equals(this.dataTrack)) {
            this.dataTrack = l.getDataTrack();
            ctrl.setService(this);
            ctrl.setListener(l);
            ctrl.startExecution(dataTrack);
        }
    }

    public void stopExecution() {
        if (dataTrack != null) {
            sendNotification(dataTrack);
            ExecutionController.getInstance().stopExecution(dataTrack);
            dataTrack = null;
        }
    }

    private void sendNotification(DataTrack dt) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(getString(R.string.experiment_finished_title))
                        .setContentText(getString(R.string.experiment_finished_description, dt.getLabel()));


        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

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
