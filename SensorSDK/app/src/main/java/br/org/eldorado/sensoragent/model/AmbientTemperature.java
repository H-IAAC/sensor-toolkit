package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;

public class AmbientTemperature  extends SensorBase {
    public static final String TAG = "AmbientTemperature";

    public AmbientTemperature() {
        super(TAG, SensorBase.TYPE_AMBIENT_TEMPERATUR);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]).append("ºC");
        return sb.toString();
    }
}
