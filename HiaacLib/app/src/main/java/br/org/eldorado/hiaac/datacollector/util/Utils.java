package br.org.eldorado.hiaac.datacollector.util;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

public class Utils {
    public static void emitStartBeep() {
        ToneGenerator startBeep = new ToneGenerator(AudioManager.STREAM_RING, 100);
        startBeep.startTone(ToneGenerator.TONE_SUP_DIAL, 2000);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startBeep.startTone(ToneGenerator.TONE_SUP_DIAL, 2000);
            }
        });
    }

    public static void emitStopBeep() {
        ToneGenerator startBeep = new ToneGenerator(AudioManager.STREAM_RING, 100);
        startBeep.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 2000);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startBeep.startTone(ToneGenerator.TONE_SUP_INTERCEPT, 2000);
            }
        });
    }

    public static void emitErrorBeep() {
        ToneGenerator startBeep = new ToneGenerator(AudioManager.AUDIOFOCUS_REQUEST_FAILED, 100);
        startBeep.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 2000);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                startBeep.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 2000);
            }
        });
    }

    public static long getTimeDifference(long time1, long time2) {
        if (time1 >= time2)
            return time1 - time2;

        return time2 - time1;
    }
}
