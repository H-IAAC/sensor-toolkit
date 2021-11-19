package br.org.eldorado.sensorapi.connection;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.org.eldorado.sensorapi.connection.SensorAPICommand.CommandType;
import br.org.eldorado.sensorapi.model.SensorBase;
import br.org.eldorado.sensorapi.utils.Constants;

public class ServerConnection {
	
	private String ip;
	private String name;
	private Socket connection;
	private boolean isConnected;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	private Map<Integer, SensorBase> sensors;
	
	public ServerConnection(String ip, String n) {
		this.ip = ip;
		this.name = n;
		this.sensors = new HashMap<Integer, SensorBase>();
	}
	
	public void connect() {
		try {
			System.out.println("Trying to connect to " + ip);
			connection = new Socket(ip, Constants.SERVER_PORT);
			isConnected = true;
			System.out.println("Connected to " + ip);
			sendKeepAlive();
			receive();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void addSensor(SensorBase sensor) {
		System.out.println("addSensor: " + sensor);
		sensors.put(sensor.getType(), sensor);
		sensor.setConnection(this);
		SensorAPICommand cmd = new SensorAPICommand(CommandType.TYPE_GET_SENSOR_DATA, Arrays.asList(sensor.getType()));
		send(cmd);
		try {
			Thread.sleep(500);
			if (sensor.isStarted()) {
				sensor.notify(CommandType.TYPE_SENSOR_STARTED);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ObjectInputStream getInput() {
		if (in == null) {
			try {
				in = new ObjectInputStream(connection.getInputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return in;
	}
	
	private ObjectOutputStream getOutput() {
		if (out == null) {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return out;
	}
	
	public synchronized void send(SensorAPICommand cmd) {
		try {
			System.out.println("Sending " + cmd.getJson() +" to " + ip);
			byte[] msg = cmd.getBytes();
			
			getOutput().writeInt(msg.length);
			getOutput().flush();
			getOutput().write(msg);
			getOutput().flush();
		} catch (IOException e) {
			isConnected = false;
			e.printStackTrace();
		}
	}
	
	/**
	 * Receives a message from server and handle it
	 */
	private void receive() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (isConnected) {
						if (getInput().available() > 0) {
							byte[] msg = new byte[getInput().readInt()];
							getInput().read(msg);
                            SensorAPICommand cmd = new Gson().fromJson(new String(msg), SensorAPICommand.class);
                            handleMessage(cmd);
						}
						Thread.sleep(200);
					}
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						if (connection != null) {
							connection.close();
						}
						if (in != null) {
							in.close();
						}
						if (out != null) {
							out.close();
						}
					} catch (Exception e) {
						
					}
				}
			}
		}).start();
	}
	
	private void handleMessage(SensorAPICommand cmd) {
        System.out.println("Handling msg: " + cmd.getJson());
        SensorBase sensor = sensors.get(cmd.getSensorType());
        if (sensor != null) {
	        switch (cmd.getCommand()) {
				case TYPE_START_SENSOR:
					break;
				case TYPE_SENSOR_STARTED:
					/* notify listener and ask for sensor data */
					sensor.notify(CommandType.TYPE_SENSOR_STARTED);
					cmd.setCommand(CommandType.TYPE_GET_SENSOR_DATA);
					send(cmd);
					break;
				case TYPE_STOP_SENSOR:
					break;
				case TYPE_SENSOR_STOPPED:
					sensor.setStarted(false);
					sensor.notify(CommandType.TYPE_SENSOR_STOPPED);
					break;
				case TYPE_GET_SENSOR_DATA:
					if (cmd.getParameters().get(1) != null) {
						JsonObject json = new Gson().fromJson(cmd.getParameters().get(1).toString(), JsonObject.class);
						sensor.setStarted(true);
						sensor.setPower(json.get("power").getAsFloat());
						sensor.setTimestamp(json.get("timestamp").getAsLong());
						sensor.setValues(new Gson().fromJson(json.get("values").getAsJsonArray(), float[].class));
						sensor.notify(CommandType.TYPE_GET_SENSOR_DATA);
					}
					break;
				default:
					break;
			}
        }
	}
	
	/**
	 * Sends a keep alive command every 20 seconds,
	 * otherwise server will disconnect this client
	 */
	private void sendKeepAlive() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (isConnected) {
						SensorAPICommand keepAlive = new SensorAPICommand(SensorAPICommand.CommandType.TYPE_KEEP_ALIVE, null);
						send(keepAlive);
						Thread.sleep(20000);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

}
