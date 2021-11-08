// ISensorAgent.aidl
package br.org.eldorado.sensoragent.model;

import br.org.eldorado.sensoragent.IsensorAgentListener;
parcelable SensorBase;

interface ISensorAgent {

    void startSensor(int sensor);
    void stopSensor(int sensor);
    SensorBase getInformation(int sensor);

    /* Callbacks methods to notify the Android clients */
    void registerListener(ISensorAgentListener l);
    void unregisterListener(ISensorAgentListener l);
}