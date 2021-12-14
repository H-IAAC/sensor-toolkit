package br.org.eldorado.sensorsdk.controller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

import br.org.eldorado.sensoragent.ISensorAgentListener;
import br.org.eldorado.sensoragent.model.ISensorAgent;
import br.org.eldorado.sensorsdk.SensorSDKContext;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensorsdk.util.Log;


public class SensorController {

    private static final String TAG = "SensorController";
    private Log log;
    private Map<Integer, SensorBase> sensorMap;
    private static SensorController inst;
    private ISensorAgent sensorAgent;
    private Context mContext;
    private ServiceConnection mServiceConnection;
    private SensorAgentListener listener;

    public static SensorController getInstance() {
        if (inst == null) {
            inst = new SensorController();
        }
        return inst;
    }

    private SensorController() {
        this.sensorMap = new HashMap<Integer, SensorBase>();
        this.listener = new SensorAgentListener();
        this.log = new Log(TAG);
        this.initController();
    }

    private void initController() {
        try {
            log.i("initController");
            mContext = SensorSDKContext.getInstance().getContext();
            mServiceConnection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    try {
                        log.i("onServiceConnected");
                        sensorAgent = ISensorAgent.Stub.asInterface(service);
                        service.linkToDeath(mDeathRecipient,0);
                        sensorAgent.registerListener(listener);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                    log.i("onServiceDisconnected");
                    sensorAgent = null;
                }
            };
            bindService();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void bindService() {
        Intent intent = new Intent("br.org.eldorado.sensoragent.SENSOR_AGENT");
        ComponentName n = new ComponentName("br.org.eldorado.sensoragent", "br.org.eldorado.sensoragent.service.SensorAgentService");
        intent.setComponent(n);
        intent.setPackage("br.org.eldorado.sensoragent");
        mContext.startForegroundService(intent);
        boolean b = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        log.i("bind: " + b);
    }

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            if(sensorAgent != null){
                //Unexpected death on Server side
                try {
                    sensorAgent.unregisterListener(listener);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                sensorAgent.asBinder().unlinkToDeath(mDeathRecipient,0);
                sensorAgent = null;
                /* Bind the service again */
                bindService();
            }
        }
    };

    public void addSensor(SensorBase sensor) {
        //if (!sensorMap.containsKey(sensor.getType())) {
            sensorMap.put(sensor.getType(), sensor);
        //}
    }

    public void startSensor(SensorBase sensor) {
        try {
            log.i("Starting sensor " + sensor.getName());
            sensorAgent.startSensor(sensor.getType());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void stopSensor(SensorBase sensor) {
        try {
            log.i("Stopping sensor " + sensor.getName());
            sensorAgent.stopSensor(sensor.getType());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void getInformation(SensorBase sensor) {
        try {
            log.i("Getting information of " + sensor.getName());
            sensor.updateInformation(sensorAgent.getInformation(sensor.getType()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void startGettingInformationThread(SensorBase sensor) {
        if (!sensor.isStarted()) {
            sensor.setIsStarted(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    log.i("Starting getting information thread for " + sensor.getName() + " isSatrted: " + sensor.isStarted());
                    while (sensor.isStarted()) {
                        try {
                            getInformation(sensor);
                            Thread.sleep(1000/sensor.getFrequency());
                        } catch (InterruptedException e) {
                            sensor.setIsStarted(false);
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    class SensorAgentListener extends ISensorAgentListener.Stub {

        @Override
        public void onSensorStarted(int sensorType) throws RemoteException {
            SensorBase sensor = sensorMap.get(sensorType);
            if (sensor != null && sensor.getListener() != null) {
                try {
                    Thread.sleep(1000);
                    startGettingInformationThread(sensor);
                    sensor.getListener().onSensorStarted(sensor);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onSensorStopped(int sensorType) throws RemoteException {
            SensorBase sensor = sensorMap.get(sensorType);
            if (sensor != null && sensor.getListener() != null) {
                sensor.getListener().onSensorStopped(sensor);
            }
        }
    }
}
