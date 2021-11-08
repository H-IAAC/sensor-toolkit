package br.org.eldorado.sensorapi.model;

public class Proximity extends SensorBase {

    private static final String TAG = "Proximity";

    public Proximity() {
        super(SensorBase.TYPE_PROXIMITY);
        setName(TAG);

    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValues()[0]);
        return sb.toString();
    }
}
