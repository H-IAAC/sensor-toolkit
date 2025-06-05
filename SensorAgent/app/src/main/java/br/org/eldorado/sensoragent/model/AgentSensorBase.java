package br.org.eldorado.sensoragent.model;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;

import java.util.Arrays;

import br.org.eldorado.sensoragent.SensorAgentContext;
import br.org.eldorado.sensoragent.apiserver.APICommand;
import br.org.eldorado.sensoragent.apiserver.APIController;
import br.org.eldorado.sensoragent.util.Log;
import br.org.eldorado.sensoragent.util.RemoteApplicationTime;

public class AgentSensorBase
        implements SensorEventListener, Parcelable {

    public static final int TYPE_ACCELEROMETER = Sensor.TYPE_ACCELEROMETER;
    public static final int TYPE_LINEAR_ACCELEROMETER = Sensor.TYPE_LINEAR_ACCELERATION;
    public static final int TYPE_AMBIENT_TEMPERATURE = Sensor.TYPE_AMBIENT_TEMPERATURE;
    public static final int TYPE_GYROSCOPE = Sensor.TYPE_GYROSCOPE;
    public static final int TYPE_LIGHT = Sensor.TYPE_LIGHT;
    public static final int TYPE_PROXIMITY = Sensor.TYPE_PROXIMITY;
    public static final int TYPE_MAGNETIC_FIELD = Sensor.TYPE_MAGNETIC_FIELD;
    public static final int TYPE_GRAVITY = Sensor.TYPE_GRAVITY;
    public static final int TYPE_GPS = 100;

    // Location
    private static final long LOCATION_UPDATE_INTERVAL_MILLIS = 500;
    private static final long LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS = 50;
    private FusedLocationProviderClient gpsClient;
    private LocationCallback locationCalback;

    protected SensorManager sensorManager;
    protected long timestamp;
    protected float power;
    protected float[] values;
    protected Log log;
    protected int type;


    public AgentSensorBase(String sensorClass, int type) {
        this.type = type;
        log = new Log(sensorClass);
        if (type == TYPE_GPS) {
            gpsClient = LocationServices.getFusedLocationProviderClient(SensorAgentContext.getInstance().getContext());
            requestGPSUpdates();
        } else {
            this.sensorManager = (SensorManager) SensorAgentContext.getInstance().getContext().getSystemService(Context.SENSOR_SERVICE);
        }
    }

    public AgentSensorBase(Parcel in) {
        timestamp = in.readLong();
        in.readFloatArray(values);
        power = in.readFloat();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public float[] getValuesArray() {
        return values;
    }

    public float getPower() {
        return power;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        try {
            //if (power != event.sensor.getPower() || !Arrays.equals(event.values, values)) {
                /* TODO save data to database */
                /*log.i(event.sensor.getStringType() + " power: " + event.sensor.getPower() + " values: " +
                        (event.values == null ? "null" : event.values[0]));*/
                this.timestamp = System.currentTimeMillis();
                this.timestamp = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                this.power = event.sensor.getPower();
                this.values = Arrays.copyOf(event.values, event.values.length);
                //this.timestamp = System.currentTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                //this.timestamp = RemoteApplicationTime.getCurrentRemoteTimeMillis() + (event.timestamp - SystemClock.elapsedRealtimeNanos()) / 1000000;
                //this.timestamp = RemoteApplicationTime.getCurrentRemoteTimeMillis();

                /*APICommand cmd =
                        new APICommand(
                                APICommand.CommandType.TYPE_GET_SENSOR_DATA,
                                Arrays.asList(type, new Gson().toJson(AgentSensorBase.this, AgentSensorBase.class)));
                APIController.getInstance().sendForAll(cmd);*/
            //}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public void registerListener(Sensor sensor) {
        log.i("Starting sensor: " + sensor.getName());
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST, 1000);
    }

    public void unregisterListener() {
        log.i("Stopping sensor");
        if (type == TYPE_GPS) {
            if (locationCalback != null) {
                gpsClient.removeLocationUpdates(locationCalback);
            }
        } else {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(timestamp);
        dest.writeFloatArray(values);
        dest.writeFloat(power);
    }

    public static final Parcelable.Creator<AgentSensorBase> CREATOR = new Creator<AgentSensorBase>() {
        @Override
        public AgentSensorBase createFromParcel(Parcel source) {
            return new AgentSensorBase(source);
        }

        @Override
        public AgentSensorBase[] newArray(int size) {
            return new AgentSensorBase[size];
        }
    };


    public void requestGPSUpdates() {
        if (ActivityCompat.checkSelfPermission(SensorAgentContext.getInstance().getContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationRequest locationRequest = new LocationRequest();
            locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_MILLIS);
            locationRequest.setFastestInterval(LOCATION_FASTEST_UPDATE_INTERVAL_MILLIS);
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            locationCalback = new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    values[0] = (float)locationResult.getLastLocation().getLatitude();
                    values[1] = (float)locationResult.getLastLocation().getLongitude();
                    timestamp = locationResult.getLastLocation().getTime();
                    //timestamp = System.currentTimeMillis();
                    timestamp = RemoteApplicationTime.getCurrentRemoteTimeMillis();
                }
            };

            gpsClient.requestLocationUpdates(locationRequest, locationCalback, Looper.getMainLooper()).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    log.d("requestGPSUpdates onFailure - " + e.toString());
                }
            });
        } else {
            Toast.makeText(SensorAgentContext.getInstance().getContext(), "Missing GPS Permission", Toast.LENGTH_LONG).show();
            log.d("requestGPSUpdates - no GPS permission ");
        }
    }

    /*@Override
    public void onLocationChanged(Location location) {
        double timerefSeconds;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            timerefSeconds = ((double) location.getElapsedRealtimeNanos()) / NANOSECONDS_IN_SECOND;
        }
        else {
            timerefSeconds = ((double) location.getTime()) / MILLISECONDS_IN_SECOND;
        }
        Log.d(LOG_TAG,"got location update with time reference: " + timerefSeconds);

        addHighFrequencyMeasurement(LOC_TIME, timerefSeconds);
        // Should we send the exact coordinates?
        if ((!ESSettings.shouldUseLocationBubble()) ||
                (ESSettings.locationBubbleCenter() == null) ||
                (ESSettings.locationBubbleCenter().distanceTo(location) > LOCATION_BUBBLE_RADIUS_METERS)) {
            Log.i(LOG_TAG, "Sending location coordinates");
            addHighFrequencyMeasurement(LOC_LAT, location.getLatitude());
            addHighFrequencyMeasurement(LOC_LONG,location.getLongitude());
        }
        else {
            Log.i(LOG_TAG,"Hiding location coordinates (sending invalid coordinates). We're in the bubble.");
            addHighFrequencyMeasurement(LOC_LAT,LOC_LAT_HIDDEN);
            addHighFrequencyMeasurement(LOC_LONG,LOC_LONG_HIDDEN);
        }
        // Anyway, store the location coordinates separately:
        _locationCoordinatesData.get(LOC_LAT).add(location.getLatitude());
        _locationCoordinatesData.get(LOC_LONG).add(location.getLongitude());

        addHighFrequencyMeasurement(LOC_HOR_ACCURACY,location.hasAccuracy() ? location.getAccuracy() : LOC_ACCURACY_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_ALT,location.hasAltitude() ? location.getAltitude() : LOC_ALT_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_SPEED,location.hasSpeed() ? location.getSpeed() : LOC_SPEED_UNAVAILABLE);
        addHighFrequencyMeasurement(LOC_BEARING,location.hasBearing() ? location.getBearing() : LOC_BEARING_UNAVAILABLE);
    }*/
}
