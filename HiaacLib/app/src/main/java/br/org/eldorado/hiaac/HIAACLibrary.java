package br.org.eldorado.hiaac;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.FirebaseApp;

import br.org.eldorado.hiaac.datacollector.DataCollectorActivity;
import br.org.eldorado.hiaac.datacollector.util.Preferences;

public class HIAACLibrary {

    public static void openDataCollector(Context ctx) {
        // Initiate shared preferences
        Preferences.init(ctx);
        Intent intent = new Intent(ctx, DataCollectorActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        FirebaseApp.initializeApp(ctx);
        ctx.startActivity(intent);
    }
}
