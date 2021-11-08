package br.org.eldorado.sensorapi.model;

public class AmbientTemperature extends SensorBase {
    private static final String TAG = "AmbientTemperature";

    public AmbientTemperature() {
        super(SensorBase.TYPE_AMBIENT_TEMPERATUR);
        setName(TAG);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValues()[0]).append("ºC");
        return sb.toString();
    }
}
