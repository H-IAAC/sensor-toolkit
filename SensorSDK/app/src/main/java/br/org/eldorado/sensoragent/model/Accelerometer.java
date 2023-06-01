package br.org.eldorado.sensoragent.model;

public class Accelerometer extends SensorBase {
    public static final String TAG = "Accelerometer";

    public Accelerometer() {
        super(TAG, SensorBase.TYPE_ACCELEROMETER);
        values = new float[3];
    }

    public String getValuesString() {
        return getValuesArray()[0] + "," +
                getValuesArray()[1] + "," +
                getValuesArray()[2];
    }
}
