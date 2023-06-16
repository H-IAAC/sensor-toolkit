package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

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
    }

    public static String getPreferredServer() {
        return Preferences.prefs.getString(ctx.getResources().getString(R.string.settings_server_config), "1:2");
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
