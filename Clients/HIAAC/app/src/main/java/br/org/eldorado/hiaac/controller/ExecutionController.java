package br.org.eldorado.hiaac.controller;

import android.os.CountDownTimer;

import androidx.lifecycle.ViewModelProvider;

import br.org.eldorado.hiaac.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.data.LabeledData;
import br.org.eldorado.hiaac.data.SensorFrequency;
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
    private CountDownTimer timer;
    private LabelConfigViewModel dbView;

    public static ExecutionController getInstance() {
        if (inst == null) {
            inst = new ExecutionController();
        }
        return inst;
    }

    public LabelConfigViewModel getDBModel() {
        return dbView;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setListener(ExecutionServiceListener lst) {
        this.listener = lst;
    }

    public void startExecution(DataTrack dataTrack) {
        try {
            if (!isRunning) {
                for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                    sensorFrequency.sensor.setFrequency(sensorFrequency.frequency);
                    //sensorFrequency.sensor.setFrequency(50);
                    sensorFrequency.sensor.registerListener(new MySensorListener(dataTrack));
                    sensorFrequency.sensor.startSensor();
                }
                setExecutionTimer(dataTrack);
                isRunning = true;
                listener.onStarted();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setService(ExecutionService svr) {
        this.service = svr;
        this.dbView = ViewModelProvider.AndroidViewModelFactory.getInstance(
                service.getApplication()).create(LabelConfigViewModel.class);
    }

    public void stopExecution(DataTrack dataTrack) {
        if (isRunning) {
            isRunning = false;
            timer.cancel();
            for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                sensorFrequency.sensor.stopSensor();
            }
            listener.onStopped();
            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                service = null;
            }
        }
    }

    private ExecutionController() {
        this.log = new Log(TAG);
        this.isRunning = false;
    }

    private void setExecutionTimer(DataTrack dataTrack) {
        if (!isRunning) {
            timer = new CountDownTimer(dataTrack.getStopTime() * 1000, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    fireExecutionListener(TYPE_TICK, millisUntilFinished);
                }

                @Override
                public void onFinish() {
                    service.stopExecution();
                }
            }.start();
        }
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
        public void onSensorStarted(SensorBase sensor) {
            log.d("Sensor STARTED");
        }

        @Override
        public void onSensorStopped(SensorBase sensor) {
            log.d("Sensor STOPED");
        }

        private int num = 1;
        @Override
        public void onSensorChanged(SensorBase sensor) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    log.d(dataTrack.getLabel() + " " + num++ + " - " + sensor.toString());
                    LabeledData labeledData = new LabeledData(dataTrack.getLabel(), sensor);
                    dbView.insertLabeledData(labeledData);
                }
            }).start();
        }
    }
}
