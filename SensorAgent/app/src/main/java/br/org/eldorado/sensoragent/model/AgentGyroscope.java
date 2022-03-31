package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AgentGyroscope extends AgentSensorBase {
    private static final String TAG = "Gyroscope";
    private Sensor sensor;

    public AgentGyroscope() {
        super(TAG, TYPE_GYROSCOPE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        registerListener(sensor);
        values = new float[3];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
    }
}
