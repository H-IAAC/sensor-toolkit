package br.org.eldorado.hiaac.datacollector.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ExperimentStatistics {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "experiment-id")
    private int experimentId;

    @ColumnInfo(name = "sensor-name")
    private String sensorName;

    @ColumnInfo(name = "sensor-frequency")
    private int sensorFrequency;

    @ColumnInfo(name = "start-time")
    private long startTime;

    @ColumnInfo(name = "end-time")
    private long endTime;

    @ColumnInfo(name = "valid-data")
    private long collectedData;

    @ColumnInfo(name = "invalid-data")
    private long invalidData;

    @ColumnInfo(name = "timestamp-average")
    private long timestampAverage;

    @ColumnInfo(name = "timestamp-standard-variation")
    private long timestampStandardVariation;

    public int getId() {
        return id;
    }

    public int getExperimentId() {
        return experimentId;
    }

    public String getSensorName() {
        return sensorName;
    }

    public int getSensorFrequency() {
        return sensorFrequency;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getCollectedData() {
        return collectedData;
    }

    public long getInvalidData() {
        return invalidData;
    }

    public long getTimestampAverage() {
        return timestampAverage;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public void setSensorFrequency(int sensorFrequency) {
        this.sensorFrequency = sensorFrequency;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public void setCollectedData(long collectedData) {
        this.collectedData = collectedData;
    }

    public void setInvalidData(long invalidData) {
        this.invalidData = invalidData;
    }

    public void setTimestampAverage(long timestampAverage) {
        this.timestampAverage = timestampAverage;
    }

    public long getTimestampStandardVariation() {
        return timestampStandardVariation;
    }

    public void setTimestampStandardVariation(long timestampStandardVariation) {
        this.timestampStandardVariation = timestampStandardVariation;
    }
}
