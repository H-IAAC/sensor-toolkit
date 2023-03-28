package br.org.eldorado.hiaac.datacollector;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.NEW_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.UPDATE_LABEL_CONFIG_ACTIVITY;
import static br.org.eldorado.hiaac.datacollector.view.adapter.SensorFrequencyViewAdapter.frequencyOptions;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ApiInterface;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
import br.org.eldorado.hiaac.datacollector.api.StatusResponse;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.hiaac.datacollector.view.adapter.LabelRecyclerViewAdapter;
import br.org.eldorado.hiaac.datacollector.view.adapter.SensorFrequencyViewAdapter;
import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.AmbientTemperature;
import br.org.eldorado.sensoragent.model.GPS;
import br.org.eldorado.sensoragent.model.Gravity;
import br.org.eldorado.sensoragent.model.Gyroscope;
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
            5 * MINUTE,
            10 * MINUTE,
            30 * MINUTE,
            1 * HOUR,
            12 * HOUR,
            24 * HOUR,
            3 * DAY,
            7 * DAY,
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
    private static final String TAG = "LabelOptionsActivity";
    private Log log;
    private boolean isConfigLoaded;

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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        log = new Log(TAG);
        setContentView(R.layout.activity_label_options);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mLabelTile = findViewById(R.id.edit_label_name);
        mActivityTxt = findViewById(R.id.activity_txt);
        isConfigLoaded = false;

        mScheduleTimeTxt = findViewById(R.id.txtScheduleTime);
        mScheduleTimeTxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar now = Calendar.getInstance();
                now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());

                timePickerDialog = new TimePickerDialog(LabelOptionsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mScheduleTimeTxt.setText(hourOfDay + ":" + minute);
                    }

                } , now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true);

                timePickerDialog.show();
                timePickerDialog.getButton(TimePickerDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timePickerDialog.cancel();
                        mScheduleTimeTxt.setText("");
                    }
                });
            }
        });

        mStopTimeSpinner = findViewById(R.id.stops_at_spinner);
        mDeviceLocation = findViewById(R.id.device_location_spinner);
        mUserIdTxt = findViewById(R.id.user_id_txt);
        mSendFilesToServer = findViewById(R.id.send_files_to_server_checkbox);
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);

        ArrayList<String> list = Tools.createTimeFormatedList(stopTimeOptions);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter(this,
                R.layout.custom_spinner, list);
        mStopTimeSpinner.setAdapter(arrayAdapter);

        List<String> deviceLocationList = new ArrayList<>();
        for (int i = 0; i < mDeviceLocation.getCount(); i++) {
            deviceLocationList.add(mDeviceLocation.getItemAtPosition(i).toString());
        }
        mDeviceLocation.setAdapter(new ArrayAdapter(this,
                R.layout.custom_spinner, deviceLocationList));


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
            mLabelConfigViewModel.deleteSensorsFromLabel(mCurrentConfig);
            if (mSensorFrequencies != null) {
                mLabelConfigViewModel.deleteAllSensorFrequencies(mSensorFrequencies);
            }
            closeActivity();
        }
    }

    private void updateFields() {
        if (mCurrentConfig != null) {
            mLabelTile.setText(mCurrentConfig.label);
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
            position = 0;
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
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_GRAVITY, Gravity.TAG));
        selectedSensorFrequencies.add(createSelectedSensorFrequency(sensorTypeFrequencyMap,
                SensorBase.TYPE_GPS, GPS.TAG));

        return selectedSensorFrequencies;
    }

    private SensorFrequencyViewAdapter.SelectedSensorFrequency createSelectedSensorFrequency(
            Map<Integer, Integer> sensorTypeFrequencyMap, int sensorType, String sensorName) {
        Integer frequency = sensorTypeFrequencyMap.get(sensorType);

        SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency =
                new SensorFrequencyViewAdapter.SelectedSensorFrequency(
                        frequency != null,
                        sensorName,
                        frequency == null ? frequencyOptions.get(0) : frequency.intValue()
                );

        return selectedSensorFrequency;
    }

    private List<SensorFrequency> getSensorFrequenciesFromSelectedSensorFrequencies(String label) {
        List<SensorFrequency> sensorFrequencies = new ArrayList<>();
        for (SensorFrequencyViewAdapter.SelectedSensorFrequency selectedSensorFrequency : mSelectedSensors) {
            if (selectedSensorFrequency.isSelected()) {
                SensorFrequency sensorFrequency = new SensorFrequency(
                        label,
                        Tools.getSensorFromTitleName(selectedSensorFrequency.getSensor()),
                        selectedSensorFrequency.getFrequency());
                sensorFrequencies.add(sensorFrequency);
            }
        }

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

        int spinnerPosition = mStopTimeSpinner.getSelectedItemPosition();
        String deviceLocation = mDeviceLocation.getSelectedItem().toString();
        int stopTime = stopTimeOptions[spinnerPosition];
        LabelConfig newConfig = new LabelConfig(label, stopTime, deviceLocation, userId, mSendFilesToServer.isChecked(), activity, scheduledTime);
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
                                                newConfig.userId+"_"+newConfig.label+"_"+newConfig.activity+"_"+newConfig.deviceLocation+".json");
                                PrintWriter writer = new PrintWriter(configJson.getAbsolutePath(), "UTF-8");
                                writer.println(json);
                                //writer.println(jsonSensors);
                                writer.close();

                                sendConfigurationToServer(configJson, newConfig);
                            } catch (Exception e) {
                                e.printStackTrace();
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
                        MultipartBody.Part.createFormData("experiment", cfg.label);
                MultipartBody.Part subjectPart =
                        MultipartBody.Part.createFormData("subject", cfg.userId);
                MultipartBody.Part activityPart =
                        MultipartBody.Part.createFormData("activity", cfg.activity);

                MultipartBody.Part filePart = filePart = MultipartBody.Part.createFormData(
                        "file", config.getName(),
                        RequestBody.create(MediaType.parse("multipart/form-data"), config));

                ClientAPI apiClient = new ClientAPI();
                ApiInterface apiInterface = apiClient.getClient(Tools.SERVER_HOST, Tools.SERVER_PORT).create(ApiInterface.class);
                Call<StatusResponse> call = apiInterface.uploadConfigFile(filePart, experimentPart, subjectPart, activityPart);
                call.enqueue(uploadCallback(config));
            }
        }).start();
    }

    private void getAllExperiments() {
        ClientAPI api = new ClientAPI();
        ApiInterface apiInterface = api.getClient(Tools.SERVER_HOST, Tools.SERVER_PORT).create(ApiInterface.class);
        Call<JsonObject> call = apiInterface.getAllExperiments();
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                JsonObject res = new Gson().fromJson(response.body(), JsonObject.class);
                List<String> experiments = new ArrayList<String>();
                for (JsonElement el : res.get("experiment").getAsJsonArray()) {
                    JsonObject exp = el.getAsJsonObject();
                    experiments.add(exp.get("experiment").getAsString()+"_"+exp.get("activity").getAsString()+"_"+exp.get("user").getAsString());
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                        builder.setTitle(R.string.choose_experiment);
                        builder.setItems(experiments.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String[] exp = experiments.get(which).split("_");
                                Call<JsonObject> call = apiInterface.getExperimentConfig(exp[0], exp[2], exp[1]);
                                loadServerConfig(call);
                            }
                        });
                        builder.show();
                    }
                });
            }
            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.printStackTrace();
                call.cancel();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                        builder.setTitle("Error");
                        builder.setMessage(t.getMessage());
                        builder.show();
                    }
                });
            }
        });
    }

    private void loadServerConfig(Call<JsonObject> call) {
        try {
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
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
                            for (int i = 0; i < sensors.size(); i++) {
                                JsonObject sensor = sensors.get(i).getAsJsonObject();
                                mSelectedSensors.get(i).setFrequency(sensor.get("frequency").getAsInt());
                                mSelectedSensors.get(i).setSelected(sensor.get("isSelected").getAsBoolean());
                            }
                            mSensorFrequencyViewAdapter.setSelectedSensors(mSelectedSensors);
                            mLabelConfigViewModel.insertAllSensorFrequencies(getSensorFrequenciesFromSelectedSensorFrequencies(mCurrentConfig.label));

                        }
                    });
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    t.printStackTrace();
                    call.cancel();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(LabelOptionsActivity.this);
                            builder.setTitle("Error");
                            builder.setMessage(t.getMessage());
                            builder.show();
                        }
                    });
                }
            });
        } catch (Exception e) {

        }
    }

    private Callback<StatusResponse> uploadCallback(final File file) {
        return new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                try {
                    log.d(response.code()+"");
                    file.delete();
                    closeActivity();
                } catch (Exception e) {
                    e.printStackTrace();
                    closeActivity();
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                t.printStackTrace();
                log.d("FAIL " + t);
                call.cancel();
                try {
                    file.delete();
                    closeActivity();
                } catch (Exception e) {
                    e.printStackTrace();
                    closeActivity();
                }
            }
        };
    }

    private void closeActivity() {
        this.finish();
        Intent intent = new Intent(getApplicationContext(), DataCollectorActivity.class);
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

    public static class SaveConfigDialogFragment extends DialogFragment {
        private SaveConfigListener mListener;

        public SaveConfigDialogFragment(SaveConfigListener listener) {
            this.mListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.save_config_on_server_confirmation)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onConfirmClick();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onNegativeConfirm();
                        }
                    });

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