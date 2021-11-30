package br.org.eldorado.sensorapi.model;

public class Gyroscope extends SensorBase {

    private static final String TAG = "Gyroscope";

    public Gyroscope() {
        super(SensorBase.TYPE_GYROSCOPE);
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
