package br.org.eldorado.hiaac.view.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.layout.AnimatedLinearLayout;
import br.org.eldorado.hiaac.util.Tools;
import br.org.eldorado.sensoragent.model.SensorBase;

public class SensorFrequencyViewAdapter extends RecyclerView.Adapter<SensorFrequencyViewAdapter.ViewHolder> {
    private final LayoutInflater mInflater;
    private List<SelectedSensorFrequency> mSelectedSensors;
    private SensorFrequencyChangeListener mListener;

    public static final int[] frequencyOptions = {
            10,
            50,
            500,
            1000
    };

    public SensorFrequencyViewAdapter(Context context, SensorFrequencyChangeListener listener) {
        mInflater = LayoutInflater.from(context);
        mSelectedSensors = new ArrayList<>();
        mListener = listener;
    }

    public void setSelectedSensors(List<SelectedSensorFrequency> selectedSensors) {
        this.mSelectedSensors = selectedSensors;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.sensor_frequency_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CheckBox checkBox = holder.getSelectSensorCheckBox();
        AnimatedLinearLayout frequencyContainer = holder.getFrequencyContainer();
        Spinner frequenciesSpinner = holder.getFrequenciesSpinner();
        SelectedSensorFrequency selectedSensorFrequency = mSelectedSensors.get(position);

        ArrayList<String> list = Tools.createHertzList(frequencyOptions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(frequenciesSpinner.getContext(),
                R.layout.custom_spinner, list);
        frequenciesSpinner.setAdapter(adapter);
        if (selectedSensorFrequency.isSelected) {
            frequencyContainer.expand(60);
            checkBox.setChecked(true);
        }

        int spinnerPosition = 0;
        for (int i = 0; i < frequencyOptions.length; i++) {
            if (selectedSensorFrequency.getFrequency() == frequencyOptions[i]) {
                spinnerPosition = i;
                break;
            }
        }
        frequenciesSpinner.setSelection(spinnerPosition);

        frequenciesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSensorFrequency.setFrequency(frequencyOptions[position]);
                notifySensorFrequencyChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        checkBox.setText(selectedSensorFrequency.sensor);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkBox.isChecked()) {
                    selectedSensorFrequency.setSelected(true);
                    frequencyContainer.expand(60);
                } else {
                    selectedSensorFrequency.setSelected(false);
                    frequencyContainer.close();
                }
                notifySensorFrequencyChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mSelectedSensors.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private CheckBox selectSensorCheckBox;
        private AnimatedLinearLayout frequencyContainer;
        private Spinner frequenciesSpinner;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            selectSensorCheckBox = itemView.findViewById(R.id.select_sensor_check_box);
            frequencyContainer = itemView.findViewById(R.id.frequency_container);
            frequenciesSpinner = itemView.findViewById(R.id.frequencies_spinner);
        }

        public CheckBox getSelectSensorCheckBox() {
            return selectSensorCheckBox;
        }

        public AnimatedLinearLayout getFrequencyContainer() {
            return frequencyContainer;
        }

        public Spinner getFrequenciesSpinner() {
            return frequenciesSpinner;
        }
    }

    private void notifySensorFrequencyChanged() {
        mListener.onSensorFrequencyChanged(mSelectedSensors);
    }

    public interface SensorFrequencyChangeListener {
        void onSensorFrequencyChanged(List<SelectedSensorFrequency> selectedSensorFrequencies);
    }

    public static class SelectedSensorFrequency {
        private boolean isSelected;
        private String sensor;
        private int frequency;

        public SelectedSensorFrequency(boolean isSelected, String sensor, int frequency) {
            this.isSelected = isSelected;
            this.sensor = sensor;
            this.frequency = frequency;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public void setSelected(boolean selected) {
            isSelected = selected;
        }

        public String getSensor() {
            return sensor;
        }

        public int getFrequency() {
            return frequency;
        }

        public void setFrequency(int frequency) {
            this.frequency = frequency;
        }
    }
}
