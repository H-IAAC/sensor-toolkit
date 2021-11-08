package br.org.eldorado.sensoragent;

interface ISensorAgentListener {

    void onSensorStarted(int sensorType);
    void onSensorStoped(int sensorType);
}