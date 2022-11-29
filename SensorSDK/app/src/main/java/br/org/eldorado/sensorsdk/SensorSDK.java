package br.org.eldorado.sensorsdk;

import android.app.Application;
import android.hardware.SensorManager;

import br.org.eldorado.sensoragent.util.RemoteApplicationTime;
import br.org.eldorado.sensorsdk.controller.SensorController;
import br.org.eldorado.sensorsdk.util.Log;

public class SensorSDK extends Application {

    private Log log;
    private static SensorSDK sdk;
    @Override
    public void onCreate() {
        super.onCreate();
        this.log = new Log("SensorSDK");
        this.initSDK();
    }

    public void initSDK() {
        log.i("initSDK");
        SensorSDKContext.getInstance().setContext(getApplicationContext());
        SensorController.getInstance();
        sdk = this;
    }

    public static SensorSDK getInstance() {
        return sdk;
    }

    public boolean checkSensorAvailability(int sensorType) {
        SensorManager sensorManager = (SensorManager) getSystemService(SensorSDKContext.getInstance().getContext().SENSOR_SERVICE);
        return sensorManager.getDefaultSensor(sensorType) != null;
    }

    public void setRemoteTime(long time) {
        RemoteApplicationTime.setCurrentTimeMillis(time);
    }
}
