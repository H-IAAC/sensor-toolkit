package br.org.eldorado.hiaac.datacollector.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;

public class DataTrack {

    private List<SensorFrequency> sensorList;
    private String label;
    private int stopTime;

    public DataTrack() {
        sensorList = new ArrayList<SensorFrequency>();
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

    public void addSensorList(List<SensorFrequency> sensorList) {
        this.sensorList = sensorList;
    }

    public List<SensorFrequency> getSensorList() {
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
