package br.org.eldorado.sensorapi.model;

import java.util.Arrays;

import br.org.eldorado.sensorapi.connection.SensorAPICommand;
import br.org.eldorado.sensorapi.connection.SensorAPICommand.CommandType;
import br.org.eldorado.sensorapi.connection.ServerConnection;
import listener.SensorListener;

public class SensorBase {

	public static final int TYPE_ACCELEROMETER = 1;
    public static final int TYPE_MAGNETIC_FIELD = 2;
    public static final int TYPE_GYROSCOPE = 4;
    public static final int TYPE_LUMINOSITY = 5;
    public static final int TYPE_PROXIMITY = 8;
    public static final int TYPE_AMBIENT_TEMPERATUR = 13;

    private long timestamp;
    private String name;
    private float power;
    protected float[] values;
    private int type;
    private SensorListener listener;
    private boolean isStarted;
	private ServerConnection connection;
	
	public SensorBase(int type) {
		this.type = type;
		this.name = "UNKNOWN";
		this.values = new float[1];
		this.isStarted = false;
	}
	
	public void notify(CommandType type) {
		if (listener != null) {
			switch (type) {
				case TYPE_SENSOR_STARTED:
					System.out.println("notify - Sensor Started");
					listener.onStarted(this);
					break;
				case TYPE_SENSOR_STOPPED:
					System.out.println("notify - Sensor Stopped");
					listener.onStopped(this);
					break;
				case TYPE_GET_SENSOR_DATA:
					listener.onChanged(this);
					break;
				default:
					break;
			}
		}
	}
	
	public void registerListener(SensorListener l) {
		this.listener = l;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public float getPower() {
		return power;
	}
	
	public void setPower(float power) {
		this.power = power;
	}
	
	public float[] getValues() {
		return values;
	}

	public void setValues(float[] values) {
		this.values = values;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public boolean isStarted() {
		return isStarted;
	}

	public void setStarted(boolean isStarted) {
		this.isStarted = isStarted;
	}

	public void setConnection(ServerConnection c) {
		this.connection = c;
	}
	
	public void start() {
		if (!isStarted) {
			SensorAPICommand cmd = new SensorAPICommand(CommandType.TYPE_START_SENSOR, Arrays.asList(getType()));
			connection.send(cmd);
		}
	}
	
	public void stop() {
		SensorAPICommand cmd = new SensorAPICommand(CommandType.TYPE_STOP_SENSOR, Arrays.asList(getType()));
		connection.send(cmd);
	}
	
	public String getValuesString() {
		return "";
	}


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(getName()).append(" -\n")
                .append("isStarted: ").append(isStarted()).append("\n")
                .append("power: ").append(getPower()).append(" mAh\n")
                .append("Timestamp: ").append(getTimestamp()).append("\n")
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
