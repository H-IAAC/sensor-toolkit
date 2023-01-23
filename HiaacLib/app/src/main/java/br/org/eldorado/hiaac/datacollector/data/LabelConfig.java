package br.org.eldorado.hiaac.datacollector.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LabelConfig {
    @NonNull
    @PrimaryKey
    public String label;

    @NonNull
    @ColumnInfo(name = "stop-time")
    public int stopTime;

    @NonNull
    @ColumnInfo(name = "device-location")
    public String deviceLocation;

    @NonNull
    @ColumnInfo(name = "user-id")
    public String userId;

    @NonNull
    @ColumnInfo(name = "activity")
    public String activity;


    @NonNull
    @ColumnInfo(name = "sendToServer")
    public boolean sendToServer;

    public LabelConfig(@NonNull String label, int stopTime, String deviceLocation, String userId, boolean sendToServer, String activity) {
        this.label = label;
        this.stopTime = stopTime;
        this.deviceLocation = deviceLocation;
        this.userId = userId;
        this.sendToServer = sendToServer;
        this.activity = activity;
    }
}
