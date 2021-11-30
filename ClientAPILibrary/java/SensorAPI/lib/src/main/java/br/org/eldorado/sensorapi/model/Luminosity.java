package br.org.eldorado.sensorapi.model;

public class Luminosity extends SensorBase {

    private static final String TAG = "Luminosity";

    public Luminosity() {
        super(SensorBase.TYPE_LUMINOSITY);
        setName(TAG);

    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValues()[0]);
        return sb.toString();
    }
}
