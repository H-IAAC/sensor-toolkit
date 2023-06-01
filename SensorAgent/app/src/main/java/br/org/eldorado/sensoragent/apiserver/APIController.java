package br.org.eldorado.sensoragent.apiserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.org.eldorado.sensoragent.controller.SensorController;
import br.org.eldorado.sensoragent.model.AgentSensorBase;
import br.org.eldorado.sensoragent.util.Log;

public class APIController {

    private final Log log;
    private static APIController inst;
    private final List<ClientConnection> clientList;

    public static APIController getInstance() {
        if (inst == null) {
            inst = new APIController();
        }
        return inst;
    }

    private APIController() {
        this.log = new Log("APIController");
        this.clientList = new ArrayList<>();
        this.startServer();
    }

    private void startServer() {
        ServerConnection.getInstance();
    }

    public void addClient(ClientConnection c) {
        log.i("Adding new client");
        this.clientList.add(c);
    }

    public void removeClient(ClientConnection c) {
        this.clientList.remove(c);
    }

    public void handleClientMessage(APICommand message, ClientConnection origin) {
        log.i("handleClientMessage: " + message + " clients: " + clientList.size());
        switch (message.getCommand()) {
            case TYPE_KEEP_ALIVE:
                origin.updateKeepAlive();
                break;
            case TYPE_START_SENSOR:
                SensorController.getInstance().addSensor(Double.valueOf(message.getParameters().get(0).toString()).intValue());
                break;
                /* TODO send msg to sensormanager app */
            case TYPE_STOP_SENSOR:
                SensorController.getInstance().stopSensor(Double.valueOf(message.getParameters().get(0).toString()).intValue());
                /* TODO send msg to sensormanager app */
                break;
            case TYPE_GET_SENSOR_DATA:
                AgentSensorBase sensor = SensorController.getInstance().getInformation(Double.valueOf(message.getParameters().get(0).toString()).intValue());
                message.setParameters(Arrays.asList(message.getParameters().get(0), sensor));
                origin.sendMessage(message);
                break;
        }
    }

    public void sendForAll(APICommand msg) {
        for (ClientConnection c : clientList) {
            c.sendMessage(msg);
        }
    }
}
