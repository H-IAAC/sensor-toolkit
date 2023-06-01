package br.org.eldorado.hiaac.datacollector.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import com.opencsv.bean.CsvNumber;
import br.org.eldorado.sensoragent.model.SensorBase;

@Entity(indices = {
        @Index(value = {"config-id"}),
        @Index(value = {"data-used"}),
        @Index(value = {"sensor-name"}),
        @Index(value = {"sensor-timestamp"}),
        @Index(value = {"uid"})
})
public class LabeledData {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "experiment")
    private String experiment;

    @ColumnInfo(name = "config-id")
    private long configId;

    @ColumnInfo(name = "device-position")
    private String devicePosition;

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

    @ColumnInfo(name = "user-id")
    private String userId;

    @ColumnInfo(name = "activity")
    private String activity;


    @ColumnInfo(name = "data-used")
    private int isDataUsed;

    @ColumnInfo(name = "uid")
    private String uid;

    private final SensorBase sensor;

    public LabeledData(String experiment, SensorBase sensor, String devicePosition, String userId, String activity, long configId, long timestamp, String uid) {
        this.sensor = sensor;
        this.experiment = experiment;
        this.sensorName = sensor.getName();
        //this.timestamp = sensor.getTimestamp();
        this.timestamp = timestamp;
        this.frequency = sensor.getFrequency();
        this.sensorValues = sensor.getValuesString();
        this.power = sensor.getPower();
        this.isDataUsed = 0;
        this.devicePosition = devicePosition;
        this.userId = userId;
        this.activity = activity;
        this.configId = configId;
        this.uid = uid;
    }

    public long getConfigId() {
        return configId;
    }

    public void setConfigId(int configId) {
        this.configId = configId;
    }

    public String getUserId() {
        return userId;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDevicePosition() {
        return devicePosition;
    }

    public void setDevicePosition(String devicePosition) {
        this.devicePosition = devicePosition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
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

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String[] getCSVFormattedString() {
        String[] values = sensorValues.split(",");
        return new String[]{experiment, sensorName, String.valueOf(power), String.valueOf(frequency), String.valueOf(timestamp),
                            values[0], values.length > 1 ? values[1] : "-",
                            values.length > 2 ? values[2] : "-"};
    }

    public String[] getCSVHeaders() {
        return new String[]{"Experiment Name", "Sensor Name", "Power Consumption (mAh)", "Sensor Frequency (Hz)",
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
