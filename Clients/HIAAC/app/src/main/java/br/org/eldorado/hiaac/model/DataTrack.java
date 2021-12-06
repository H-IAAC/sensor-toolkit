package br.org.eldorado.hiaac.model;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import br.org.eldorado.sensoragent.model.SensorBase;

public class DataTrack {

    private List<SensorBase> sensorList;
    private String label;

    public DataTrack() {
        sensorList = new ArrayList<SensorBase>();
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
