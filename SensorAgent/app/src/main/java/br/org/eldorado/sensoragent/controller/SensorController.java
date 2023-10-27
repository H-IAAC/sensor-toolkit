package br.org.eldorado.sensoragent.controller;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import br.org.eldorado.sensoragent.ISensorAgentListener;
import br.org.eldorado.sensoragent.apiserver.APICommand;
import br.org.eldorado.sensoragent.apiserver.APIController;
import br.org.eldorado.sensoragent.model.AgentAccelerometer;
import br.org.eldorado.sensoragent.model.AgentAmbientTemperature;
import br.org.eldorado.sensoragent.model.AgentGPS;
import br.org.eldorado.sensoragent.model.AgentGravity;
import br.org.eldorado.sensoragent.model.AgentGyroscope;
import br.org.eldorado.sensoragent.model.AgentLinearAccelerometer;
import br.org.eldorado.sensoragent.model.AgentLuminosity;
import br.org.eldorado.sensoragent.model.AgentMagneticField;
import br.org.eldorado.sensoragent.model.AgentProximity;
import br.org.eldorado.sensoragent.model.AgentSensorBase;
import br.org.eldorado.sensoragent.util.Log;

public class SensorController {

    private static final int LISTENER_SENSOR_STARTED = 0;
    private static final int LISTENER_SENSOR_STOPPED = 1;
    private static final String TAG = "SensorController";
    private Log log;
    private Map<Integer, AgentSensorBase> sensorMap;
    private static SensorController inst;
    private RemoteCallbackList<ISensorAgentListener> mListener;

    public static SensorController getInstance() {
        if (inst == null) {
            inst = new SensorController();
        }
        return inst;
    }

    private SensorController() {
        this.sensorMap = new HashMap<Integer, AgentSensorBase>();
        this.log = new Log(TAG);
    }

    public void setListener(RemoteCallbackList<ISensorAgentListener> l) {
        mListener = l;
    }

    public void addSensor(int sensorType) {
        if (!sensorMap.containsKey(sensorType)) {
            try {
                AgentSensorBase sensor;
                switch (sensorType) {
                    case AgentSensorBase.TYPE_ACCELEROMETER:
                        sensor = new AgentAccelerometer();
                        break;
                    case AgentSensorBase.TYPE_LINEAR_ACCELEROMETER:
                        sensor = new AgentLinearAccelerometer();
                        break;
                    case AgentSensorBase.TYPE_AMBIENT_TEMPERATURE:
                        sensor = new AgentAmbientTemperature();
                        break;
                    case AgentSensorBase.TYPE_GYROSCOPE:
                        sensor = new AgentGyroscope();
                        break;
                    case AgentSensorBase.TYPE_LIGHT:
                        sensor = new AgentLuminosity();
                        break;
                    case AgentSensorBase.TYPE_PROXIMITY:
                        sensor = new AgentProximity();
                        break;
                    case AgentSensorBase.TYPE_MAGNETIC_FIELD:
                        sensor = new AgentMagneticField();
                        break;
                    case AgentSensorBase.TYPE_GRAVITY:
                        sensor = new AgentGravity();
                        break;
                    case AgentSensorBase.TYPE_GPS:
                        sensor = new AgentGPS();
                        break;
                    default:
                        sensor = new AgentSensorBase("UNKNOWN", -1);
                }
                sensorMap.put(sensorType, sensor);
                APICommand cmd = new APICommand(APICommand.CommandType.TYPE_SENSOR_STARTED, Arrays.asList(sensorType));
                APIController.getInstance().sendForAll(cmd);
                notifyAndroidClients(LISTENER_SENSOR_STARTED, sensorType);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopSensor(int sensorType) {
        if (sensorMap.containsKey(sensorType)) {
            try {
                sensorMap.get(sensorType).unregisterListener();
                sensorMap.remove(sensorType);
                APICommand cmd = new APICommand(APICommand.CommandType.TYPE_SENSOR_STOPPED, Arrays.asList(sensorType));
                APIController.getInstance().sendForAll(cmd);
                notifyAndroidClients(LISTENER_SENSOR_STOPPED, sensorType);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void notifyAndroidClients(int type, int sensorType) throws RemoteException {
        mListener.beginBroadcast();
        for(int i=0; i < mListener.getRegisteredCallbackCount(); i++){
            ISensorAgentListener list = mListener.getBroadcastItem(i);
            if (LISTENER_SENSOR_STARTED == type) {
                list.onSensorStarted(sensorType);
            } else if (LISTENER_SENSOR_STOPPED == type) {
                list.onSensorStopped(sensorType);
            }
        }
        mListener.finishBroadcast();
    }

    public AgentSensorBase getInformation(int sensorType) {
        if (sensorMap.containsKey(sensorType)) {
            return sensorMap.get(sensorType);
        }
        return null;
    }
}
