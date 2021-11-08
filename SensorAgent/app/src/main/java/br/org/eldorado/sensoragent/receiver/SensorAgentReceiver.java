package br.org.eldorado.sensoragent.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import br.org.eldorado.sensoragent.service.SensorAgentService;
import br.org.eldorado.sensoragent.util.Log;

public class SensorAgentReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ComponentName n = context.startForegroundService(new Intent(context, SensorAgentService.class));
        new Log("SensorAgentReceiver").i("ComponentName: " + n);
    }
}
