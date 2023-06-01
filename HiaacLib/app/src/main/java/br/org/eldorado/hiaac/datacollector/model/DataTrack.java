package br.org.eldorado.hiaac.datacollector.model;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;

public class DataTrack {

    private List<SensorFrequency> sensorList;
    private String label;
    private long configId;
    private int stopTime;
    private String mDeviceLocation;
    private String mUserId;
    private String mActivity;
    private boolean mSendFilesToServer;
    private String uid;

    public DataTrack() {
        sensorList = new ArrayList<>();
        mDeviceLocation = "";
    }

    public String getActivity() {
        return mActivity;
    }

    public void setActivity(String mActivity) {
        this.mActivity = mActivity;
    }

    public String getUserId() {
        return mUserId;
    }

    public void setUserId(String mUserId) {
        this.mUserId = mUserId;
    }

    public boolean isSendFilesToServer() {
        return mSendFilesToServer;
    }

    public void setSendFilesToServer(boolean mSendFilesToServer) {
        this.mSendFilesToServer = mSendFilesToServer;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getDeviceLocation() {
        return mDeviceLocation;
    }

    public void setDeviceLocation(String mDeviceLocation) {
        this.mDeviceLocation = mDeviceLocation;
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

    public void setConfigId(long lId) {
        this.configId = lId;
    }

    public long getConfigId() {
        return configId;
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
            return dt.getLabel().equals(getLabel()) && dt.getConfigId() == getConfigId();
        }
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return getLabel() + " " + getStopTime();
    }
}
