package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

import br.org.eldorado.sensorsdk.listener.SensorSDKListener;

public class Accelerometer extends SensorBase {
    private static final String TAG = "Accelerometer";

    public Accelerometer() {
        super(TAG, SensorBase.TYPE_ACCELEROMETER);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X: ").append(getValuesArray()[0]).append(System.getProperty("line.separator"))
                .append(" Y: ").append(getValuesArray()[1]).append(System.getProperty("line.separator"))
                .append(" Z: ").append(getValuesArray()[2]);
        return sb.toString();
    }
}
