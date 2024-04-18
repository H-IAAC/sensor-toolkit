package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.os.Handler;
import android.widget.TextView;

import com.google.gson.JsonObject;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
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

    public static void startServerTimeUpdates(TextView serverTimeTxt, Context context) {
        log.d("TimeSync: startServerTimeUpdates");
        updateTimeInSync = true;

        syncServerTimeHandler.postDelayed(new Runnable() {
            public void run() {
                syncServerTime();
                syncServerTimeHandler.postDelayed(this, 2000);
            }
        }, 2000);

        updateTimeLabelHandler.postDelayed(new Runnable() {
            public void run() {
                setRemoteTimeText(SensorSDK.getInstance().getRemoteTime(), serverTimeTxt, context);
                updateTimeLabelHandler.postDelayed(this, 50);
            }
        }, 50);
    }

    public static void stopServerTimeUpdates() {
        log.d("TimeSync: stopServerTimeUpdates");

        updateTimeInSync = false;
        syncServerTimeHandler.removeCallbacksAndMessages(null);
        updateTimeLabelHandler.removeCallbacksAndMessages(null);
    }

    private static void setRemoteTimeText(long timeInMillis, final TextView serverTimeTxt, Context context) {
        if (serverTimeTxt != null) {
            Date date = new Date(timeInMillis);
            String time = df.format(date);
/*            runOnUiThread(new Runnable() {
                @Override
                public void run() {*/
                    if (updateTimeInSync) {
                        serverTimeTxt.setText(context.getString(R.string.server_time) + " " + time);
                    } else {
                        serverTimeTxt.setText(context.getString(R.string.local_time) + " " + time);
                    }
                }
            //});
       // }
    }
}
