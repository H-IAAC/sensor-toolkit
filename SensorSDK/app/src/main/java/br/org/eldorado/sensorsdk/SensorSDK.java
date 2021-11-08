package br.org.eldorado.sensorsdk;

import android.app.Application;

import br.org.eldorado.sensorsdk.controller.SensorController;
import br.org.eldorado.sensorsdk.util.Log;

public class SensorSDK extends Application {

    private Log log;
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
    }
}
