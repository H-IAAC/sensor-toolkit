package br.org.eldorado.sensorapi.model;

public class Accelerometer extends SensorBase {
    private static final String TAG = "Accelerometer";

    public Accelerometer() {
        super(SensorBase.TYPE_ACCELEROMETER);
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
