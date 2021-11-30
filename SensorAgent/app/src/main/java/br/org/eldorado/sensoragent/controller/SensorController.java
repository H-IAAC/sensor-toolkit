package br.org.eldorado.sensoragent.controller;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import br.org.eldorado.sensoragent.ISensorAgentListener;
import br.org.eldorado.sensoragent.apiserver.APICommand;
import br.org.eldorado.sensoragent.apiserver.APIController;
import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensoragent.util.Log;

public class SensorController {

    private static final int LISTENER_SENSOR_STARTED = 0;
    private static final int LISTENER_SENSOR_STOPPED = 1;
    private static final String TAG = "SensorController";
    private Log log;
    private Map<Integer, SensorBase> sensorMap;
    private static SensorController inst;
    private RemoteCallbackList<ISensorAgentListener> mListener;

    public static SensorController getInstance() {
        if (inst == null) {
            inst = new SensorController();
        }
        return inst;
    }

    private SensorController() {
        this.sensorMap = new HashMap<Integer, SensorBase>();
        this.log = new Log(TAG);
    }

    public void setListener(RemoteCallbackList<ISensorAgentListener> l) {
        mListener = l;
    }

    public void addSensor(int sensorType) {
        if (!sensorMap.containsKey(sensorType)) {
            try {
                SensorBase sensor;
                switch (sensorType) {
                    case SensorBase.TYPE_ACCELEROMETER:
                        sensor = new Accelerometer();
                        break;
                    case SensorBase.TYPE_AMBIENT_TEMPERATURE:
                        sensor = new AmbientTemperature();
                        break;
                    case SensorBase.TYPE_GYROSCOPE:
                        sensor = new Gyroscope();
                        break;
                    case SensorBase.TYPE_LIGHT:
                        sensor = new Luminosity();
                        break;
                    case SensorBase.TYPE_PROXIMITY:
                        sensor = new Proximity();
                        break;
                    case SensorBase.TYPE_MAGNETIC_FIELD:
                        sensor = new MagneticField();
                        break;
                    default:
                        sensor = new SensorBase("UNKNOWN", -1);
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

    private void notifyAndroidClients(int type, int sensorType) throws RemoteException {
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

    public SensorBase getInformation(int sensorType) {
        if (sensorMap.containsKey(sensorType)) {
            return sensorMap.get(sensorType);
        }
        return null;
    }
}
