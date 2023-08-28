package br.org.eldorado.hiaac.datacollector.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import br.org.eldorado.hiaac.R;

public class Preferences {

    public static void init(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if (notContains(ctx, R.string.settings_server_config)) {
            String server = getArrayResource(ctx, R.array.server_urls, 0);
            prefs.edit().putString(getResource(ctx, R.string.settings_server_config),
                                               server).apply();
        }

        if (notContains(ctx, R.string.settings_counter_key)) {
            String server = getArrayResource(ctx, R.array.collect_counter_values, 8);
            prefs.edit().putString(getResource(ctx, R.string.settings_counter_key),
                    server).apply();
        }
    }

    public static String getPreferredServer(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(ctx.getResources().getString(R.string.settings_server_config), "1:2");
    }

    public static Integer getPreferredStartDelay(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return Integer.parseInt(prefs.getString(ctx.getResources().getString(R.string.settings_counter_key),
                        "8"));
    }

    private static String getResource(Context ctx, Integer stringId) {
        return ctx.getResources().getString(stringId);
    }
    private static String getArrayResource(Context ctx, Integer arrayId, Integer idx) {
        return ctx.getResources().getStringArray(arrayId)[idx];
    }

    private static Boolean notContains(Context ctx, Integer stringId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return !prefs.contains(getResource(ctx, stringId));
    }
}
