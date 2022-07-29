package br.org.eldorado.hiaac.actuators;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;

public class WifiController {
    private Activity activity;

    protected WifiController(Activity activity) {
        this.activity = activity;
    }

    public void toogle() {
        WifiManager wifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (Build.VERSION.SDK_INT<Build.VERSION_CODES.Q)
            wifiManager.setWifiEnabled(!wifiManager.isWifiEnabled());
        else
        {
            Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
            activity.startActivityForResult(panelIntent,1);
        }
    }
}
