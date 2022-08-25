package br.org.eldorado.sensoragent.model;

public class Gravity extends SensorBase {
    public static final String TAG = "Gravity";

    public Gravity() {
        super(TAG, SensorBase.TYPE_GRAVITY);
        values = new float[3];
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]).append(",")
                .append(getValuesArray()[1]).append(",")
                .append(getValuesArray()[2]);
        return sb.toString();
    }
}
