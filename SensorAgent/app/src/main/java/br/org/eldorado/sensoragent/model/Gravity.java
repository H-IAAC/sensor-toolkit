package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class Gravity extends SensorBase {
    private static final String TAG = "Gravity";
    private Sensor sensor;

    public Gravity() {
        super(TAG, TYPE_GRAVITY);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        registerListener(sensor);
        values = new float[3];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
    }
}
