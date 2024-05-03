package br.org.eldorado.hiaac.datacollector.receiver;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.ExecutionService;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Utils;
import br.org.eldorado.hiaac.datacollector.util.WakeLocks;
import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;
import br.org.eldorado.hiaac.datacollector.view.adapter.LabelRecyclerViewAdapter;

import br.org.eldorado.sensorsdk.SensorSDK;

public class SchedulerReceiver extends BroadcastReceiver {
    private static final Log log = new Log("SchedulerReceiver");

    @Override
    public void onReceive(Context context, Intent intent) {

        log.d("SchedulerReceiver checking");

        if(intent == null || intent.getAction() == null)
            return;

        switch (intent.getAction()) {
            case AlarmConfig.SCHEDULER_ACTIONS:

                LabelRecyclerViewAdapter.ViewHolder holder = LabelRecyclerViewAdapter.getViewHolder(intent.getStringExtra("holder"));
                if (holder != null && !holder.isStarted()) {
                    log.d("SchedulerReceiver: Broadcast received");

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            // Disable alarm when it automatically starts after receive broadcast message
                            AlarmConfig.cancelAlarm();
                            AlarmConfig.releaseWakeLock();

                            WakeLocks.collectAcquire(context.getApplicationContext());

                            if (DataCollectorActivity.getAdapter() == null) {
                                log.d("SchedulerReceiver: Cant startExecution as adapter is null!!");
                                Utils.emitErrorBeep();
                            } else {
                                log.d("SchedulerReceiver: Broadcast received - startExecution");
                                DataCollectorActivity.getAdapter().startExecution(holder);
                            }
                        }
                    });

                } else {
                    log.d("SchedulerReceiver: Broadcast received invalid");
                }

                break;
            default:
                log.d("SchedulerReceiver: invalid action");
        }
    }

}
