package br.org.eldorado.sensoragent.model;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;

public class Accelerometer extends SensorBase {
    private static final String TAG = "Accelerometer";
    private Sensor sensor;

    public Accelerometer() {
        super(TAG, TYPE_ACCELEROMETER);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        registerListener(sensor);
    }
}
