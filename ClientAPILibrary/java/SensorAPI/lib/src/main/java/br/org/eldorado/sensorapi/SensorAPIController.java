package br.org.eldorado.sensorapi;

import java.util.ArrayList;
import java.util.List;

import br.org.eldorado.sensorapi.connection.ServerConnection;

public class SensorAPIController {

	private static SensorAPIController inst;
	private List<ServerConnection> serverList;
	
	public static SensorAPIController getInstance() {
		if (inst == null) {
			inst = new SensorAPIController();
		}
		return inst;
	}
	
	private SensorAPIController() {
		this.serverList = new ArrayList<ServerConnection>();
	}
	
	public void addServerConnection(ServerConnection c) {
		serverList.add(c);
	}
}
