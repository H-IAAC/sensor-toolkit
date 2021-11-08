package br.org.eldorado.sensorapi.connection;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.Gson;

public class SensorAPICommand {

	public static enum CommandType {
        TYPE_KEEP_ALIVE, TYPE_PING, TYPE_START_SENSOR,
        TYPE_STOP_SENSOR, TYPE_GET_SENSOR_DATA,
        TYPE_SENSOR_STARTED, TYPE_SENSOR_STOPPED
	}
	
	private CommandType command;
	private List<Object> parameters;
	
	public SensorAPICommand(CommandType cmd, List<Object> par) {
	    this.parameters = par;
	    this.command = cmd;
	}
	
	public int getSensorType() {
		return new Double((double)parameters.get(0)).intValue();
	}
	
	public void setCommand(CommandType c) {
		this.command = c;
	}
	
	public CommandType getCommand() {
		return command;
	}
	
	public List<Object> getParameters() {
	    return parameters;
	}
	
	public void setParameters(List<Object> parameters) {
	    this.parameters = parameters;
	}
	
	public byte[] getBytes() {
	    return new Gson().toJson(this).getBytes(StandardCharsets.UTF_8);
	}
	
	public String getJson() {
	    return new Gson().toJson(this).toString();
	}
	
	@Override
	public String toString() {
	    return "Command: " + command + " parameters: " + parameters;
	}
}