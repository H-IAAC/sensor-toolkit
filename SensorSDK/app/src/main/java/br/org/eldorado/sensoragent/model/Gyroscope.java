package br.org.eldorado.sensoragent.model;

public class Gyroscope extends SensorBase {

    public static final String TAG = "Gyroscope";

    public Gyroscope() {
        super(TAG, SensorBase.TYPE_GYROSCOPE);
        values = new float[3];

    }

    public String getValuesString() {
        return getValuesArray()[0] + "," +
                getValuesArray()[1] + "," +
                getValuesArray()[2];
    }
}
