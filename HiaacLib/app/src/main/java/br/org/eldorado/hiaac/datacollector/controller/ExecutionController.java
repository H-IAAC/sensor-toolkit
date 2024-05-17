package br.org.eldorado.hiaac.datacollector.controller;

import android.os.CountDownTimer;

import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import br.org.eldorado.hiaac.datacollector.data.ExperimentStatistics;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.ExecutionService;
import br.org.eldorado.hiaac.datacollector.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensorsdk.listener.SensorSDKListener;

public class ExecutionController {

    private static final int TYPE_STARTED = 1;
    private static final int TYPE_STOPPED = 2;
    private static final int TYPE_TICK = 3;
    private static ExecutionController inst;
    private final Log log = new Log("ExecutionController");
    private static boolean isRunning;
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

    public static boolean isRunning() {
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
                setAsRunning();

                listener.onStarted();
            }
        } catch (Exception e) {
            setAsNotRunning();
            listener.onError(e.getMessage());
            log.d("startExecution Exception: " + e.getMessage());
        }
    }

    public void setService(ExecutionService svr) {
        this.service = svr;
        this.dbView = ViewModelProvider.AndroidViewModelFactory.getInstance(
                service.getApplication()).create(LabelConfigViewModel.class);
    }

    private ExperimentStatistics getExperimentStatistics(DataTrack dataTrack, SensorFrequency sensorFrequency) {
        ExperimentStatistics st = new ExperimentStatistics();
        st.setConfigId(dataTrack.getConfigId());
        st.setSensorName(sensorFrequency.sensor.getName());
        st.setSensorFrequency(sensorFrequency.sensor.getFrequency());
        st.setStartTime(((MySensorListener) sensorFrequency.sensor.getListener()).getStartTime());
        st.setEndTime(((MySensorListener) sensorFrequency.sensor.getListener()).getEndTime());
        st.setCollectedData(((MySensorListener) sensorFrequency.sensor.getListener()).getCollectedData());
        st.setInvalidData(((MySensorListener) sensorFrequency.sensor.getListener()).getInvalidData());
        st.setTimestampAverage(((MySensorListener) sensorFrequency.sensor.getListener()).getTimestampAverage());
        st.setMaxTimestampDifference(((MySensorListener) sensorFrequency.sensor.getListener()).getMaxTimestampDifference());
        st.setMinTimestampDifference(((MySensorListener) sensorFrequency.sensor.getListener()).getMinTimestampDifference());
        st.setTimestampStandardVariation(0);
        st.setUsingServerTime(dataTrack.isUsingServerTime());
        st.setServerTimeDiffFromLocal(dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime());

        log.d("Total data collected from " + sensorFrequency.sensor.getName() + ": " + ((MySensorListener) sensorFrequency.sensor.getListener()).getTotalData());
        log.d("\tValid data from " + sensorFrequency.sensor.getName() + ": " + ((MySensorListener) sensorFrequency.sensor.getListener()).getCollectedData());
        log.d("\tInvalid data from " + sensorFrequency.sensor.getName() + ": " + ((MySensorListener) sensorFrequency.sensor.getListener()).getInvalidData());

        return st;
    }

    public void stopExecution(DataTrack dataTrack) {
        if (isRunning) {
            timer.cancel();
            List<ExperimentStatistics> statistics = new ArrayList<ExperimentStatistics>();
            for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                sensorFrequency.sensor.stopSensor();

                if (sensorFrequency.sensor.getListener() != null) {
                    statistics.add(getExperimentStatistics(dataTrack, sensorFrequency));
                    dbView.insertLabeledData(((MySensorListener) sensorFrequency.sensor.getListener()).getLabeledDataList());
                }
            }
            dbView.insertExperimentStatistics(statistics);
            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                service = null;
            }

            //dbView.insertLabeledData(labeledDataList);
            listener.onStopped();

            setAsNotRunning();
        }
    }

    private void setAsRunning() {
        isRunning = true;
        TimeSync.stopServerTimeUpdates();
    }

    private void setAsNotRunning() {
        isRunning = false;
        TimeSync.startServerTimeUpdates();
    }

    private ExecutionController() {
        setAsNotRunning();
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

        private final DataTrack dataTrack;
        private final ArrayList<LabeledData> labeledData;
        private final long startTime;
        private long endTime, timestampAverage, lastTimestamp, maxTimestampDifference, minTimestampDifference;
        private long collectedData = 0;
        private long invalidData = 0;
        // Valid + Invalid data
        private long totalData = 0;

        public MySensorListener(DataTrack data) {
            this.dataTrack = data;
            this.labeledData = new ArrayList<LabeledData>(50000);
            this.startTime = System.currentTimeMillis();
            this.timestampAverage = 0;
            this.lastTimestamp = 0;
            this.maxTimestampDifference = 0;
            this.minTimestampDifference = Long.MAX_VALUE;
        }

        public List<LabeledData> getLabeledDataList() {
            return labeledData;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getEndTime() {
            return endTime;
        }

        public long getInvalidData() {
            return invalidData;
        }

        public long getTotalData() {
            return totalData;
        }

        public long getCollectedData() {
            return collectedData;
        }

        public long getTimestampAverage() {
            return timestampAverage;
        }

        public long getMaxTimestampDifference() {
            return maxTimestampDifference;
        }

        public long getMinTimestampDifference() {
            return minTimestampDifference;
        }

        @Override
        public void onSensorStarted(SensorBase sensor) {
            log.d(sensor.getName() + " sensor STARTED");
        }
        @Override
        public void onSensorStopped(SensorBase sensor) {
            log.d(sensor.getName() + " sensor STOPPED");
            this.endTime = System.currentTimeMillis();
            this.timestampAverage = (collectedData < 2 ? 0 : timestampAverage/(collectedData-1)) ;
        }

        private long calcServerTime(long localTime) {
            // If sever time is ahead of local time
            if (this.dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime() > 0)
                // the local time must be incremented by this difference
                return localTime + this.dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime();
            else
                // otherwise, decrease the time difference
                return localTime - Math.abs(this.dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime());
        }

        @Override
        public void onSensorChanged(SensorBase sensor) {
            try {
                if (lastTimestamp == sensor.getTimestamp()) return;

                long localTime = sensor.getTimestamp();
                long serverTime = localTime;

                // Now, if is using server time, we need to calculate set the diff time
                if (this.dataTrack.isUsingServerTime())
                    serverTime = calcServerTime(localTime);

                if (collectedData > 0) {
                    // Ignore 'timestampAverage' when checking the first collectedData
                    if (lastTimestamp != 0)
                        timestampAverage += (localTime - lastTimestamp);

                    maxTimestampDifference = Math.max((localTime - lastTimestamp), maxTimestampDifference);
                    minTimestampDifference = Math.min((localTime - lastTimestamp), minTimestampDifference);
                }
                lastTimestamp = localTime;

                LabeledData data = new LabeledData(dataTrack.getLabel(),
                                                   sensor,
                                                   dataTrack.getDeviceLocation(),
                                                   dataTrack.getUserId(),
                                                   dataTrack.getActivity(),
                                                   dataTrack.getConfigId(),
                                                   serverTime,
                                                   localTime,
                                                   dataTrack.getUid());
                totalData++;
                labeledData.add(data);
                collectedData++;

                if (labeledData.size() >= 50000) {
                    log.d("Collected data so far for " + dataTrack.getLabel() + " - " + sensor.getName() + "\n\tValid: " + collectedData + "\n\tInvalid: " + invalidData + "\n\tAverage: " + (timestampAverage/collectedData));
                    dbView.insertLabeledData((ArrayList<LabeledData>)labeledData.clone());
                    labeledData.clear();
                }
                if (!sensor.isValidValues()) {
                    data.setValidData(false);
                    invalidData++;
                }
            } catch (Exception e) {
                if (labeledData.size() > 0) {
                    dbView.insertLabeledData(labeledData);
                    labeledData.clear();
                }
                log.d(sensor.getName() + " OnSensorChanged error: " + e.getMessage());
            }
        }
    }
}
