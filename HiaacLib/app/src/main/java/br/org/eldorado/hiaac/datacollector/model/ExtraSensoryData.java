package br.org.eldorado.hiaac.datacollector.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.org.eldorado.sensoragent.model.SensorBase;

public class ExtraSensoryData {

    private Map<String, Double> features;

    public static enum FeatureName {
        //Magnitude Stats (7)
        MAGNITUDE_MEAN(":magnitude_stats:mean"),
        MAGNITUDE_STD(":magnitude_stats:std"),
        MAGNITUDE_MOMENT3(":magnitude_stats:moment3"),
        MAGNITUDE_MOMENT4(":magnitude_stats:moment4"),
        MAGNITUDE_PERCENTILE25(":magnitude_stats:percentile25"),
        MAGNITUDE_PERCENTILE50(":magnitude_stats:percentile50"),
        MAGNITUDE_PERCENTILE75(":magnitude_stats:percentile75"),

        //Magnitude Entropies (2)
        MAGNITUDE_VALUE_ENTROPY(":magnitude_stats:value_entropy"),
        MAGNITUDE_TIME_ENTROPY(":magnitude_stats:time_entropy"),

        //Magnitude Spectrum (6)
        MAGNITUDE_SPECTRUM_LOG_BAND0(":magnitude_spectrum:log_energy_band0"),
        MAGNITUDE_SPECTRUM_LOG_BAND1(":magnitude_spectrum:log_energy_band1"),
        MAGNITUDE_SPECTRUM_LOG_BAND2(":magnitude_spectrum:log_energy_band2"),
        MAGNITUDE_SPECTRUM_LOG_BAND3(":magnitude_spectrum:log_energy_band3"),
        MAGNITUDE_SPECTRUM_LOG_BAND4(":magnitude_spectrum:log_energy_band4"),
        MAGNITUDE_SPECTRAL_ENTROPY(":magnitude_spectrum:spectral_entropy"),

        //Magnitude Autocorrelation (2)
        MAGNITUDE_AUTOCORRELATION_PERIOD(":magnitude_autocorrelation:period"),
        MAGNITUDE_AUTOCORRELATION_NORMALIZED_AC(":magnitude_autocorrelation:normalized_ac"),

        // 3D Features (9)
        THREE_D_MEAN_X(":3d:mean_x"),
        THREE_D_MEAN_Y(":3d:mean_y"),
        THREE_D_MEAN_Z(":3d:mean_z"),
        THREE_D_STD_X(":3d:std_x"),
        THREE_D_STD_Y(":3d:std_y"),
        THREE_D_STD_Z(":3d:std_z"),
        THREE_D_RO_XY(":3d:ro_xy"),
        THREE_D_RO_XZ(":3d:ro_xz"),
        THREE_D_RO_YZ(":3d:ro_yz");


        private final String value;

        FeatureName(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private String[] csvHeaders;

    public ExtraSensoryData() {
        this.features = new LinkedHashMap<>();
    }
    public boolean isSensorSupported(int type) {
        return type == SensorBase.TYPE_ACCELEROMETER || type == SensorBase.TYPE_GYROSCOPE || type == SensorBase.TYPE_MAGNETIC_FIELD;
    }

    public void addFeature(FeatureName name, int sensor, double value) {
        features.put(getSensorPrefix(sensor) + name.getValue(), value);
    }

    public String[] getCsvHeaders() {
        if (csvHeaders == null) {
            csvHeaders = features.keySet().toArray(new String[0]);
        }
        return csvHeaders;
    }

    public String[] getCsvValues() {
        List<String> values = new ArrayList<String>(csvHeaders.length);
        for (String key : csvHeaders) {
            values.add(String.valueOf(features.get(key)));
        }
        return values.toArray(new String[0]);
    }

    private String getSensorPrefix(int sensor) {
        switch (sensor) {
            case SensorBase.TYPE_ACCELEROMETER:
                return "raw_acc";
            case SensorBase.TYPE_GYROSCOPE:
                return "proc_gyro";
            case SensorBase.TYPE_MAGNETIC_FIELD:
                return "raw_magnet";
            default:
                return "unsupported";
        }
    }
}
