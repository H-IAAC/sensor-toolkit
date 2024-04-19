package br.org.eldorado.hiaac.datacollector;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.NEW_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.UPDATE_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.datacollector.view.adapter.SensorFrequencyViewAdapter.frequencyOptions;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ApiInterface;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
import br.org.eldorado.hiaac.datacollector.api.StatusResponse;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;
import br.org.eldorado.hiaac.datacollector.util.CsvFiles;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.hiaac.datacollector.view.adapter.SensorFrequencyViewAdapter;
import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.GPS;
import br.org.eldorado.sensoragent.model.Gravity;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.LinearAccelerometer;
import br.org.eldorado.sensoragent.model.Luminosity;
import br.org.eldorado.sensoragent.model.MagneticField;
import br.org.eldorado.sensoragent.model.Proximity;
import br.org.eldorado.sensoragent.model.SensorBase;
import br.org.eldorado.sensorsdk.SensorSDK;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelOptionsActivity extends AppCompatActivity {
    public static final int MINUTE = 60;
    public static final int HOUR = 3600;
    public static final int DAY = 24 * HOUR;

    private final int[] stopTimeOptions = {
            5,
            15,
            30,
            45,
            1 * MINUTE,
            2 * MINUTE,
            3 * MINUTE,
            3 * MINUTE + 30,
            5 * MINUTE,
            10 * MINUTE,
            30 * MINUTE
    };

    private LabelConfigViewModel mLabelConfigViewModel;
    private LabelConfig mCurrentConfig;
    private boolean mIsUpdating;
    private List<SensorFrequency> mSensorFrequencies;
    private List<SensorFrequencyViewAdapter.SelectedSensorFrequency> mSelectedSensors;
    private SensorFrequencyViewAdapter mSensorFrequencyViewAdapter;
    private EditText mLabelTile;
    private EditText mActivityTxt;
    private EditText mScheduleTimeTxt;
    private TimePickerDialog timePickerDialog;
    private Spinner mStopTimeSpinner;
    private Spinner mDeviceLocation;
    private CheckBox mSendFilesToServer;
    private EditText mUserIdTxt;
    private TextView mWarnTxt;
    @SuppressLint("RestrictedApi")
    private ActionMenuItemView mLoadConfigBtn;
    private static final String TAG = "LabelOptionsActivity";
    private Log log;
    private boolean isConfigLoaded;
    private CsvFiles csvFiles;
    private int lastPosition = 1;
    private Context appContext;
    private final SensorFrequencyViewAdapter.SensorFrequencyChangeListener mSensorFrequencyChangeListener =
            new SensorFrequencyViewAdapter.SensorFrequencyChangeListener() {
                @Override
                public void onSensorFrequencyChanged(List<SensorFrequencyViewAdapter.SelectedSensorFrequency> selectedSensorFrequencies) {
                    mSelectedSensors = selectedSensorFrequencies;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        log = new Log(TAG);
        appContext = this.getApplicationContext();
        Preferences.init(this.getApplicationContext());
        setContentView(R.layout.activity_label_options);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLabelTile = findViewById(R.id.edit_label_name);
        mActivityTxt = findViewById(R.id.activity_txt);
        isConfigLoaded = false;
        csvFiles = new CsvFiles(this.getApplicationContext());

        mScheduleTimeTxt = findViewById(R.id.txtScheduleTime);
        mScheduleTimeTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                // Increment 5 min (300000 min), to always suggest schedule time 5 min ahead
                now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime() + 300000);

                timePickerDialog = new TimePickerDialog(LabelOptionsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mScheduleTimeTxt.setText(String.format("%02d", hourOfDay) + ":" + String.format("%02d", minute));
                    }

                }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

                timePickerDialog.show();
                timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timePickerDialog.cancel();
                        mScheduleTimeTxt.setText("");
                    }
                });

                // Disable alarm when user manually clicks start button
                AlarmConfig.cancelAlarm();
            }
        });

        mStopTimeSpinner = findViewById(R.id.stops_at_spinner);
        mDeviceLocation = findViewById(R.id.device_location_spinner);
        mUserIdTxt = findViewById(R.id.user_id_txt);
        mWarnTxt = findViewById(R.id.edit_warn_txt);
        mSendFilesToServer = findViewById(R.id.send_files_to_server_checkbox);
        mSendFilesToServer.setVisibility(View.INVISIBLE);
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);

        ArrayList<String> list = Tools.createTimeFormatedList(stopTimeOptions);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(this,
                R.layout.custom_spinner, list);
        mStopTimeSpinner.setAdapter(arrayAdapter);

        final ArrayAdapter<String> deviceLocationAdapter = new ArrayAdapter(this,
                R.layout.custom_spinner, Preferences.getDeviceLocationsList());
        mDeviceLocation.setAdapter(deviceLocationAdapter);
        mDeviceLocation.setSelection(1);

        mDeviceLocation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                if (position == 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                    builder.setTitle(getResources().getString(R.string.device_location_add));

                    // Set up the input
                    final EditText input = new EditText(LabelOptionsActivity.this);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton(getResources().getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (input.getText().toString().isEmpty()) {
                                dialog.cancel();
                                mDeviceLocation.setSelection(lastPosition);
                            } else {
                                String newLocation = input.getText().toString();
                                deviceLocationAdapter.add(newLocation);
                                deviceLocationAdapter.sort(new Comparator<String>() {
                                    @Override
                                    public int compare(String s, String t1) {
                                        return s.toLowerCase().compareTo(t1.toLowerCase());
                                    }
                                });
                                mDeviceLocation.setSelection(deviceLocationAdapter.getPosition(newLocation));
                                Preferences.addNewDeviceLocation(newLocation);
                            }
                        }
                    });
                    builder.setNegativeButton(getResources().getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            mDeviceLocation.setSelection(lastPosition);
                        }
                    });

                    builder.show();
                } else {
                    lastPosition = position;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.sensors_recycler_view);
        mSensorFrequencyViewAdapter =
                new SensorFrequencyViewAdapter(this, mSensorFrequencyChangeListener);
        recyclerView.setAdapter(mSensorFrequencyViewAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Bundle extras = getIntent().getExtras();
        int activityType = extras.getInt(LABEL_CONFIG_ACTIVITY_TYPE);
        switch (activityType) {
            case NEW_LABEL_CONFIG_ACTIVITY:
                mIsUpdating = false;
                mSelectedSensors = getSelectedSensorFrequenciesFromSensorFrequencies();
                mSensorFrequencyViewAdapter.setSelectedSensors(mSelectedSensors);
                break;
            case UPDATE_LABEL_CONFIG_ACTIVITY:
                mIsUpdating = true;
                long labelId = extras.getLong(LABEL_CONFIG_ACTIVITY_ID);
                if (csvFiles.getFiles(labelId).size() > 0) {
                    mLabelTile.setEnabled(false);
                    mActivityTxt.setEnabled(false);
                    mUserIdTxt.setEnabled(false);
                    mWarnTxt.setVisibility(View.VISIBLE);
                }

                mLabelConfigViewModel.getLabelConfigById(labelId)
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (100 == requestCode) {
            log.d("request fine_location");
            mSensorFrequencyViewAdapter.requestBackgroundPermission();
        } else if (101 == requestCode) {
            log.d("request background");
            mSensorFrequencyViewAdapter.updateGPS(grantResults[0]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_label_options, menu);

        if (mIsUpdating) {
            menu.getItem(0).setVisible(true);
            menu.getItem(1).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_label_button) {
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
        } else if (item.getItemId() == R.id.save_label_button) {
            onSaveButtonClick();
            return true;
        } else if (item.getItemId() == R.id.load_config_button) {
            getAllExperiments();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        closeActivity();
        return true;
    }

    private void deleteCurrentConfig() {
        if (mCurrentConfig != null) {
            mLabelConfigViewModel.deleteConfig(mCurrentConfig);
            if (mSensorFrequencies != null) {
                mLabelConfigViewModel.deleteAllSensorFrequencies(mSensorFrequencies);
            } else {
                mLabelConfigViewModel.deleteSensorsFromLabel(mCurrentConfig);
            }
            closeActivity();
        }
    }

    private void updateFields() {
        if (mCurrentConfig != null) {
            mLabelTile.setText(mCurrentConfig.experiment);
            mActivityTxt.setText(mCurrentConfig.activity);

            if (mCurrentConfig.scheduledTime > 0) {
                DateFormat df = new SimpleDateFormat("HH:mm");
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(mCurrentConfig.scheduledTime);
                mScheduleTimeTxt.setText(df.format(c.getTime()));
            }

            mUserIdTxt.setText(mCurrentConfig.userId);
            mSendFilesToServer.setChecked(mCurrentConfig.sendToServer);
            int position = 0;
            for (int i = 0; i < stopTimeOptions.length; i++) {
                if (mCurrentConfig.stopTime == stopTimeOptions[i]) {
                    position = i;
                    break;
                }
            }
            mStopTimeSpinner.setSelection(position);

            for (int i = 0; i < mDeviceLocation.getCount(); i++) {
                if (mCurrentConfig.deviceLocation.equals(mDeviceLocation.getItemAtPosition(i))) {
                    mDeviceLocation.setSelection(i);
                    break;
                }
            }
            populateRecyclerView();
        }
    }

    private void populateRecyclerView() {
        mLabelConfigViewModel.getAllSensorsFromLabel(mCurrentConfig.id)
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
                SensorBase.TYPE_LINEAR_ACCELEROMETER, LinearAccelerometer.TAG));
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
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_GRAVITY, Gravity.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_GPS, GPS.TAG));

        return selectedSensorFrequencies;
    }

    private SensorFrequencyViewAdapter.SelectedSensorFrequency createSelectedSensorFrequency(
            Map<Integer, Integer> sensorTypeFrequencyMap, int sensorType, String sensorName) {
        Integer frequency = sensorTypeFrequencyMap.get(sensorType);

        return new SensorFrequencyViewAdapter.SelectedSensorFrequency(
                        frequency != null,
                        sensorName,
                        frequency == null ? frequencyOptions.get(0) : frequency.intValue());
    }

    private List<SensorFrequency> getSensorFrequenciesFromSelectedSensorFrequencies(long config_id) {
        List<SensorFrequency> sensorFrequencies = new ArrayList<>();
        for (SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency : mSelectedSensors) {
            if (selectedSensorFrequency.isSelected()) {
                SensorFrequency sensorFrequency = new SensorFrequency(
                        config_id,
                        Tools.getSensorFromTitleName(selectedSensorFrequency.getSensor()),
                        selectedSensorFrequency.getFrequency());
                sensorFrequencies.add(sensorFrequency);
            }
        }
        log.d("LISTA DE SENSORES " + sensorFrequencies);
        return sensorFrequencies;
    }

    private void onSaveButtonClick() {
        String label = mLabelTile.getText().toString().trim();
        if (label.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    R.string.label_title_empty, Toast.LENGTH_LONG).show();
            return;
        }

        String activity = mActivityTxt.getText().toString().trim();
        if (activity.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    R.string.activity_title_empty, Toast.LENGTH_LONG).show();
            return;
        }

        String scheduledTimeStr = mScheduleTimeTxt.getText().toString().trim();
        long scheduledTime = 0;
        if (!scheduledTimeStr.isEmpty()) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());
            String[] timeSplit = scheduledTimeStr.split(":");
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeSplit[0]));
            c.set(Calendar.MINUTE, Integer.parseInt(timeSplit[1]));
            c.set(Calendar.SECOND, 0);
            scheduledTime = c.getTimeInMillis();
        }

        String userId = mUserIdTxt.getText().toString().trim();
        if (userId.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    R.string.user_id_empty, Toast.LENGTH_LONG).show();
            return;
        }
        int selectedSensors = 0;
        for (SensorFrequencyViewAdapter.SelectedSensorFrequency mSensor : mSelectedSensors) {
            if (mSensor.isSelected()) {
                selectedSensors++;
                if (mSensor.getFrequency() == 0) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.frequency_not_set, mSensor.getSensor()), Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        if (selectedSensors == 0) {
            Toast.makeText(getApplicationContext(),
                    R.string.any_sensor_selected, Toast.LENGTH_LONG).show();
            return;
        }

        int spinnerPosition = mStopTimeSpinner.getSelectedItemPosition();
        String deviceLocation = mDeviceLocation.getSelectedItem().toString();
        int stopTime = stopTimeOptions[spinnerPosition];
        LabelConfig newConfig = new LabelConfig(label, stopTime, deviceLocation, userId, mSendFilesToServer.isChecked(), activity, scheduledTime);

        if (mIsUpdating) {
            newConfig.id = mCurrentConfig.id;
            mLabelConfigViewModel.updateConfig(newConfig);
            finishSaveProcess(newConfig, mCurrentConfig.id);
        } else {
            try {
                newConfig.id = mLabelConfigViewModel.insertNewConfig(newConfig);
                finishSaveProcess(newConfig, newConfig.id);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(getApplicationContext(), "Error when saving config.", Toast.LENGTH_SHORT).show();
                log.d("Error when saving config: " + e.getMessage());
            }
        }
    }

    private Observer<LabelConfig> obs = null;
    private boolean isFinishing = false;
    private void finishSaveProcess(LabelConfig newConfig, long id) {
        if (isFinishing) return;
        isFinishing = true;

        mLabelConfigViewModel.insertAllSensorFrequencies(getSensorFrequenciesFromSelectedSensorFrequencies(id));

        if (isConfigLoaded) {
            closeActivity();
        } else {
            SaveConfigDialogFragment saveDialogFragment = new SaveConfigDialogFragment(
                    new SaveConfigListener() {
                        @Override
                        public void onConfirmClick() {
                            try {
                                Gson gson = new Gson();
                                String json = gson.toJson(newConfig);
                                String jsonSensors = gson.toJson(mSelectedSensors);
                                json = "{\"main\":"+json+",\"sensors\":"+jsonSensors +"}";
                                File directory = new File(
                                        getApplicationContext().getFilesDir().getAbsolutePath() +
                                                File.separator +
                                                FOLDER_NAME +
                                                File.separator);
                                if (!directory.exists()) {
                                    directory.mkdirs();
                                }

                                File configJson = new File(
                                        getApplicationContext().getFilesDir().getAbsolutePath() +
                                                File.separator +
                                                FOLDER_NAME +
                                                File.separator +
                                                newConfig.userId+"_"+newConfig.experiment+"_"+newConfig.activity+".json");
                                PrintWriter writer = new PrintWriter(configJson.getAbsolutePath(), "UTF-8");
                                writer.println(json);
                                writer.close();

                                sendConfigurationToServer(configJson, newConfig);
                            } catch (Exception e) {
                                log.d("Error to send config to server: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onNegativeConfirm() {
                            closeActivity();
                        }
                    }
            );
            saveDialogFragment.show(getSupportFragmentManager().beginTransaction(),
                    SaveConfigDialogFragment.class.toString());
        }
    }

    private void sendConfigurationToServer(File config, LabelConfig cfg) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                log.d("sendConfigurationToServer ");
                MultipartBody.Part experimentPart =
                        MultipartBody.Part.createFormData("experiment", cfg.experiment);
                MultipartBody.Part subjectPart =
                        MultipartBody.Part.createFormData("subject", cfg.userId);
                MultipartBody.Part activityPart =
                        MultipartBody.Part.createFormData("activity", cfg.activity);

                MultipartBody.Part filePart = filePart = MultipartBody.Part.createFormData(
                        "file", config.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), config));

                Call<StatusResponse> call = ClientAPI.get(ClientAPI.httpHighTimeout()).uploadConfigFile(filePart, experimentPart, subjectPart, activityPart);
                call.enqueue(uploadCallback(config));
            }
        }).start();
    }

    private void getAllExperiments() {
        log.d("getAllExperiments from server");
        if (mLoadConfigBtn == null) {
            mLoadConfigBtn = findViewById(R.id.load_config_button);
        }
        mLoadConfigBtn.setEnabled(false);

        Call<JsonObject> call = ClientAPI.get(ClientAPI.httpHighTimeout()).getAllExperiments();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject res = new Gson().fromJson(response.body(), JsonObject.class);
                List<String> experiments = new ArrayList<String>();

                for (JsonElement el : res.get("experiment").getAsJsonArray()) {
                    JsonObject exp = el.getAsJsonObject();
                    if (exp.get("configAvailable") == null || exp.get("configAvailable").getAsBoolean()) {
                        experiments.add(exp.get("experiment").getAsString()+"_"+exp.get("activity").getAsString()+"_"+exp.get("user").getAsString());
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLoadConfigBtn.setEnabled(true);
                        AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                        builder.setTitle(R.string.choose_experiment);
                        builder.setItems(experiments.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] exp = experiments.get(which).split("_");
                                Call<JsonObject> call = ClientAPI.get(ClientAPI.httpHighTimeout()).getExperimentConfig(exp[0], exp[2], exp[1]);
                                try {
                                    loadServerConfig(call);
                                } catch (Exception e) {
                                    log.d("Failed to loadServerConfig: " + e.getMessage());
                                }
                            }
                        });
                        builder.show();
                    }
                });
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                log.d("Get Experiments list from server failed: " + t.getCause());
                call.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage(t.getMessage());
                        builder.show();
                        mLoadConfigBtn.setEnabled(true);
                    }
                });
            }
        });
    }

    private void loadServerConfig(Call<JsonObject> call) throws Exception {
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mLoadConfigBtn == null) {
                            mLoadConfigBtn = findViewById(R.id.load_config_button);
                        }
                        mLoadConfigBtn.setEnabled(true);
                        JsonObject config = new Gson().fromJson(response.body(), JsonObject.class);
                        if (config.get("main") == null || config.get("sensors") == null ) {
                            Exception t = new Exception("No configuration found for this experiment!");
                            onFailure(call, t);
                            return;
                        }
                        isConfigLoaded = true;

                        mCurrentConfig = new Gson().fromJson(config.get("main").getAsJsonObject().toString(), LabelConfig.class);
                        updateFields();
                        mLabelConfigViewModel.deleteSensorsFromLabel(mCurrentConfig);
                        JsonArray sensors = new Gson().fromJson(config.get("sensors").getAsJsonArray().toString(), JsonArray.class);
                        boolean checkGPSPermission = false;
                        for (int i = 0; i < sensors.size(); i++) {
                            JsonObject sensor = sensors.get(i).getAsJsonObject();
                            if (sensor.get("isSelected").getAsBoolean() && mSensorFrequencyViewAdapter.checkSensorAvailability(sensor.get("sensor").getAsString())) {
                                for (SensorFrequencyViewAdapter.SelectedSensorFrequency mSensor : mSelectedSensors) {
                                    if (mSensor.getSensor().equalsIgnoreCase(sensor.get("sensor").getAsString())) {
                                        mSensor.setFrequency(sensor.get("frequency").getAsInt());
                                        mSensor.setSelected(sensor.get("isSelected").getAsBoolean());
                                        if (mSensor.getSensor().equalsIgnoreCase("gps")) {
                                            checkGPSPermission = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        //log.d(sensors.toString());
                        mSensorFrequencyViewAdapter.setSelectedSensors(mSelectedSensors);
                        mLabelConfigViewModel.insertAllSensorFrequencies(getSensorFrequenciesFromSelectedSensorFrequencies(mCurrentConfig.id));
                        if (checkGPSPermission) {
                            mSensorFrequencyViewAdapter.checkGPSPermission();
                        }
                    }
                });
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                log.d("Get Configs from server failed: " + t.getCause());
                call.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage(t.getMessage());
                        builder.show();
                        if (mLoadConfigBtn == null) {
                            mLoadConfigBtn = findViewById(R.id.load_config_button);
                        }
                        mLoadConfigBtn.setEnabled(true);
                    }
                });
            }
        });
    }

    private Callback<StatusResponse> uploadCallback(final File file) {
        return new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                try {
                    log.d("Config sent to the server: " + response.code());
                    Toast.makeText(appContext, "Config sent to server", Toast.LENGTH_SHORT).show();
                    file.delete();
                    closeActivity();
                } catch (Exception e) {
                    log.d("Failed to delete config file: " + e.getMessage());
                    closeActivity();
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                log.d("Failed to send config to the server: " + t.getMessage());
                Toast.makeText(appContext, "Failed to send config to the server", Toast.LENGTH_SHORT).show();
                call.cancel();
                try {
                    file.delete();
                    closeActivity();
                } catch (Exception e) {
                    log.d("Failed to delete config file: " + e.getMessage());
                    closeActivity();
                }
            }
        };
    }

    private void closeActivity() {
        //super.onBackPressed();
        finish();
    }

    public static class DeleteDialogFragment extends DialogFragment {
        private final DeleteDialogListener mListener;

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

    public static class SaveConfigDialogFragment extends DialogFragment {
        private final SaveConfigListener mListener;

        public SaveConfigDialogFragment(SaveConfigListener listener) {
            this.mListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.save_config_on_server_confirmation)
                    .setPositiveButton(R.string.yes, (dialog, which) -> mListener.onConfirmClick())
                    .setNegativeButton(R.string.no, (dialog, which) -> mListener.onNegativeConfirm());

            return builder.create();
        }
    }

    public interface DeleteDialogListener {
        void onConfirmClick();
    }

    public interface SaveConfigListener {
        void onConfirmClick();
        void onNegativeConfirm();
    }
}