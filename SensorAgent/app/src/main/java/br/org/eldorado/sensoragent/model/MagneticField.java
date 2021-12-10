package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class MagneticField extends SensorBase {
    private static final String TAG = "MagneticField";
    private Sensor sensor;

    public MagneticField() {
        super(TAG, TYPE_MAGNETIC_FIELD);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        registerListener(sensor);
        values = new float[3];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
    }
}
