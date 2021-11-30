package br.org.eldorado.sensoragent;

import android.content.Context;

public class SensorAgentContext {

    private Context context;
    private static SensorAgentContext inst;

    public static SensorAgentContext getInstance() {
        if (inst == null) {
            inst = new SensorAgentContext();
        }
        return inst;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context c) {
        context = c;
    }
}
