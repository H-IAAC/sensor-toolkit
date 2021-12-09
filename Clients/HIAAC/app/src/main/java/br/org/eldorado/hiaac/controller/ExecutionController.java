package br.org.eldorado.hiaac.controller;

import android.os.CountDownTimer;

import java.util.Timer;
import java.util.TimerTask;

import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.service.ExecutionService;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.util.Log;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensorsdk.listener.SensorSDKListener;

public class ExecutionController {

    private static final int TYPE_STARTED = 1;
    private static final int TYPE_STOPPED = 2;
    private static final int TYPE_TICK = 3;

    private static final String TAG = "ExecutionController";
    private static ExecutionController inst;
    private Log log;
    private boolean isRunning;
    private ExecutionServiceListener listener;
    private ExecutionService service;

    public static ExecutionController getInstance() {
        if (inst == null) {
            inst = new ExecutionController();
        }
        return inst;
    }

    public void startExecution(DataTrack dataTrack, ExecutionServiceListener listener) {
        if (!isRunning) {
            isRunning = true;
            this.listener = listener;
            try {
                for (SensorBase sensor : dataTrack.getSensorList()) {
                    sensor.registerListener(new MySensorListener(dataTrack));
                    sensor.startSensor();
                }
                setExecutionTimer(dataTrack);
                listener.onStarted();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setService(ExecutionService svr) {
        this.service = svr;
    }

    public void stopExecution(DataTrack dataTrack) {
        if (isRunning) {
            isRunning = false;
            for (SensorBase sensor : dataTrack.getSensorList()) {
                sensor.stopSensor();
            }
            listener.onStopped();
            dataTrack = null;
            if (service != null) {
                service.stopForeground(true);
                service = null;
            }
        }
    }

    private ExecutionController() {
        this.log = new Log(TAG);
        this.isRunning = false;
    }

    private void setExecutionTimer(DataTrack dataTrack) {
        new CountDownTimer(dataTrack.getStopTime() * 1000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                fireExecutionListener(TYPE_TICK, millisUntilFinished);
            }

            @Override
            public void onFinish() {
                stopExecution(dataTrack);
            }
        }.start();
    }

    private void fireExecutionListener(int type, long remainingTime){
        if (listener != null) {
            switch (type) {
                case TYPE_STARTED:
                    listener.onStarted();
                    break;
                case TYPE_STOPPED:
                    listener.onStopped();
                    break;
                case TYPE_TICK:
                    listener.onRunning(remainingTime);
                    break;
            }
        }
    }

    private class MySensorListener implements SensorSDKListener {

        private DataTrack dataTrack;

        public MySensorListener(DataTrack data) {
            this.dataTrack = data;
        }

        @Override
        public void onSensorStarted(SensorBase sensor) {}

        @Override
        public void onSensorStopped(SensorBase sensor) {}

        @Override
        public void onSensorChanged(SensorBase sensor) {
            /* TODO Saves data to database */
            log.d(sensor.toString());
        }
    }
}
