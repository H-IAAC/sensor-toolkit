package br.org.eldorado.sensoragent.model;

public class Gyroscope extends SensorBase {

    public static final String TAG = "Gyroscope";

    public Gyroscope() {
        super(TAG, SensorBase.TYPE_GYROSCOPE);
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
