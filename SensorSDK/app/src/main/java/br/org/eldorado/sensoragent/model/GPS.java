package br.org.eldorado.sensoragent.model;

public class GPS extends SensorBase {
    public static final String TAG = "GPS";

    public GPS() {
        super(TAG, SensorBase.TYPE_GPS);
        values = new float[2];
    }

    public String getValuesString() {
        return getValuesArray()[0] + "," +
                getValuesArray()[1] + ",";
    }
}
