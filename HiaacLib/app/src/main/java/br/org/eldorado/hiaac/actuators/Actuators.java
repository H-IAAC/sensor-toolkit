package br.org.eldorado.hiaac.actuators;

import android.app.Activity;

public class Actuators {
    public VolumeController VOLUME;
    public BluetoothController BLUETOOTH;
    public BrightnessController BRIGHTNESS;
    public GpsController GPS;
    public WifiController WIFI;

    public Actuators(Activity activity) {
        VOLUME = new VolumeController(activity);
        BLUETOOTH = new BluetoothController(activity);
        BRIGHTNESS = new BrightnessController(activity);
        GPS = new GpsController(activity);
        WIFI = new WifiController(activity);
    }
}
