package br.org.eldorado.sensoragent.apiserver;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class APICommand {

    public static enum CommandType {
        TYPE_KEEP_ALIVE, TYPE_PING, TYPE_START_SENSOR,
        TYPE_STOP_SENSOR, TYPE_GET_SENSOR_DATA,
        TYPE_SENSOR_STARTED, TYPE_SENSOR_STOPPED
    }

    private CommandType command;
    private List<Object> parameters;

    public APICommand(CommandType cmd, List<Object> par) {
        this.parameters = par;
        this.command = cmd;
    }

    public List<Object> getParameters() {
        return parameters;
    }

    public void setParameters(List<Object> parameters) {
        this.parameters = parameters;
    }

    public void setCommand(CommandType cmd) {
        this.command = cmd;
    }

    public CommandType getCommand() {
        return command;
    }

    public byte[] getBytes() {
        return new Gson().toJson(this).getBytes(StandardCharsets.UTF_8);
    }

    public String getJson() {
        return new Gson().toJson(this).toString();
    }

    @NonNull
    @Override
    public String toString() {
        return "Command: " + command + " parameters: " + parameters;
    }
}
