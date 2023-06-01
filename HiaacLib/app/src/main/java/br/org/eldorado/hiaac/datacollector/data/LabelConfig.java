package br.org.eldorado.hiaac.datacollector.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class LabelConfig {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "experiment")
    public final String experiment;

    @NonNull
    @ColumnInfo(name = "activity")
    public final String activity;

    @NonNull
    @ColumnInfo(name = "user-id")
    public final String userId;

    @NonNull
    @ColumnInfo(name = "device-location")
    public final String deviceLocation;

    @NonNull
    @ColumnInfo(name = "stop-time")
    public final int stopTime;

    @NonNull
    @ColumnInfo(name = "scheduled-time")
    public final long scheduledTime;

    @NonNull
    @ColumnInfo(name = "sendToServer")
    public final boolean sendToServer;

    public LabelConfig(@NonNull String experiment, int stopTime, String deviceLocation, String userId, boolean sendToServer, String activity, long scheduledTime) {
        this.experiment = experiment;
        this.activity = activity;
        this.userId = userId;
        this.deviceLocation = deviceLocation;
        this.stopTime = stopTime;
        this.sendToServer = sendToServer;
        this.scheduledTime = scheduledTime;
    }
}
