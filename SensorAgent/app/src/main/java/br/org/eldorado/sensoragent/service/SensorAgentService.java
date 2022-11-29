package br.org.eldorado.sensoragent.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import br.org.eldorado.sensoragent.ISensorAgentListener;
import br.org.eldorado.sensoragent.R;
import br.org.eldorado.sensoragent.SensorAgentContext;
import br.org.eldorado.sensoragent.model.AgentSensorBase;
import br.org.eldorado.sensoragent.model.ISensorAgent;
import br.org.eldorado.sensoragent.controller.SensorController;
import br.org.eldorado.sensoragent.util.Log;

public class SensorAgentService extends Service {

    private static final String TAG = "SensorAgentService";
    private Log log;
    private SensorAgentBind sensorBind = new SensorAgentBind();
    private RemoteCallbackList<ISensorAgentListener> mClientListener;

    @Override
    public void onCreate() {
        log = new Log(TAG);
        log.i("onCreate");
        super.onCreate();
        initService();
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

        startForeground(9669, notification);
        if (SensorAgentContext.getInstance().getContext() == null) {
            SensorAgentContext.getInstance().setContext(this);
        }
        //APIController.getInstance();
        mClientListener = new RemoteCallbackList<ISensorAgentListener>();
        SensorController.getInstance().setListener(mClientListener);
    }

    private String createNotificationChannel(NotificationManager notificationManager){
        String channelId = "sensoragent_service_channelid";
        String channelName = "Sensor Agent Foreground Service";
        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return channelId;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sensorBind;
    }

    public class SensorAgentBind extends ISensorAgent.Stub {

        @Override
        public void startSensor(int sensor) throws RemoteException {
            SensorController.getInstance().addSensor(sensor);
        }

        @Override
        public void stopSensor(int sensor) throws RemoteException {
            SensorController.getInstance().stopSensor(sensor);
        }

        @Override
        public AgentSensorBase getInformation(int sensor) throws RemoteException {
            return SensorController.getInstance().getInformation(sensor);
        }

        @Override
        public void registerListener(ISensorAgentListener l) throws RemoteException {
            mClientListener.register(l);
        }

        @Override
        public void unregisterListener(ISensorAgentListener l) throws RemoteException {
            mClientListener.unregister(l);
        }
    }
}
