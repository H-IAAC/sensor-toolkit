package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AmbientTemperature  extends SensorBase {
    private static final String TAG = "AmbientTemperature";
    private Sensor sensor;

    public AmbientTemperature() {
        super(TAG, TYPE_AMBIENT_TEMPERATURE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        registerListener(sensor);
        values = new float[1];
        values[0] = 20;
    }
}