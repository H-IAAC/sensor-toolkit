package br.org.eldorado.hiaac.datacollector.controller;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.telephony.TelephonyManager;

import java.util.Calendar;

import br.org.eldorado.hiaac.datacollector.model.ExtraSensoryData;
import br.org.eldorado.hiaac.datacollector.util.Log;

public class DiscreteFeaturesExtractor {

    private final Log log = new Log("DiscreteFeaturesExtractor");
    public void collectFeatures(Context context, ExtraSensoryData esData) {

        // 1️⃣ App state
        collectAppState(context, esData);

        // 2️⃣ Battery
        collectBatteryFeatures(context, esData);

        // 3️⃣ On the phone
        collectPhoneState(context, esData);

        // 4️⃣ Ringer mode
        collectRingerMode(context, esData);

        // 5️⃣ WiFi status
        collectNetworkStatus(context, esData);

        // 6️⃣ Time of day
        collectTimeOfDay(esData);
    }

    // 1️⃣ App state
    private void collectAppState(Context context, ExtraSensoryData features) {
        try {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            boolean isInteractive = pm.isInteractive(); //

            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_APP_STATE_ACTIVE, isInteractive ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_APP_STATE_INACTIVE, isInteractive ? 0 : 1);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_APP_STATE_BACKGROUND, 1);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_APP_STATE_MISSING, 0);
        } catch (Exception e) {
            log.e("collectAppState: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 2️⃣ Battery features
    private void collectBatteryFeatures(Context context, ExtraSensoryData features) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus == null) return;

            int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
            int plugged = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);

            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_PLUGGED_AC, (plugged == BatteryManager.BATTERY_PLUGGED_AC) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_PLUGGED_USB, (plugged == BatteryManager.BATTERY_PLUGGED_USB) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_PLUGGED_WIRELESS, (plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_PLUGGED_MISSING, 0);

            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_UNKNOWN, (status == BatteryManager.BATTERY_STATUS_UNKNOWN) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_UNPLUGGED, (plugged == 0) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_NOT_CHARGING, (status == BatteryManager.BATTERY_STATUS_NOT_CHARGING) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_DISCHARGING, (status == BatteryManager.BATTERY_STATUS_DISCHARGING) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_CHARGING, (status == BatteryManager.BATTERY_STATUS_CHARGING) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_FULL, (status == BatteryManager.BATTERY_STATUS_FULL) ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_BATTERY_STATE_MISSING, 0);
        } catch (Exception e) {
            log.e("collectBatteryFeatures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 3️⃣ On the phone
    private void collectPhoneState(Context context, ExtraSensoryData features) {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            boolean onCall = (tm.getCallState() != TelephonyManager.CALL_STATE_IDLE);

            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_ON_THE_PHONE_TRUE, onCall ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_ON_THE_PHONE_FALSE, onCall ? 0 : 1);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_ON_THE_PHONE_MISSING, 0);
        } catch (Exception e) {
            log.e("collectBatteryFeatures: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 4️⃣ Ringer mode
    private void collectRingerMode(Context context, ExtraSensoryData features) {
        try {
            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int mode = audio.getRingerMode();

            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_RINGER_MODE_NORMAL, mode == AudioManager.RINGER_MODE_NORMAL ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_RINGER_MODE_SILENT_NO_VIBRATE, mode == AudioManager.RINGER_MODE_SILENT ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_RINGER_MODE_SILENT_VIBRATE, mode == AudioManager.RINGER_MODE_VIBRATE ? 1 : 0);
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_RINGER_MODE_MISSING, 0);
        } catch (Exception e) {
            log.e("collectRingerMode: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 5️⃣ WiFi / network
    private void collectNetworkStatus(Context context, ExtraSensoryData features) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork != null && activeNetwork.isConnected()) {
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WIFI, 1);
                    features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WWAN, 0);
                } else {
                    features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WIFI, 0);
                    features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WWAN, 1);
                }
                features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_NOT_REACHABLE, 0);
            } else {
                features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_NOT_REACHABLE, 1);
                features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WIFI, 0);
                features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_REACHABLE_WWAN, 0);
            }
            features.addDiscreteFeature(ExtraSensoryData.FeatureName.DISCRETE_WIFI_STATUS_MISSING, 0);
        } catch (Exception e) {
            log.e("collectNetworkStatus: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 6️⃣ Time of day (faixas de 3h)
    private void collectTimeOfDay(ExtraSensoryData features) {
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);

        int[] ranges = {0, 3, 6, 9, 12, 15, 18, 21};
        for (int i = 0; i < ranges.length; i++) {
            int start = ranges[i];
            int end = (i == ranges.length - 2) ? 24 : ((i == ranges.length - 1) ? 3 : ranges[i + 2]);
            //String key = "discrete:time_of_day:between" + start + "and" + end;
            features.addDiscreteTimeFeature(start, end, (hour >= start && hour < end) ? 1 : 0);
            //features.put(key, (hour >= start && hour < end) ? 1 : 0);
        }
    }
}
