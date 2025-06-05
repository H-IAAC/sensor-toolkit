package br.org.eldorado.sensoragent.model;

public class GPS extends SensorBase {
    public static final String TAG = "GPS";

    public GPS() {
        super(TAG, SensorBase.TYPE_GPS);
        values = new float[2];
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]).append(",")
                .append(getValuesArray()[1]).append(",");
        return sb.toString();
    }
}
