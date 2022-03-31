package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AgentAccelerometer extends AgentSensorBase {
    private static final String TAG = "Accelerometer";
    private Sensor sensor;

    public AgentAccelerometer() {
        super(TAG, TYPE_ACCELEROMETER);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        registerListener(sensor);
        values = new float[3];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
    }
}
