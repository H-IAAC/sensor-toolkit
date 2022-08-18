package br.org.eldorado.hiaac.actuators;

import android.app.Activity;

public class Actuators {
    public final VolumeController VOLUME;
    public final BluetoothController BLUETOOTH;
    public final BrightnessController BRIGHTNESS;
    public final GpsController GPS;
    public final WifiController WIFI;

    public Actuators(Activity activity) {
        VOLUME = new VolumeController(activity);
        BLUETOOTH = new BluetoothController(activity);
        BRIGHTNESS = new BrightnessController(activity);
        GPS = new GpsController(activity);
        WIFI = new WifiController(activity);
    }
}
