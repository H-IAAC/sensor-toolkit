package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;

import br.org.eldorado.sensorsdk.controller.SensorController;
import br.org.eldorado.sensorsdk.listener.SensorSDKListener;
import br.org.eldorado.sensorsdk.util.Log;


public class SensorBase implements Parcelable {
    public static final int TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public static final int TYPE_AMBIENT_TEMPERATUR = Sensor.TYPE_AMBIENT_TEMPERATURE;
    public static final int TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public static final int TYPE_LUMINOSITY = Sensor.TYPE_LIGHT;
    public static final int TYPE_PROXIMITY = Sensor.TYPE_PROXIMITY;
    public static final int TYPE_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;


    private long timestamp;
    private String name;
    private float power;
    protected float[] values;
    private int type;
    private Log log;
    private SensorController controller;
    private SensorSDKListener listener;
    private boolean isStarted;

    /* TODO limitar funcoes se listener estiver null */

    public SensorBase(String sensorClass, int type) {
        this.log = new Log(sensorClass);
        this.listener = null;
        this.name = sensorClass;
        this.type = type;
        this.values = new float[1];
        this.isStarted = false;
        this.controller = SensorController.getInstance();
        this.controller.addSensor(this);
    }

    public void registerListener(SensorSDKListener l) {
        this.listener = l;
        this.controller.getInformation(this);
    }

    public SensorSDKListener getListener() {
        return listener;
    }

    public String getValuesString() {
        return "";
    };

    public SensorBase(Parcel in) {
        timestamp = in.readLong();
        values = in.createFloatArray();
        power = in.readFloat();
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    protected float[] getValuesArray() {
        return values;
    }

    public float getPower() {
        return power;
    }

    public void updateInformation(SensorBase s) {
        if (s == null || s.getValuesArray() == null) {
            log.i("Sensor not started");
            isStarted = false;
            listener.onSensorStopped();
            return;
        }
        this.timestamp = s.getTimestamp();
        this.power = s.getPower();
        this.values = s.getValuesArray();
        log.i("Update information " + toString());
        if (!isStarted) {
            //isStarted = true;
            listener.onSensorStarted();
            controller.startGettingInformationThread(this);
        } else {
            this.listener.onSensorChanged();
        }
    }

    public boolean isStarted() {
        return isStarted;
    }

    public void setIsStarted(boolean b) {
        isStarted = b;
    }

    public String getName() {
        return name;
    }

    public void startSensor() {
        log.i("Starting sensor isStarted: " + isStarted);
        if (!isStarted) {
            controller.startSensor(this);
        }
    }

    public void stopSensor() {
        controller.stopSensor(this);
        listener.onSensorStopped();
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(name).append(" -\n")
                .append("isStarted: ").append(isStarted).append("\n")
                .append("power: ").append(power).append(" mAh\n")
                .append("Timestamp: ").append(timestamp).append("\n")
                .append("Values: [");
        if (values == null) {
            sb.append("null]");
        } else {
            for (int i = 0; i < values.length; i++) {
                sb.append(values[i]);
                if (i == values.length - 1) {
                    sb.append("]");
                } else {
                    sb.append(", ");
                }
            }
        }
        return sb.toString();
    }
}
