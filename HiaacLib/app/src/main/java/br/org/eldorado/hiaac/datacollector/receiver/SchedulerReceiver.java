package br.org.eldorado.hiaac.datacollector.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.util.concurrent.locks.LockSupport;

import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.data.Repository;
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
                long configId = intent.getLongExtra("configId", 0L);
                long time = intent.getLongExtra("time", SensorSDK.getInstance().getRemoteTime());

                if (holder != null && !holder.isStarted()) {
                    log.d("SchedulerReceiver: Broadcast received");

                    new Handler().post(new Runnable() {
                        @Override
                        public void run() {
                            // Disable alarm when it automatically starts after receive broadcast message
                            AlarmConfig.cancelAlarm();
                            AlarmConfig.releaseWakeLock();

                            if (configId != 0L) {
                                LabelConfig labelConfig = Repository.getLabelConfigRepositoryInstance().getLabelConfig(configId);
                                labelConfig.scheduledTime = 0L;
                                Repository.getLabelConfigRepositoryInstance().updateConfig(labelConfig);
                            }

                            WakeLocks.collectAcquire(context.getApplicationContext());

                            if (DataCollectorActivity.getAdapter() == null) {
                                log.d("SchedulerReceiver: Cant startExecution as adapter is null!!");
                                Utils.emitErrorBeep();
                            } else {
                                while(SensorSDK.getInstance().getRemoteTime() < time) {
                                    LockSupport.parkNanos(10);
                                }
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
