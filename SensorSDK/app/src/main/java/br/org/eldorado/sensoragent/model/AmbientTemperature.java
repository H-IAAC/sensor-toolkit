package br.org.eldorado.sensoragent.model;

public class AmbientTemperature  extends SensorBase {
    public static final String TAG = "AmbientTemperature";

    public AmbientTemperature() {
        super(TAG, SensorBase.TYPE_AMBIENT_TEMPERATUR);
    }

    public String getValuesString() {
        return String.valueOf(getValuesArray()[0]);
    }
}
