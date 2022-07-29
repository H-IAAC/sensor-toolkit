package br.org.eldorado.hiaac.actuators;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

public class VolumeController {
    private Activity activity;
    private int stream = AudioManager.STREAM_MUSIC;

    protected VolumeController(Activity activity) {
        this.activity = activity;
    }

    public void volumeUp() {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
        int volume = audioManager.getStreamVolume(stream);
        Log.d("isVolumeFixed", ": " + audioManager.isVolumeFixed());
        Log.d("Volume", ": " + volume);
    }

    public void volumeDown() {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        int volume = audioManager.getStreamVolume(stream);
        Log.d("isVolumeFixed", ": " + audioManager.isVolumeFixed());
        Log.d("Volume", ": " + volume);
    }

    public void volumeToogleMute() {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI);
        int volume = audioManager.getStreamVolume(stream);
        Log.d("Volume", ": " + volume);
    }

    public void volumeMax() {
        AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(stream);
        audioManager.setStreamVolume(stream, maxVolume, AudioManager.FLAG_SHOW_UI);
        int volume = audioManager.getStreamVolume(stream);
        Log.d("Volume", ": " + volume);
    }
}
