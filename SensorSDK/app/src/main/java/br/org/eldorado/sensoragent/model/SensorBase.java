package br.org.eldorado.sensoragent.model;

import android.hardware.Sensor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AndroidException;

import br.org.eldorado.sensorsdk.controller.SensorController;
import br.org.eldorado.sensorsdk.listener.SensorSDKListener;
import br.org.eldorado.sensorsdk.util.Log;


public class SensorBase implements Parcelable, Cloneable {
    public static final int TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public static final int TYPE_LINEAR_ACCELEROMETER = Sensor.TYPE_LINEAR_ACCELERATION;
    public static final int TYPE_AMBIENT_TEMPERATUR = Sensor.TYPE_AMBIENT_TEMPERATURE;
    public static final int TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public static final int TYPE_LUMINOSITY = Sensor.TYPE_LIGHT;
    public static final int TYPE_PROXIMITY = Sensor.TYPE_PROXIMITY;
    public static final int TYPE_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;
    public static final int TYPE_GRAVITY = Sensor.TYPE_GRAVITY;
    public static final int TYPE_GPS = 100;

    private static final int ON_STARTED = 0;
    private static final int ON_STOPPED = 1;
    private static final int ON_CHANGED = 2;

    private long timestamp;
    private String name;
    private float power;
    protected float[] values;
    private int type;
    private Log log;
    private SensorController controller;
    private SensorSDKListener listener;
    private boolean isStarted;
    private int frequency;



    public SensorBase(String sensorClass, int type) {
        this.log = new Log(sensorClass);
        this.listener = null;
        this.frequency = 1;
        this.name = sensorClass;
        this.type = type;
        this.values = new float[1];
        this.isStarted = false;
        this.controller = SensorController.getInstance();

        for (float value : values) {
            value = Float.MIN_VALUE;
        }
    }

    public void registerListener(SensorSDKListener l) {
        this.listener = l;
        this.controller.getInformation(this);
    }

    public boolean isValidValues() {
        int unchangedValue = 0;
        for (float value : values) {
            if (value == Float.MIN_VALUE) {
                unchangedValue++;
            }
        }
        int zeros = 0;
        for (float value : values) {
            if (value == 0f) {
                zeros++;
            }
        }
        return (unchangedValue != values.length) && ((zeros != values.length) ||  type == TYPE_PROXIMITY  || type == TYPE_LUMINOSITY);
    }

    public void setFrequency(int f) {
        this.frequency = f;
    }

    public int getFrequency() {
        return frequency;
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

    public float[] getValuesArray() {
        return values;
    }

    public float getPower() {
        return power;
    }

    public void updateInformation(AgentSensorBase s) {
        if (s == null || s.getValuesArray() == null) {
            log.i(getName() + " sensor not started");
            isStarted = false;
            fireListener(ON_STOPPED);
            return;
        } else if (values.length != s.getValuesArray().length) {
            values = new float[s.getValuesArray().length];
        }
        this.timestamp = s.getTimestamp();
        this.power = s.getPower();
        for (int i = 0; i < values.length; i++) {
            this.values[i] = s.getValuesArray()[i];
        }
        //this.values = s.getValuesArray();
        if (values.length == 3 && values[0] == Float.MIN_VALUE && values[1] == Float.MIN_VALUE && values[2] == Float.MIN_VALUE) {
            log.d("INVALID " + getName() + " " + values[0] + " " + values[1] + " " + values[2]);
        }
        //log.i("Update information " + toString());
        if (!isStarted) {
            //isStarted = true;
            fireListener(ON_STARTED);
            controller.startGettingInformationThread(this);
        } else {
            fireListener(ON_CHANGED);
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
        log.i(getName() + " sensor isStarted: " + isStarted);
        if (!isStarted) {
            controller.startSensor(this);
        }
    }

    public void stopSensor() {
        controller.stopSensor(this);
    }

    private void fireListener(int type) {
        if (listener != null) {
            try {
                switch (type) {
                    case ON_STARTED:
                        listener.onSensorStarted((SensorBase)this.clone());
                        break;
                    case ON_STOPPED:
                        listener.onSensorStopped((SensorBase)this.clone());
                        break;
                    case ON_CHANGED:
                        listener.onSensorChanged((SensorBase) this.clone());
                        break;
                }
            } catch (CloneNotSupportedException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
                .append("Frequency: ").append(frequency).append("\n")
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
