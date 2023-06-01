package br.org.eldorado.sensoragent.model;

public class Luminosity extends SensorBase {

    public static final String TAG = "Luminosity";

    public Luminosity() {
        super(TAG, SensorBase.TYPE_LUMINOSITY);

    }

    public String getValuesString() {
        return String.valueOf(values[0]);
    }
}
