package br.org.eldorado.sensoragent.model;

public class AgentGPS extends AgentSensorBase {

    private static final String TAG = "GPS";

    public AgentGPS() {
        super(TAG, TYPE_GPS);
        values = new float[2];
        values[0] = 0;
        values[1] = 0;
    }
}
