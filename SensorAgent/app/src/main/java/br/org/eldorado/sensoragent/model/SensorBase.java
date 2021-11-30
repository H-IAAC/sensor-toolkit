package br.org.eldorado.sensoragent.model;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.Gson;

import java.util.Arrays;

import br.org.eldorado.sensoragent.SensorAgentContext;
import br.org.eldorado.sensoragent.apiserver.APICommand;
import br.org.eldorado.sensoragent.apiserver.APIController;
import br.org.eldorado.sensoragent.util.Log;

public class SensorBase implements SensorEventListener, Parcelable {
    public static final int TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public static final int TYPE_AMBIENT_TEMPERATURE = Sensor.TYPE_AMBIENT_TEMPERATURE;
    public static final int TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public static final int TYPE_LIGHT = Sensor.TYPE_LIGHT;
    public static final int TYPE_PROXIMITY = Sensor.TYPE_PROXIMITY;
    public static final int TYPE_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;
    public static final int TYPE_GRAVITY = Sensor.TYPE_GRAVITY;

    protected SensorManager sensorManager;
    protected long timestamp;
    protected float power;
    protected float[] values;
    protected Log log;
    protected int type;

    public SensorBase(String sensorClass, int type) {
        this.type = type;
        this.sensorManager = (SensorManager) SensorAgentContext.getInstance().getContext().getSystemService(Context.SENSOR_SERVICE);
        log = new Log(sensorClass);
    }

    public SensorBase(Parcel in) {
        timestamp = in.readLong();
        in.readFloatArray(values);
        power = in.readFloat();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            if (power != event.sensor.getPower() || !Arrays.equals(event.values, values)) {
                /* TODO save data to database */
                log.i(event.sensor.getStringType() + " power: " + event.sensor.getPower() + " values: " +
                        (event.values == null ? "null" : event.values[0]));
                this.power = event.sensor.getPower();
                this.values = Arrays.copyOf(event.values, event.values.length);
                this.timestamp = event.timestamp;
                APICommand cmd =
                        new APICommand(
                                APICommand.CommandType.TYPE_GET_SENSOR_DATA,
                                Arrays.asList(type, new Gson().toJson(SensorBase.this, SensorBase.class)));
                APIController.getInstance().sendForAll(cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void registerListener(Sensor sensor) {
        log.i("Starting sensor");
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void unregisterListener() {
        log.i("Stopping sensor");
        sensorManager.unregisterListener(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp);
        dest.writeFloatArray(values);
        dest.writeFloat(power);
    }

    public static final Parcelable.Creator<SensorBase> CREATOR = new Creator<SensorBase>() {
        @Override
        public SensorBase createFromParcel(Parcel source) {
            return new SensorBase(source);
        }

        @Override
        public SensorBase[] newArray(int size) {
            return new SensorBase[size];
        }
    };
}
