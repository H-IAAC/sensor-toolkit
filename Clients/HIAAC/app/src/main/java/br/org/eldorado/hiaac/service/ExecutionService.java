package br.org.eldorado.hiaac.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.provider.ContactsContract;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.controller.ExecutionController;
import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListenerAdapter;
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
        initService();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onCreate() {
        log = new Log(TAG);
        /*log.i("onCreate");
        super.onCreate();
        initService();*/
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
            ExecutionController.getInstance().stopExecution(dataTrack);
        }
    }
}
