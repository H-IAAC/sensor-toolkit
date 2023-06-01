package br.org.eldorado.sensoragent.model;

public class Gravity extends SensorBase {
    public static final String TAG = "Gravity";

    public Gravity() {
        super(TAG, SensorBase.TYPE_GRAVITY);
        values = new float[3];
    }

    public String getValuesString() {
        return getValuesArray()[0] + "," +
                getValuesArray()[1] + "," +
                getValuesArray()[2];
    }
}
