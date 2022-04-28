package br.org.eldorado.hiaac;

import android.content.Context;
import android.content.Intent;

import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;

public class HIAACLibrary {

    public static void openDataCollector(Context ctx) {
        Intent intent = new Intent(ctx, DataCollectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }
}
