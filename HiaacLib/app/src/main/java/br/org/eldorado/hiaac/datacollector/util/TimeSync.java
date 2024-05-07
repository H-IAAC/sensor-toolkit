package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
import br.org.eldorado.hiaac.datacollector.controller.ExecutionController;
import br.org.eldorado.sensorsdk.SensorSDK;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TimeSync {
    private static final Log log = new Log("TimeSync");
    private static boolean updateTimeInSync = false;
    private static final DateFormat df = new SimpleDateFormat("HH:mm:ss");
    private static final Handler syncServerTimeHandler = new Handler();
    private static final Handler updateTimeLabelHandler = new Handler();
    private static TextView serverTimeTxt;
    private static TextView timeDiffTxt;
    private static Context context;

    public static boolean isUsingServerTime() {
        return updateTimeInSync;
    }

    private static void syncServerTime() {
        Call<JsonObject> call = ClientAPI.get(ClientAPI.httpLowTimeout()).getServerTime();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                long timeInMillis = response.body().get("currentTimeMillis").getAsLong();
                SensorSDK.getInstance().setRemoteTime(
                        timeInMillis +(response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis())/2);
                updateTimeInSync = true;
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                updateTimeInSync = false;
                log.d("Get ServerTime failed at: " + Preferences.getPreferredServer() + " " + t.getCause());
                call.cancel();
            }
        });
    }

    public static void startServerTimeUpdates() {
        startServerTimeUpdates(TimeSync.serverTimeTxt, TimeSync.timeDiffTxt, TimeSync.context);
    }

    public static void startServerTimeUpdates(TextView serverTimeTxt, TextView timeDiffTxt, Context context) {
        TimeSync.serverTimeTxt = serverTimeTxt;
        TimeSync.timeDiffTxt = timeDiffTxt;
        TimeSync.context = context;

        if (ExecutionController.isRunning()) {
            setRemoteTimeText(0L, 0L, false, context);
            return;
        }

        log.d("TimeSync: startServerTimeUpdates");

        syncServerTimeHandler.postDelayed(new Runnable() {
            public void run() {
                syncServerTime();
                syncServerTimeHandler.postDelayed(this, 2000);
            }
        }, 2000);

        updateTimeLabelHandler.post(new Runnable() {
            public void run() {
                long remoteTime = SensorSDK.getInstance().getRemoteTime();
                long localTime = System.currentTimeMillis();
                setRemoteTimeText(remoteTime, localTime, true, context);
                updateTimeLabelHandler.postDelayed(this, 50);
            }
        });
    }

    public static void stopServerTimeUpdates() {
        log.d("TimeSync: stopServerTimeUpdates");

        setRemoteTimeText(0L, 0L, false, context);

        updateTimeInSync = false;
        syncServerTimeHandler.removeCallbacksAndMessages(null);
        updateTimeLabelHandler.removeCallbacksAndMessages(null);
    }

    public static long getTimestampDiffFromServerAndLocal() {
        long remote = SensorSDK.getInstance().getRemoteTime();
        long local = System.currentTimeMillis();
        return remote - local;
    }

    public static long getTimestampBasedOnDiffFromServer(long diff) {
            if (diff > 0)
                // the local time must be incremented by this difference
                return System.currentTimeMillis() + diff;
            else
                // otherwise, decrease the time difference
                return System.currentTimeMillis() - Math.abs(diff);
    }

    public static long getTimestamp() {
        if (isUsingServerTime()) {
            long diff = getTimestampDiffFromServerAndLocal();
            return getTimestampBasedOnDiffFromServer(diff);
        }

        return System.currentTimeMillis();
    }

    private static void setRemoteTimeText(long remoteTimeInMillis,
                                          long localTimeInMillis,
                                          final boolean timeIsRunning,
                                          Context context) {
        if (serverTimeTxt != null) {
            Date date = new Date(remoteTimeInMillis);
            String time = df.format(date);

            if (!timeIsRunning) {
                serverTimeTxt.setText("Time Sync");
                serverTimeTxt.setTextColor(Color.GRAY);
                timeDiffTxt.setText("Not Running");
                return;
            }

            if (updateTimeInSync) {
                serverTimeTxt.setText(context.getString(R.string.server_time) + " " + time);
                serverTimeTxt.setTextColor(Color.BLUE);
                timeDiffTxt.setText(context.getString(R.string.time_diff) + " " + (remoteTimeInMillis - localTimeInMillis) + "ms");
            } else {
                serverTimeTxt.setText(context.getString(R.string.local_time) + " " + time);
                serverTimeTxt.setTextColor(Color.GRAY);
                timeDiffTxt.setText("---");
            }
        }
    }
}
