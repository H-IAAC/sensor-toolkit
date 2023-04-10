package br.org.eldorado.hiaac.datacollector.controller;

import android.os.CountDownTimer;
import androidx.lifecycle.ViewModelProvider;

import java.util.LinkedList;
import java.util.List;

import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.ExecutionService;
import br.org.eldorado.hiaac.datacollector.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.datacollector.util.Log;
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
            long totalData = 0;
            for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                sensorFrequency.sensor.stopSensor();
                //labeledDataList.addAll(((MySensorListener)sensorFrequency.sensor.getListener()).getLabeledDataList());
                if (sensorFrequency.sensor.getListener() != null) {
                    log.d("Collected data from " + sensorFrequency.sensor.getName() + ": " + ((MySensorListener) sensorFrequency.sensor.getListener()).getCollectedData());
                    log.d("Invalid data from " + sensorFrequency.sensor.getName() + ": " + ((MySensorListener) sensorFrequency.sensor.getListener()).getInvalidData());
                    dbView.insertLabeledData(((MySensorListener) sensorFrequency.sensor.getListener()).getLabeledDataList());
                    totalData += ((MySensorListener) sensorFrequency.sensor.getListener()).getCollectedData();
                }
            }
            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                service = null;
            }
            log.d("Total data collected: " + totalData);
            //dbView.insertLabeledData(labeledDataList);
            listener.onStopped();
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
        private LinkedList<LabeledData> labeledData;
        private long collectedData = 0;
        private long startTime, endTime;
        private long invalidData = 0;

        public MySensorListener(DataTrack data) {
            this.dataTrack = data;
            labeledData = new LinkedList<LabeledData>();
            this.startTime = System.currentTimeMillis();
        }

        public List<LabeledData> getLabeledDataList() {
            return labeledData == null ? new LinkedList<LabeledData>() : labeledData;
        }

        public long getInvalidData() {
            return invalidData;
        }

        public long getCollectedData() {
            return collectedData;
        }

        @Override
        public void onSensorStarted(SensorBase sensor) {
            log.d("Sensor STARTED");
        }

        @Override
        public void onSensorStopped(SensorBase sensor) {
            log.d("Sensor STOPED");
            this.endTime = System.currentTimeMillis();
        }

        @Override
        public void onSensorChanged(SensorBase sensor) {
            try {
                if (sensor.isValidValues()) {
                    //log.d(dataTrack.getLabel() + " Active Threads: " + Thread.activeCount() + "  - " + num++ + " - " + sensor.toString());
                    LabeledData data = new LabeledData(dataTrack.getLabel(), sensor, dataTrack.getDeviceLocation(), dataTrack.getUserId(), dataTrack.getActivity(), dataTrack.getLabelId());
                    labeledData.add(data);
                    collectedData++;

                    if (labeledData.size() > 50000) {
                        dbView.insertLabeledData((LinkedList<LabeledData>)labeledData.clone());
                        labeledData.clear();
                    }
                } else {
                    invalidData++;
                    log.d("Invalid Data Collected\n" + sensor.toString());
                }
            } catch (Exception e) {
                if (labeledData.size() > 0) {
                    dbView.insertLabeledData(labeledData);
                    labeledData.clear();
                }
                log.d("OnSensorChanged error");
                e.printStackTrace();
            }
        }
    }
}
