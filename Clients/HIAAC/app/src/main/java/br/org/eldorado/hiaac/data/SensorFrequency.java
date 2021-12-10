package br.org.eldorado.hiaac.data;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import br.org.eldorado.sensoragent.model.SensorBase;

@Entity
public class SensorFrequency {
    public SensorFrequency(@NonNull String label, @NonNull SensorBase sensor, int frequency) {
        this.label = label;
        this.sensor = sensor;
        this.frequency = frequency;
    }

    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String label;

    @NonNull
    public SensorBase sensor;

    @NonNull
    public int frequency;
}
