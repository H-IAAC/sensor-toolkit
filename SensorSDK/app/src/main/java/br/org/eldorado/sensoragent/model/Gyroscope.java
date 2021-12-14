package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class Gyroscope extends SensorBase {

    public static final String TAG = "Gyroscope";

    public Gyroscope() {
        super(TAG, SensorBase.TYPE_GYROSCOPE);

    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X: ").append(getValuesArray()[0]).append(System.getProperty("line.separator"))
                .append(" Y: ").append(getValuesArray()[1]).append(System.getProperty("line.separator"))
                .append(" Z: ").append(getValuesArray()[2]);
        return sb.toString();
    }
}
