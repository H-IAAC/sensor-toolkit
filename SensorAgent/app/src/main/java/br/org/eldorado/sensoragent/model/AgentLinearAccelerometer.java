package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AgentLinearAccelerometer extends AgentSensorBase {
    private static final String TAG = "LinearAccelerometer";
    private Sensor sensor;

    public AgentLinearAccelerometer() {
        super(TAG, TYPE_LINEAR_ACCELEROMETER);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        registerListener(sensor);
        values = new float[3];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
    }
}
