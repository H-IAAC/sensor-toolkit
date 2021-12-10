package br.org.eldorado.hiaac;

import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.NEW_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.view.adapter.SensorFrequencyViewAdapter.frequencyOptions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.data.LabelConfig;
import br.org.eldorado.hiaac.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.data.SensorFrequency;
import br.org.eldorado.hiaac.util.Tools;
import br.org.eldorado.hiaac.view.adapter.SensorFrequencyViewAdapter;
import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;

public class LabelOptionsActivity extends AppCompatActivity {
    public static final int MINUTE = 60;
    public static final int HOUR = 3600;

    private final int[] stopTimeOptions = {
            5,
            30,
            1 * MINUTE,
            5 * MINUTE,
            10 * MINUTE
    };

    private LabelConfigViewModel mLabelConfigViewModel;
    private LabelConfig mCurrentConfig;
    private boolean mIsUpdating;
    private List<SensorFrequency> mSensorFrequencies;
    private List<SensorFrequencyViewAdapter.SelectedSensorFrequency> mSelectedSensors;

    private SensorFrequencyViewAdapter mSensorFrequencyViewAdapter;
    private EditText mLabelTile;
    private Spinner mStopTimeSpinner;

    private SensorFrequencyViewAdapter.SensorFrequencyChangeListener mSensorFrequencyChangeListener =
            new SensorFrequencyViewAdapter.SensorFrequencyChangeListener() {
                @Override
                public void onSensorFrequencyChanged(List<SensorFrequencyViewAdapter.SelectedSensorFrequency> selectedSensorFrequencies) {
                    mSelectedSensors = selectedSensorFrequencies;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_options);
        mLabelTile = findViewById(R.id.edit_label_name);
        mStopTimeSpinner = findViewById(R.id.stops_at_spinner);
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);

        ArrayList<String> list = Tools.createTimeFormatedList(stopTimeOptions);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(this,
                R.layout.custom_spinner, list);
        mStopTimeSpinner.setAdapter(arrayAdapter);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.sensors_recycler_view);
        mSensorFrequencyViewAdapter =
                new SensorFrequencyViewAdapter(this, mSensorFrequencyChangeListener);
        recyclerView.setAdapter(mSensorFrequencyViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Bundle extras = getIntent().getExtras();
        int activityType = extras.getInt(LABEL_CONFIG_ACTIVITY_TYPE);
        switch (activityType) {
            case NEW_LABEL_CONFIG_ACTIVITY:
                mSelectedSensors = getSelectedSensorFrequenciesFromSensorFrequencies();
                mSensorFrequencyViewAdapter.setSelectedSensors(mSelectedSensors);
                break;
            case UPDATE_LABEL_CONFIG_ACTIVITY:
                mIsUpdating = true;
                String lableId = extras.getString(LABEL_CONFIG_ACTIVITY_ID);
                mLabelConfigViewModel.getLabelConfigById(lableId)
                        .observe(this, new Observer<LabelConfig>() {
                            @Override
                            public void onChanged(LabelConfig labelConfig) {
                                mCurrentConfig = labelConfig;
                                updateFields();
                            }
                        });
                break;
            default:
                mIsUpdating = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_label_options, menu);

        if (mIsUpdating) {
            menu.getItem(0).setVisible(true);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_label_button:
                DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment(
                        new DeleteDialogListener() {
                            @Override
                            public void onConfirmClick() {
                                Toast.makeText(getApplicationContext(),
                                        R.string.configuration_deleted, Toast.LENGTH_LONG).show();
                                deleteCurrentConfig();
                            }
                        }
                );
                deleteDialogFragment.show(getSupportFragmentManager().beginTransaction(),
                        DeleteDialogFragment.class.toString());
                return true;
            case R.id.save_label_button:
                onSaveButtonClick();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentConfig() {
        if (mCurrentConfig != null) {
            mLabelConfigViewModel.deleteConfig(mCurrentConfig);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void updateFields() {
        if (mCurrentConfig != null) {
            mLabelTile.setText(mCurrentConfig.label);
            int position = 0;
            for (int i = 0; i < stopTimeOptions.length; i++) {
                if (mCurrentConfig.stopTime == stopTimeOptions[i]) {
                    position = i;
                    break;
                }
            }
            mStopTimeSpinner.setSelection(position);
            populateRecyclerView();
        }
    }

    private void populateRecyclerView() {
        mLabelConfigViewModel.getAllSensorsFromLabel(mCurrentConfig.label)
                .observe(this, new Observer<List<SensorFrequency>>() {
                    @Override
                    public void onChanged(List<SensorFrequency> sensorFrequencies) {
                        mSensorFrequencies = sensorFrequencies;
                        mSelectedSensors = getSelectedSensorFrequenciesFromSensorFrequencies();
                        mSensorFrequencyViewAdapter.setSelectedSensors(mSelectedSensors);
                    }
                });
    }

    private List<SensorFrequencyViewAdapter.SelectedSensorFrequency> getSelectedSensorFrequenciesFromSensorFrequencies() {
        List<SensorFrequencyViewAdapter.SelectedSensorFrequency> selectedSensorFrequencies = new ArrayList<>();
        Map<Integer, Integer> sensorTypeFrequencyMap = new HashMap<>();
        if (mSensorFrequencies != null) {
            for (SensorFrequency sensorFrequency : mSensorFrequencies) {
                sensorTypeFrequencyMap.put(sensorFrequency.sensor.getType(), sensorFrequency.frequency);
            }
        }

        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_ACCELEROMETER, Accelerometer.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_AMBIENT_TEMPERATUR, AmbientTemperature.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_GYROSCOPE, Gyroscope.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_LUMINOSITY, Luminosity.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_PROXIMITY, Proximity.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_MAGNETIC_FIELD, MagneticField.TAG));

        return selectedSensorFrequencies;
    }

    private SensorFrequencyViewAdapter.SelectedSensorFrequency createSelectedSensorFrequency(
            Map<Integer, Integer> sensorTypeFrequencyMap, int sensorType, String sensorName) {
        Integer frequency = sensorTypeFrequencyMap.get(sensorType);

        SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency =
                new SensorFrequencyViewAdapter.SelectedSensorFrequency(
                        frequency != null,
                        sensorName,
                        frequency == null ? frequencyOptions[0] : frequency.intValue()
                );

        return selectedSensorFrequency;
    }

    private List<SensorFrequency> getSensorFrequenciesFromSelectedSensorFrequencies(String label) {
        List<SensorFrequency> sensorFrequencies = new ArrayList<>();
        for (SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency : mSelectedSensors) {
            if (selectedSensorFrequency.isSelected()) {
                SensorFrequency sensorFrequency = new SensorFrequency(
                        label,
                        getSensorFromTitleName(selectedSensorFrequency.getSensor()),
                        selectedSensorFrequency.getFrequency());
                sensorFrequencies.add(sensorFrequency);
            }
        }

        return sensorFrequencies;
    }

    private SensorBase getSensorFromTitleName(String title) {
        switch (title) {
            case Accelerometer.TAG:
                return new Accelerometer();
            case AmbientTemperature.TAG:
                return new AmbientTemperature();
            case Gyroscope.TAG:
                return new Gyroscope();
            case Luminosity.TAG:
                return new Luminosity();
            case MagneticField.TAG:
                return new MagneticField();
            case Proximity.TAG:
                return new Proximity();
        }
        return null;
    }

    private void onSaveButtonClick() {
        String label = mLabelTile.getText().toString().trim();
        if (label.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    R.string.label_title_empty, Toast.LENGTH_LONG).show();
            return;
        }

        int spinnerPosition = mStopTimeSpinner.getSelectedItemPosition();
        int stopTime = stopTimeOptions[spinnerPosition];
        LabelConfig newConfig = new LabelConfig(label, stopTime);
        if (mIsUpdating && mCurrentConfig != null) {
            if (label == mCurrentConfig.label) {
                mLabelConfigViewModel.updateConfig(newConfig);
            } else {
                mLabelConfigViewModel.deleteConfig(mCurrentConfig);
                mLabelConfigViewModel.insertNewConfig(newConfig);
            }
        } else {
            mLabelConfigViewModel.insertNewConfig(newConfig);
        }
        if (mSensorFrequencies != null) {
            mLabelConfigViewModel.deleteAllSensorFrequencies(mSensorFrequencies);
        }
        mLabelConfigViewModel.insertAllSensorFrequencies(
                getSensorFrequenciesFromSelectedSensorFrequencies(label)
        );
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    public static class DeleteDialogFragment extends DialogFragment {
        private DeleteDialogListener mListener;

        public DeleteDialogFragment(DeleteDialogListener listener) {
            this.mListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.delete_config_confirmation)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onConfirmClick();
                        }
                    })
                    .setNegativeButton(R.string.no, null);

            return builder.create();
        }
    }

    public interface DeleteDialogListener {
        void onConfirmClick();
    }
}