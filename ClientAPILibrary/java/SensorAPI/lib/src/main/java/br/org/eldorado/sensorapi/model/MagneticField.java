package br.org.eldorado.sensorapi.model;

public class MagneticField extends SensorBase {
    private static final String TAG = "MagneticField";

    public MagneticField() {
        super(SensorBase.TYPE_MAGNETIC_FIELD);
        setName(TAG);
    }

    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append("X: ").append(getValues()[0]).append(System.getProperty("line.separator"))
                .append(" Y: ").append(getValues()[1]).append(System.getProperty("line.separator"))
                .append(" Z: ").append(getValues()[2]);
        return sb.toString();
    }
}
