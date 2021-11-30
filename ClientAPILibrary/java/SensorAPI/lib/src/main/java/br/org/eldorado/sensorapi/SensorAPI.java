package br.org.eldorado.sensorapi;

import java.util.Arrays;

import org.checkerframework.checker.units.qual.Acceleration;

import br.org.eldorado.sensorapi.connection.SensorAPICommand;
import br.org.eldorado.sensorapi.connection.SensorAPICommand.CommandType;
import br.org.eldorado.sensorapi.model.Accelerometer;
import br.org.eldorado.sensorapi.connection.ServerConnection;

public class SensorAPI {

	
	public void searchServers() {}
	public void connect() {
		
		String ip = "localhost";
		ServerConnection c = new ServerConnection(ip, "Emulator");
		c.connect();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Accelerometer acc = new Accelerometer();
		c.addSensor(acc);
		acc.start();
		
	}
	
	public static void main(String[] args) {
		//new SensorAPI().connect();
	}
	
}
