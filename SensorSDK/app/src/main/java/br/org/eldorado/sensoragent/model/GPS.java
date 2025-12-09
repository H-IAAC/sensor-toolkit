package br.org.eldorado.sensoragent.model;

public class GPS extends SensorBase {
    public static final String TAG = "GPS";

    public GPS() {
        super(TAG, SensorBase.TYPE_GPS);
        values = new float[6];
    }

    /**
     * 0 - Latitude
     * 1 - Longitude
     * 2 - Altitude
     * 3 - Speed
     * 4 - Horizontal Precision
     * 5 - Vertical Precision
     * @return
     */
    public String getValuesString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getValuesArray()[0]).append(",")
                .append(getValuesArray()[1]).append(",")
                .append(getValuesArray()[2]).append(",")
                .append(getValuesArray()[3]).append(",")
                .append(getValuesArray()[4]).append(",")
                .append(getValuesArray()[5]);
        return sb.toString();
    }

    public double getLatitude() {
        return values[0];
    }

    public double getLongitude() {
        return values[1];
    }

    public double getAltitude() {
        return values[2];
    }

    public double getSpeed() {
        return values[3];
    }

    public double getHorizontalPrecision() {
        return values[4];
    }

    public double getVerticalPrecision() {
        return values[5];
    }

    public boolean hasSpeed() {
        return getSpeed() != -1;
    }

    public boolean hasAltitude() {
        return getAltitude() != -1;
    }

    public boolean hasAccuracy() {
        return getHorizontalPrecision() != -1;
    }

    public boolean hasVerticalAccuracy() {
        return getVerticalPrecision() != -1;
    }
}
