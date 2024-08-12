package br.org.eldorado.sensorsdk.controller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.Map;

import br.org.eldorado.sensoragent.ISensorAgentListener;
import br.org.eldorado.sensoragent.model.ISensorAgent;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensoragent.service.SensorAgentService;
import br.org.eldorado.sensorsdk.SensorSDKContext;
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
        ComponentName n = mContext.startForegroundService(new Intent(mContext, SensorAgentService.class));
        log.i("ComponentName: " + n);
        Intent intent = new Intent("br.org.eldorado.sensoragent.SENSOR_AGENT");
        //n = new ComponentName("br.org.eldorado.sensoragent", "br.org.eldorado.sensoragent.service.SensorAgentService");
        intent.setComponent(n);
        intent.setPackage("br.org.eldorado.sensoragent");
        //mContext.startForegroundService(intent);
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
            log.i("Starting sensor " + sensor.getName() + " type " + sensor.getType());
            addSensor(sensor);
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
            //log.i("Getting information of " + sensor.getName() + " " + sensorAgent + " Type " + sensor.getType());
            sensor.updateInformation(sensorAgent.getInformation(sensor.getType()));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void spinWait(long end) {
        long current = System.nanoTime();

        while (current < end) {
            // if current time is less 1ms from the 'end', then ignore sleep()
            if (!(current > (end - 1000000)))
                SystemClock.sleep(1);
            current = System.nanoTime();
        }
    }
    public void startGettingInformationThread(SensorBase sensor) {

        if (!sensor.isStarted()) {
            sensor.setIsStarted(true);
            long freqInMs = 1000/sensor.getFrequency();
            log.i("Starting getting information thread for " + sensor.getName() +
                                              " isStarted: " + sensor.isStarted() +
                                              " Frequency: " + sensor.getFrequency() +
                                              " Type " + sensor.getType());
            /*Handler handler = new Handler();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    //long end = System.currentTimeMillis() + freqInMs;
                    getInformation(sensor);
                    handler.postDelayed(this, freqInMs);
                }
            });*/

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Process.setThreadPriority(-20);
                    PowerManager powerManager = (PowerManager) mContext.getSystemService(mContext.POWER_SERVICE);
                    PowerManager.WakeLock executionWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                            "SensorController::StartGettingInformationThread");
                    executionWakeLock.acquire();
                    while (sensor.isStarted()) {
                        try {
                            long end = System.nanoTime() + (freqInMs * 1000000); // convert freqInMs to nanosec
                            getInformation(sensor);
                            spinWait(end);
                        } catch (Exception e) {
                            sensor.setIsStarted(false);
                            e.printStackTrace();
                        }
                    }
                    executionWakeLock.release();
                }
            }).start();
        }
    }

    class SensorAgentListener extends ISensorAgentListener.Stub {

        @Override
        public void onSensorStarted(int sensorType) throws RemoteException {
            SensorBase sensor = sensorMap.get(sensorType);
            log.d("onSensorStarted " + sensor.getName() + " " + sensor.getFrequency() + " Type " + sensor.getType());
            if (sensor != null && sensor.getListener() != null) {
                try {
                    //Thread.sleep(1000);
                    startGettingInformationThread(sensor);
                    sensor.getListener().onSensorStarted(sensor);
                } catch (/*InterruptedException*/ Exception e) {
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
