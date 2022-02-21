package br.org.eldorado.hiaac.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.opencsv.bean.CsvNumber;

import br.org.eldorado.sensoragent.model.SensorBase;

@Entity
public class LabeledData {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "label-name")
    private String label;

    @ColumnInfo(name = "sensor-name")
    private String sensorName;

    @CsvNumber("#")
    @ColumnInfo(name = "sensor-timestamp")
    private long timestamp;

    @ColumnInfo(name = "sensor-frequency")
    private int frequency;

    @ColumnInfo(name = "sensor-power")
    private float power;

    @ColumnInfo(name = "sensor-values")
    private String sensorValues;

    @ColumnInfo(name = "data-used")
    private int isDataUsed;

    private SensorBase sensor;

    public LabeledData(String label, SensorBase sensor) {
        this.sensor = sensor;
        this.label = label;
        this.sensorName = sensor.getName();
        this.timestamp = sensor.getTimestamp();
        this.frequency = sensor.getFrequency();
        this.sensorValues = sensor.getValuesString();
        this.power = sensor.getPower();
        this.isDataUsed = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(String sensorName) {
        this.sensorName = sensorName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public float getPower() {
        return power;
    }

    public void setPower(float power) {
        this.power = power;
    }

    public String getSensorValues() {
        return sensorValues;
    }

    public void setSensorValues(String sensorValues) {
        this.sensorValues = sensorValues;
    }

    public String[] getCSVFormattedString() {
        String[] values = sensorValues.split(",");
        return new String[]{label, sensorName, String.valueOf(power), String.valueOf(frequency), String.valueOf(timestamp),
                            values[0], values.length > 1 ? values[1] : "-",
                            values.length > 2 ? values[2] : "-"};
    }

    public String[] getCSVHeaders() {
        return new String[]{"Label", "Sensor Name", "Power Consumption (mAh)", "Sensor Frequency (Hz)",
                            "Timestamp", "Value 1", "Value 2", "Value 3"};
    }

    public SensorBase getSensor() {
        return sensor;
    }

    public int getIsDataUsed() {
        return isDataUsed;
    }

    public void setIsDataUsed(int isDataUsed) {
        this.isDataUsed = isDataUsed;
    }
}
