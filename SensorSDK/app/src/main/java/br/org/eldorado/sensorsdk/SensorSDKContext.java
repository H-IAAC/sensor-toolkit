package br.org.eldorado.sensorsdk;

import android.content.Context;

public class SensorSDKContext {

    private static SensorSDKContext inst;
    private Context mContext;

    public static SensorSDKContext getInstance() {
        if (inst == null) {
            inst = new SensorSDKContext();
        }
        return inst;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context c) {
        this.mContext = c;
    }
}
