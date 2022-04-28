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

    public LabelConfig(@NonNull String label, int stopTime) {
        this.label = label;
        this.stopTime = stopTime;
    }
}
