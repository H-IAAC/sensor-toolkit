package br.org.eldorado.sensormanager.view.adapter;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensormanager.R;
import br.org.eldorado.sensormanager.util.Log;
import br.org.eldorado.sensorsdk.listener.SensorSDKListener;

public class SensorRecyclerViewAdapter  extends RecyclerView.Adapter<SensorRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "SensorRecyclerViewAdapter";
    private Log log;
    private List<SensorBase> mData;
    private LayoutInflater mInflater;
    private Context mContext;

    // Data is passed into the constructor
    public SensorRecyclerViewAdapter(Context context) {
        this.log = new Log(TAG);
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);

        this.mData = new ArrayList<SensorBase>(
                Arrays.asList(
                        new Accelerometer(), new AmbientTemperature(),
                        new Gyroscope(), new Luminosity(), new Proximity(),
                        new MagneticField()));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.sensor_recyclerview_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String sensorName = mData.get(position).getName();
        holder.sensor = mData.get(position);
        holder.sensor.registerListener(holder);
        holder.sensorName.setText(sensorName);
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, SensorSDKListener {
        public TextView sensorName;
        public TextView sensorConsumption;
        public TextView sensorValues;
        public Switch sensorSwitch;
        public SensorBase sensor;

        public ViewHolder(View itemView) {
            super(itemView);
            sensorName = (TextView) itemView.findViewById(R.id.sensor_name);
            sensorConsumption = (TextView) itemView.findViewById(R.id.sensor_consumption);
            sensorValues = (TextView) itemView.findViewById(R.id.sensor_values);
            sensorSwitch = (Switch) itemView.findViewById(R.id.sensor_switch);
            sensorSwitch.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemClick(view, getAdapterPosition());
        }

        @Override
        public void onSensorStarted() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    sensorSwitch.setChecked(true);
                }
            });
        }

        @Override
        public void onSensorStopped() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    sensorSwitch.setChecked(false);
                }
            });

        }

        @Override
        public void onSensorChanged() {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                public void run() {
                    sensorConsumption.setText(sensor.getPower()+"");
                    sensorValues.setText(sensor.getValuesString());
                }
            });
        }
    }

    public SensorBase getItem(int id) {
        return mData.get(id);
    }

    public void onItemClick(View view, int position) {
        if (view instanceof Switch) {
            if (((Switch) view).isChecked()) {
                log.i("Starting sensor " + getItem(position).getName());
                getItem(position).startSensor();
            } else {
                log.i("Stopping sensor " + getItem(position).getName());
                getItem(position).stopSensor();
            }
        }
    }
}