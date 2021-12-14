package br.org.eldorado.sensoragent.model;

public class MagneticField extends SensorBase {
    public static final String TAG = "MagneticField";

    public MagneticField() {
        super(TAG, SensorBase.TYPE_MAGNETIC_FIELD);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X: ").append(getValuesArray()[0]).append(System.getProperty("line.separator"))
                .append(" Y: ").append(getValuesArray()[1]).append(System.getProperty("line.separator"))
                .append(" Z: ").append(getValuesArray()[2]);
        return sb.toString();
    }
}
