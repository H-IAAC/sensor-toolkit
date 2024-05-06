package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Patterns;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import br.org.eldorado.hiaac.R;

public class Preferences {
    private static Context ctx;
    private static SharedPreferences prefs;
    private static Log log;

    public static void init(Context ctx) {
        Preferences.ctx = ctx;
        Preferences.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        log = new Log("Preferences");

        if (notContains(R.string.settings_server_config)) {
//            String server = getArrayResource(R.array.server_urls, 0);
//            Preferences.prefs.edit().putString(getResource(R.string.settings_server_config),
//                    server).apply();
            String server = getArrayResource(R.array.server_urls, 3);
            Preferences.prefs.edit().putString(getResource(R.string.settings_server_config),
                    server).apply();
            Preferences.prefs.edit().putString(getResource(R.string.settings_custom_server_config),
                    "192.168.1.162:8080").apply();
        }

        if (notContains(R.string.settings_counter_key)) {
            String server = getArrayResource(R.array.collect_counter_values, 8);
            Preferences.prefs.edit().putString(getResource(R.string.settings_counter_key),
                    server).apply();
        }

        if (notContains(R.string.settings_device_locations)) {
            Set<String> locations = new HashSet(Arrays.asList(ctx.getResources().getStringArray(R.array.device_location_items)));
            Preferences.prefs.edit().putStringSet(getResource(R.string.settings_device_locations),
                    locations).apply();
        }

        if (notContains(R.string.preferences_run_checking)) {
            Preferences.prefs.edit().putBoolean(getResource(R.string.preferences_run_checking), true).apply();
        }
    }

    public static Boolean shouldRunChecking() {
        return Preferences.prefs.getBoolean(getResource(R.string.preferences_run_checking), true);
    }

    public static void setToRunChecking(Boolean shouldRun) {
        Preferences.prefs.edit().putBoolean(getResource(R.string.preferences_run_checking), shouldRun).apply();
    }

    public static ArrayList<String> getDeviceLocationsList() {
        Set<String> locationsList = new HashSet<String>();
        locationsList = Preferences.prefs.getStringSet(getResource(R.string.settings_device_locations), locationsList);
        ArrayList<String> locations = new ArrayList<String>(Arrays.asList( locationsList.toArray(new String[locationsList.size()])));
        locations.add(0, getResource(R.string.device_location_add));
        return locations;
    }

    public static String getGatewayIP() {
        DhcpInfo d;
        WifiManager wifii;
        wifii= (WifiManager) ctx.getSystemService(Context.WIFI_SERVICE);
        d=wifii.getDhcpInfo();
        return Formatter.formatIpAddress(d.gateway) + ":8080";
    }

    public static void addNewDeviceLocation(String location) {
        Set<String> locationsList = new HashSet<String>();
        locationsList = Preferences.prefs.getStringSet(getResource(R.string.settings_device_locations), locationsList);
        if (!locationsList.contains(location)) {
            locationsList.add(location);
            Preferences.prefs.edit().putStringSet(getResource(R.string.settings_device_locations),
                    locationsList).apply();
        }
    }

    public static String getPreferredServer() {

        String optionSelected = Preferences.prefs.getString(getResource(R.string.settings_server_config), "default");
        String localhost = getArrayResource(R.array.server_urls, 2);
        String custom = getArrayResource(R.array.server_urls, 3);
        String ret;

        if (optionSelected.equals(localhost)) {
            ret = getGatewayIP();
        } else if (optionSelected.equals(custom)) {
            ret = Preferences.prefs.getString(getResource(R.string.settings_custom_server_config), "");
        } else {
            ret = Preferences.prefs.getString(getResource(R.string.settings_server_config), "");
        }

        if (!Patterns.WEB_URL.matcher("http://" + ret).matches()) {
            log.d("Invalid server URL configured: " + ret);
            ret = "127.0.0.1:8080";
        }

        return ret;
    }

    public static Boolean setCustomServerAddress(String addr) {

        if (!(addr.split(":").length == 2) ||
            !Patterns.WEB_URL.matcher("http://" + addr).matches()) {
            Toast.makeText(ctx, "Invalid address: " + addr, Toast.LENGTH_LONG).show();

            return false;
        }

        Preferences.prefs.edit().putString(getResource(R.string.settings_custom_server_config), addr).apply();

        String address = Preferences.getPreferredServer();
        Toast.makeText(ctx, "Server address: " + address, Toast.LENGTH_LONG).show();

        return true;
    }

    public static Integer getPreferredStartDelay() {
        return Integer.parseInt(Preferences.prefs.getString(ctx.getResources().getString(R.string.settings_counter_key),
                "8"));
    }

    private static String getResource(Integer stringId) {
        return ctx.getResources().getString(stringId);
    }
    private static String getArrayResource(Integer arrayId, Integer idx) {
        return ctx.getResources().getStringArray(arrayId)[idx];
    }

    private static Boolean notContains(Integer stringId) {
        return !Preferences.prefs.contains(getResource(stringId));
    }
}
