package br.org.eldorado.sensormanager.util;

import br.org.eldorado.sensormanager.BuildConfig;

public class Log {
    private static final String MAIN_TAG = "SensorManager-";
    private String tag;

    public Log(String tag) {
        this.tag = tag;
    }

    public void d(String message) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(MAIN_TAG+tag, message);
        }
    }

    public void i(String message) {
        android.util.Log.i(MAIN_TAG+tag, message);
    }
}
