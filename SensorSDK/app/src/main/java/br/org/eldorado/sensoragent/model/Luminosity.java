package br.org.eldorado.sensoragent.model;

public class Luminosity extends SensorBase {

    private static final String TAG = "Luminosity";

    public Luminosity() {
        super(TAG, SensorBase.TYPE_LUMINOSITY);

    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(values[0]);
        return sb.toString();
    }
}
