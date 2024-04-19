package br.org.eldorado.hiaac.datacollector.util;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public class Utils {
    public static void emitStartBeep() {
        ToneGenerator startBeep = new ToneGenerator(AudioManager.STREAM_RING, 9999);
        startBeep.startTone(ToneGenerator.TONE_SUP_DIAL, 2000);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startBeep.startTone(ToneGenerator.TONE_SUP_DIAL, 2000);
            }
        });
    }

    public static void emitStopBeep() {
        ToneGenerator startBeep = new ToneGenerator(AudioManager.STREAM_RING, 9999);
        startBeep.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 2000);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startBeep.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 2000);
            }
        });
    }
}
