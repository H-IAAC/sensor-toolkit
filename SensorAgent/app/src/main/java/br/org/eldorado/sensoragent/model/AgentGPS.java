package br.org.eldorado.sensoragent.model;

public class AgentGPS extends AgentSensorBase {

    private static final String TAG = "GPS";

    public AgentGPS() {
        super(TAG, TYPE_GPS);
        values = new float[6];
        values[0] = 0;
        values[1] = 0;
        values[2] = 0;
        values[3] = 0;
        values[4] = 0;
        values[5] = 0;
    }
}
