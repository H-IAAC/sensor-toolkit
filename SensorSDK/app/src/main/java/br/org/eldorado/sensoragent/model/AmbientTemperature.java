package br.org.eldorado.sensoragent.model;

public class AmbientTemperature  extends SensorBase {
    public static final String TAG = "AmbientTemperature";

    public AmbientTemperature() {
        super(TAG, SensorBase.TYPE_AMBIENT_TEMPERATURE);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]);
        return sb.toString();
    }
}
