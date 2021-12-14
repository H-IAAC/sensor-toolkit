package br.org.eldorado.sensoragent.model;

public class Proximity extends SensorBase {

    public static final String TAG = "Proximity";

    public Proximity() {
        super(TAG, SensorBase.TYPE_PROXIMITY);

    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(values[0]);
        return sb.toString();
    }
}
