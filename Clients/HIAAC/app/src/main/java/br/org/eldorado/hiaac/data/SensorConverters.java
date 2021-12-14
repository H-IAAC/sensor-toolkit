package br.org.eldorado.hiaac.data;

import androidx.room.TypeConverter;

import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;

public class SensorConverters {
    @TypeConverter
    public static SensorBase fromName(String name) {
        switch (name) {
            case "Accelerometer":
                return new Accelerometer();
            case "AmbientTemperature":
                return new AmbientTemperature();
            case "Gyroscope":
                return new Gyroscope();
            case "Luminosity":
                return new Luminosity();
            case "MagneticField":
                return new MagneticField();
            case "Proximity":
                return new Proximity();
        }
        return null;
    }

    @TypeConverter
    public static String fromSensorBase(SensorBase sensorBase) {
        return sensorBase == null ? null : sensorBase.getClass().getSimpleName();
    }
}
