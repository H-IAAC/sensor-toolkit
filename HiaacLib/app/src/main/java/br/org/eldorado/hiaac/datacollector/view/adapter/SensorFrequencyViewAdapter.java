package br.org.eldorado.hiaac.datacollector.view.adapter;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.layout.AnimatedLinearLayout;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.sensoragent.model.GPS;
import br.org.eldorado.sensorsdk.SensorSDK;

public class SensorFrequencyViewAdapter extends RecyclerView.Adapter<SensorFrequencyViewAdapter.ViewHolder> {
    private final LayoutInflater mInflater;
    private List<SelectedSensorFrequency> mSelectedSensors;
    private SensorFrequencyChangeListener mListener;
    private final Context mContext;
    private ViewHolder gpsHolder;
    private final Log log;

    public static List<Integer> frequencyOptions = new ArrayList<Integer>(
            Arrays.asList(  0,
                            1,
                            10,
                            20,
                            30,
                            40,
                            50,
                            100,
                            200
                        )
    );

    public SensorFrequencyViewAdapter(Context context, SensorFrequencyChangeListener listener) {
        log = new Log("SensorFrequencyViewAdapter");
        mInflater = LayoutInflater.from(context);
        mContext = context;
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
        addFrequency(selectedSensorFrequency.getFrequency());

        ArrayList<String> list = Tools.createHertzList(frequencyOptions);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(frequenciesSpinner.getContext(),
                R.layout.custom_spinner, list);
        frequenciesSpinner.setAdapter(adapter);
        frequenciesSpinner.setSelection(getFrequencySpinnerPositionForSelected(selectedSensorFrequency));
        if (selectedSensorFrequency.isSelected()) {
            frequencyContainer.expand(60);
            checkBox.setChecked(true);
        }
        frequenciesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    /* Abrir popup */
                    final AlertDialog.Builder d = new AlertDialog.Builder(mContext);
                    LayoutInflater inflater = ((Activity)(mContext)).getLayoutInflater();
                    View dialogView = inflater.inflate(R.layout.number_picker_dialog, null);
                    d.setTitle(R.string.add_new_frequency_title);
                    d.setMessage(R.string.add_new_frequency_msg);
                    d.setView(dialogView);
                    final NumberPicker numberPicker = (NumberPicker) dialogView.findViewById(R.id.dialog_number_picker);
                    numberPicker.setMaxValue(200);
                    numberPicker.setMinValue(1);
                    numberPicker.setWrapSelectorWheel(true);
                    d.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            int newFreq = numberPicker.getValue();
                            addFrequency(newFreq);
                            adapter.clear();
                            adapter.addAll(Tools.createHertzList(frequencyOptions));
                            frequenciesSpinner.setAdapter(adapter);
                            selectedSensorFrequency.setFrequency(newFreq);
                            frequenciesSpinner.setSelection(getFrequencySpinnerPositionForSelected(selectedSensorFrequency));
                            notifySensorFrequencyChanged();
                        }
                    });
                    d.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            frequenciesSpinner.setSelection(getFrequencySpinnerPositionForSelected(selectedSensorFrequency));
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog alertDialog = d.create();
                    alertDialog.show();
                } else {
                    selectedSensorFrequency.setFrequency(frequencyOptions.get(position));
                    notifySensorFrequencyChanged();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        checkBox.setChecked(selectedSensorFrequency.isSelected());
        checkBox.setText(selectedSensorFrequency.sensor);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleCheckBox(holder, selectedSensorFrequency, frequencyContainer);
            }
        });
        if (checkBox.getText().equals("GPS")) {
            gpsHolder = holder;
        }
    }

    private int getFrequencySpinnerPositionForSelected(SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency) {
        int spinnerPosition = 1;
        for (int i = 1; i < frequencyOptions.size(); i++) {
            if (selectedSensorFrequency.getFrequency() == frequencyOptions.get(i)) {
                spinnerPosition = i;
                break;
            }
        }
        selectedSensorFrequency.setFrequency(spinnerPosition);
        return spinnerPosition;
    }

    public void setFrequencyForAll(int freq) {
        addFrequency(freq);
        for (SelectedSensorFrequency sensor : mSelectedSensors) {
            
            if (sensor.isSelected) {
                sensor.setFrequency(freq);
            }
        }
    }

    public void addFrequency(int freq) {
        if (!frequencyOptions.contains(freq) && freq > 0 && freq < 200) {
            frequencyOptions.add(freq);
            Collections.sort(frequencyOptions);
        }
    }


    private int nOfCheckedSensors = 0;
    private void handleCheckBox(ViewHolder holder, SelectedSensorFrequency selectedSensorFrequency, AnimatedLinearLayout frequencyContainer) {
        if (GPS.TAG.equals(holder.getSelectSensorCheckBox().getText())) {
            // Check if we have GPS permission
            //gpsHolder = holder;
            checkGPSPermission();
        } else {
            boolean isSensorAvailable = checkSensorAvailability(selectedSensorFrequency.sensor);
            if (holder.getSelectSensorCheckBox().isChecked()
                    && isSensorAvailable) {
                selectedSensorFrequency.setSelected(true);
                frequencyContainer.expand(60);
                nOfCheckedSensors++;
                if (nOfCheckedSensors == 3) {
                    showToManySensorsSelectedWarning();
                }
            } else {
                holder.getSelectSensorCheckBox().setChecked(false);
                selectedSensorFrequency.setSelected(false);
                frequencyContainer.close();

                if (isSensorAvailable) {
                    nOfCheckedSensors--;
                }
            }

            notifySensorFrequencyChanged();
        }
    }

    private void showToManySensorsSelectedWarning() {
        AlertDialog alert = new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.too_many_sensors_checked))
                .setCancelable(false)
                .setPositiveButton(mContext.getString(R.string.gps_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        alert.setTitle(mContext.getString(R.string.dialog_alert_title));
        alert.show();
    }

    public void checkGPSPermission() {
        CheckBox gpsCheckBox = gpsHolder.getSelectSensorCheckBox();
        boolean show = ActivityCompat.shouldShowRequestPermissionRationale((Activity)mContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        if (show) {
            AlertDialog alert = new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.gps_permission_description))
                    .setCancelable(false)
                    .setPositiveButton(mContext.getString(R.string.gps_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            requestGPSPermission();
                        }
                    }).setNegativeButton(mContext.getString(R.string.dont_use_gps), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            updateGPS(PackageManager.PERMISSION_DENIED);
                        }
                    }).create();
            alert.setTitle(mContext.getString(R.string.dialog_alert_title));
            alert.show();
        } else {
            requestGPSPermission();
        }
    }

    public boolean checkSensorAvailability(String sensorName) {
        if (sensorName.equalsIgnoreCase("gps")) return true;
        boolean isAvailable = true;
        if (!SensorSDK.getInstance().checkSensorAvailability(Tools.getSensorFromTitleName(sensorName).getType())) {
            AlertDialog alert = new AlertDialog.Builder(mContext)
                    .setMessage(mContext.getString(R.string.sensor_not_available, sensorName))
                    .setCancelable(false)
                    .setPositiveButton(mContext.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create();
            alert.setTitle(mContext.getString(R.string.dialog_alert_title));
            alert.show();
            isAvailable = false;
        }
        return isAvailable;
    }


    private void requestGPSPermission() {
        log.d("requestGPSPermission");
        ActivityCompat.requestPermissions((Activity)mContext, new String[] {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION },
                100);
    }

    public void requestBackgroundPermission() {
        ActivityCompat.requestPermissions((Activity)mContext, new String[] {
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION},
                101);
    }

    public void updateGPS(int permission) {
        if (gpsHolder != null && gpsHolder.getSelectSensorCheckBox() != null) {
            CheckBox gpsCheckBox = gpsHolder.getSelectSensorCheckBox();
            if (permission == PackageManager.PERMISSION_GRANTED) {
                gpsCheckBox.setEnabled(true);
                if (gpsCheckBox.isChecked()) {
                    mSelectedSensors.get(gpsHolder.getAdapterPosition()).setSelected(true);
                    gpsHolder.getFrequencyContainer().expand(60);
                } else {
                    mSelectedSensors.get(gpsHolder.getAdapterPosition()).setSelected(false);
                    gpsHolder.getFrequencyContainer().close();
                }
                notifySensorFrequencyChanged();
            } else {
                Toast.makeText(mContext, "GPS permission not granted", Toast.LENGTH_SHORT).show();
                gpsCheckBox.setEnabled(false);
                gpsCheckBox.setChecked(false);
                mSelectedSensors.get(gpsHolder.getAdapterPosition()).setSelected(false);
                gpsHolder.getFrequencyContainer().close();
            }
        }
    }

    @Override
    public int getItemCount() {
        return mSelectedSensors.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
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
