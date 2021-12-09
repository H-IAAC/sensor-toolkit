package br.org.eldorado.hiaac.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import br.org.eldorado.sensoragent.model.SensorBase;

public class DataTrack {

    private List<SensorBase> sensorList;
    private String label;
    private int stopTime;

    public DataTrack() {
        sensorList = new ArrayList<SensorBase>();
    }

    public void setStopTime(int stp) {
        this.stopTime = stp;
    }

    public void setLabel(String lbl) {
        this.label = lbl;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DataTrack) {
            DataTrack dt = (DataTrack) obj;
            if (dt.getLabel().equals(getLabel())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return getLabel() + " " + getStopTime();
    }
}
