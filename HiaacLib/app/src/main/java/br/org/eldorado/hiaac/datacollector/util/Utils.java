package br.org.eldorado.hiaac.datacollector.util;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;

import java.text.SimpleDateFormat;
import java.util.Date;

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

    public static String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        return df.format(new Date());
    }

    public static String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }
}
