package br.org.eldorado.hiaac.datacollector.controller;

import android.os.CountDownTimer;
import android.os.Handler;

import androidx.lifecycle.ViewModelProvider;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.org.eldorado.hiaac.audiocollector.AudioRecorder;
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
    private Handler extraSensoryLoopHandler;

    public static ExecutionController getInstance() {
        if (inst == null) {
            inst = new ExecutionController();
        }
        return inst;
    }

    public void setExtraSensoryLoopHandler(Handler h) {
        if (extraSensoryLoopHandler != null) {
            extraSensoryLoopHandler.removeCallbacksAndMessages(null);
        }
        extraSensoryLoopHandler = h;
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

    public ExecutionServiceListener getListener() {
        return this.listener;
    }

    public void startExecution(DataTrack dataTrack) {
        try {
            if (!isRunning) {
                for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                    sensorFrequency.getSensor().setFrequency(sensorFrequency.getFrequency());
                    sensorFrequency.getSensor().registerListener(new MySensorListener(dataTrack));
                    sensorFrequency.getSensor().startSensor();
                }
                setExecutionTimer(dataTrack);
                setAsRunning();

                listener.onStarted();
            }
        } catch (Exception e) {
            setAsNotRunning();
            listener.onError(e.getMessage());
            e.printStackTrace();
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
        st.setSensorName(sensorFrequency.getSensor().getName());
        st.setSensorFrequency(sensorFrequency.getSensor().getFrequency());
        st.setStartTime(((MySensorListener) sensorFrequency.getSensor().getListener()).getStartTime());
        st.setEndTime(((MySensorListener) sensorFrequency.getSensor().getListener()).getEndTime());
        st.setCollectedData(((MySensorListener) sensorFrequency.getSensor().getListener()).getCollectedData());
        st.setInvalidData(((MySensorListener) sensorFrequency.getSensor().getListener()).getInvalidData());
        st.setTimestampAverage(((MySensorListener) sensorFrequency.getSensor().getListener()).getTimestampAverage());
        st.setMaxTimestampDifference(((MySensorListener) sensorFrequency.getSensor().getListener()).getMaxTimestampDifference());
        st.setMinTimestampDifference(((MySensorListener) sensorFrequency.getSensor().getListener()).getMinTimestampDifference());
        st.setTimestampStandardVariation(0);
        st.setUsingServerTime(dataTrack.isUsingServerTime());
        st.setServerTimeDiffFromLocal(dataTrack.getHowMuchServerTimeIsDifferentFromLocalTime());

        log.d("Total data collected from " + sensorFrequency.getSensor().getName() + ": " + ((MySensorListener) sensorFrequency.getSensor().getListener()).getTotalData());
        log.d("\tValid data from " + sensorFrequency.getSensor().getName() + ": " + ((MySensorListener) sensorFrequency.getSensor().getListener()).getCollectedData());
        log.d("\tInvalid data from " + sensorFrequency.getSensor().getName() + ": " + ((MySensorListener) sensorFrequency.getSensor().getListener()).getInvalidData());

        return st;
    }

    public void stopExecution(DataTrack dataTrack) {
        Map<Integer, List<LabeledData>> extraSensoryDataMap=null;
        ByteArrayInputStream extraSensoryAudioData = null;
        if (isRunning && dataTrack != null) {
            timer.cancel();
            List<ExperimentStatistics> statistics = new ArrayList<ExperimentStatistics>();
            extraSensoryDataMap = new HashMap<Integer, List<LabeledData>>();
            extraSensoryDataMap.put(SensorBase.TYPE_ACCELEROMETER, null);
            extraSensoryDataMap.put(SensorBase.TYPE_GYROSCOPE, null);
            extraSensoryDataMap.put(SensorBase.TYPE_MAGNETIC_FIELD, null);
            for (SensorFrequency sensorFrequency : dataTrack.getSensorList()) {
                sensorFrequency.getSensor().stopSensor();

                if (sensorFrequency.getSensor().getListener() != null) {
                    MySensorListener sensorListener = (MySensorListener) sensorFrequency.getSensor().getListener();
                    statistics.add(getExperimentStatistics(dataTrack, sensorFrequency));
                    dbView.insertLabeledData(((MySensorListener) sensorFrequency.getSensor().getListener()).getLabeledDataList());
                    if (dataTrack.isEldoradoProfile() ) {
                        if (sensorFrequency.isAudio()) {
                            extraSensoryAudioData = sensorListener.getExtraSensoryAudioData();
                        } else {
                            extraSensoryDataMap.compute(sensorFrequency.getSensor().getType(), (k,v) -> sensorListener.getExtraSensoryData());
                        }
                    }
                }
            }
            dbView.insertExperimentStatistics(statistics);

            if (service != null) {
                service.stopForeground(true);
                service.stopSelf();
                service = null;
            }
        }

        //dbView.insertLabeledData(labeledDataList);
        listener.onStopped();
        if (dataTrack!= null && dataTrack.isEldoradoProfile() && extraSensoryDataMap != null) {
            log.d("Collected data will be converted to ExtraSensory format " + extraSensoryDataMap);
            listener.onExtraSensoryConversion(extraSensoryDataMap, extraSensoryAudioData);
        }

        setAsNotRunning();
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
                    service.stopExecution(false);
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
        private List<LabeledData> extraSensoryData;

        private AudioRecorder audioRecorder;
        private ByteArrayInputStream audioData;

        public MySensorListener(DataTrack data) {
            this.dataTrack = data;
            this.labeledData = new ArrayList<LabeledData>(50000);
            this.startTime = System.currentTimeMillis();
            this.timestampAverage = 0;
            this.lastTimestamp = 0;
            this.maxTimestampDifference = 0;
            this.minTimestampDifference = Long.MAX_VALUE;
            if (dataTrack.isEldoradoProfile()) {
                extraSensoryData = new ArrayList<LabeledData>(1500);
            }
        }

        public List<LabeledData> getExtraSensoryData() {
            return extraSensoryData;
        }

        public ByteArrayInputStream getExtraSensoryAudioData() {
            return audioData;
        }

        public long getExpectedCollectedData(int frequency) {
            long expectedFinalTime = startTime + (dataTrack.getStopTime() * 1000);
            return ((expectedFinalTime - startTime) / 1000) * frequency;
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
            if (sensor.getType() == SensorBase.TYPE_AUDIO) {
                audioRecorder = new AudioRecorder(ExecutionController.getInstance().service);
            }
        }
        @Override
        public void onSensorStopped(SensorBase sensor) {
            log.d(sensor.getName() + " sensor STOPPED");
            this.endTime = System.currentTimeMillis();
            this.timestampAverage = (collectedData < 2 ? 0 : timestampAverage/(collectedData-1)) ;
            if (sensor.getType() == SensorBase.TYPE_AUDIO) {
                audioData = audioRecorder.stopRecord();
            }
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
//                if (lastTimestamp == sensor.getTimestamp()) {
//                    log.d("TESTE_TIMESTAMP Invalid - Same timestamp as last data");
//                    invalidData++;
//                    return;
//                }

                if (totalData >= getExpectedCollectedData(sensor.getFrequency())) {
                    return;
                }

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
                //lastTimestamp = localTime;

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
                if (!sensor.isValidValues() || lastTimestamp == sensor.getTimestamp()) {
                    data.setValidData(false);
                    invalidData++;
                }
                lastTimestamp = localTime;

                if (dataTrack.isEldoradoProfile() && data.isValidData()) {
                    extraSensoryData.add(data);
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
