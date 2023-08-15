package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.ArrayAdapter;
import android.widget.Toast;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.org.eldorado.hiaac.R;

public class Preferences {
    private static Context ctx;
    private static SharedPreferences prefs;

    public static void init(Context ctx) {
        Preferences.ctx = ctx;
        Preferences.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (notContains(R.string.settings_server_config)) {
            String server = getArrayResource(R.array.server_urls, 0);
            Preferences.prefs.edit().putString(getResource(R.string.settings_server_config),
                                               server).apply();
        }

        if (notContains(R.string.settings_counter_key)) {
            String server = getArrayResource(R.array.collect_counter_values, 8);
            Preferences.prefs.edit().putString(getResource(R.string.settings_counter_key),
                    server).apply();
        }

        if (notContains(R.string.settings_device_locations)) {
            Set<String> locations = new HashSet();
            locations.addAll(Arrays.asList(ctx.getResources().getStringArray(R.array.device_location_items)));
            Preferences.prefs.edit().putStringSet(getResource(R.string.settings_device_locations),
                    locations).apply();
        }
    }

    public static String getPreferredServer() {
        return Preferences.prefs.getString(ctx.getResources().getString(R.string.settings_server_config), "1:2");
    }

    public static ArrayList<String> getDeviceLocationsList() {
        Set<String> locationsList = new HashSet<String>();
        locationsList = Preferences.prefs.getStringSet(getResource(R.string.settings_device_locations), locationsList);
        ArrayList<String> locations = new ArrayList<String>();
        locations.addAll(Arrays.asList( locationsList.toArray(new String[locationsList.size()])));
        Collections.sort(locations, new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return s.toLowerCase().compareTo(t1.toLowerCase());
            }
        });
        locations.set(0, getResource(R.string.device_location_add));
        return locations;
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
