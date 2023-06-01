package br.org.eldorado.sensoragent.model;

public class MagneticField extends SensorBase {
    public static final String TAG = "MagneticField";

    public MagneticField() {
        super(TAG, SensorBase.TYPE_MAGNETIC_FIELD);
        values = new float[3];
    }

    public String getValuesString() {
        return getValuesArray()[0] + "," +
                getValuesArray()[1] + "," +
                getValuesArray()[2];
    }
}
