package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class Gravity extends SensorBase {
    private static final String TAG = "Gravity";
    private Sensor sensor;

    public Gravity() {
        super(TAG, TYPE_GRAVITY);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        registerListener(sensor);
    }
}
