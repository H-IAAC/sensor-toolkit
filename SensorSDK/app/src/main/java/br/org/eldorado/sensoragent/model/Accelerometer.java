package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

import br.org.eldorado.sensorsdk.listener.SensorSDKListener;

public class Accelerometer extends SensorBase {
    public static final String TAG = "Accelerometer";

    public Accelerometer() {
        super(TAG, SensorBase.TYPE_ACCELEROMETER);
        values = new float[3];
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]).append(",")
                .append(getValuesArray()[1]).append(",")
                .append(getValuesArray()[2]);
        return sb.toString();
    }
}
