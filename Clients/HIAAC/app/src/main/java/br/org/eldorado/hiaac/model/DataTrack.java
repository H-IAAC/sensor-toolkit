package br.org.eldorado.hiaac.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import br.org.eldorado.sensoragent.model.SensorBase;

public class DataTrack {

    private List<SensorBase> sensorList;
    private String label;
    private int stopTime;

    public DataTrack(int stopTime) {
        sensorList = new ArrayList<SensorBase>();
        this.stopTime = stopTime;
    }

    public int getStopTime() {
        return stopTime;
    }

    public String getLabel() {
        return label;
    }

    public void addSensor(SensorBase sensor) {
        sensorList.add(sensor);
    }

    public List<SensorBase> getSensorList() {
        return sensorList;
    }
}
