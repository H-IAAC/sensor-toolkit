package br.org.eldorado.hiaac.datacollector.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.eldorado.hiaac.audiocollector.AudioRecorder;
import br.org.eldorado.sensoragent.model.SensorBase;

@Entity
public class SensorFrequency {
    public SensorFrequency(@NonNull long config_id, @NonNull SensorBase sensor, int frequency, @NonNull Boolean isAudio) {
        this.config_id = config_id;
        this.sensor = sensor;
        this.frequency = frequency;
        this.isAudio = isAudio;
    }

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public long config_id;

    @NonNull
    private SensorBase sensor;

    @NonNull
    private int frequency;

    @NonNull
    private boolean isAudio;

    @NonNull
    public long getConfigId() {
        return config_id;
    }

    @NonNull
    @Override
    public String toString() {
        return id + " " + config_id + " " + sensor.getName() + " " + frequency;
    }

    public Boolean isAudio() {
        return isAudio;
    }

    public SensorBase getSensor() {
        return sensor;
    }

    public int getFrequency() {
        return frequency;
    }
}
