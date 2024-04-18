package br.org.eldorado.hiaac.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.org.eldorado.hiaac.BuildConfig;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.hiaac.datacollector.util.Permissions;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.view.adapter.LabelRecyclerViewAdapter;
import br.org.eldorado.sensorsdk.SensorSDK;

public class DataCollectorActivity extends AppCompatActivity {
    public static final int NEW_LABEL_CONFIG_ACTIVITY = 1;
    public static final int UPDATE_LABEL_CONFIG_ACTIVITY = 2;
    public static final String LABEL_CONFIG_ACTIVITY_TYPE = "label_config_type";
    public static final String LABEL_CONFIG_ACTIVITY_ID = "label_config_id";
    public static final String FOLDER_NAME = "datacollector";
    //private static boolean isActivityVisible;
    private FloatingActionButton mAddButton;
    private LabelConfigViewModel mLabelConfigViewModel;
    private LabelRecyclerViewAdapter adapter;
    private BroadcastReceiver br;
    private Log log;
    private Permissions permissions;

    public final static String SCHEDULER_ACTIONS = "br.org.eldorado.schedule_collect_data";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Preferences.init(this.getApplicationContext());
        log = new Log("DataCollectorActivity");

        permissions = new Permissions(this, this.getApplicationContext());

        setContentView(R.layout.data_collector_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView footer = findViewById(R.id.version);
        footer.setText(BuildConfig.HIAAC_VERSION);

        if (!AlarmConfig.isInitialized())
            AlarmConfig.init(this.getApplicationContext(), (TextView) findViewById(R.id.scheduler));

        RecyclerView recyclerView = findViewById(R.id.label_recycle_view);
        adapter = new LabelRecyclerViewAdapter(this);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                LabelRecyclerViewAdapter.ViewHolder holder = adapter.getViewHolder(i.getStringExtra("holder"));
                if (holder != null && !holder.isStarted()) {
                    long startsTime = i.getLongExtra("startTime", SensorSDK.getInstance().getRemoteTime()) - SensorSDK.getInstance().getRemoteTime();

                    log.d("Scheduler: Broadcast received");

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            AlarmConfig.cancelAlarm();
                            AlarmConfig.releaseWakeLock();

                            PowerManager powerManager = (PowerManager) getApplicationContext().getSystemService(getApplicationContext().POWER_SERVICE);
                            PowerManager.WakeLock  wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                                                                                       "HIAACApp::WakeLock");
                            wakeLock.acquire();

                            log.d("Scheduler: Broadcast received - startExecution");
                            adapter.startExecution(holder);
                        }
                    }, startsTime);

                } else {
                    log.d("Scheduler: Broadcast received invalid");
                }
            }
        };

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        registerReceiver(br, new IntentFilter(DataCollectorActivity.SCHEDULER_ACTIONS));

        mLabelConfigViewModel = ViewModelProvider
                                     .AndroidViewModelFactory
                                     .getInstance(getApplication())
                                     .create(LabelConfigViewModel.class);

        mLabelConfigViewModel.getAllLabels().observe(this, new Observer<List<LabelConfig>>() {
            @Override
            public void onChanged(List<LabelConfig> labels) {
                adapter.setLabelConfigs(labels);
            }
        });

        mLabelConfigViewModel.getAllSensorFrequencies().observe(this, new Observer<List<SensorFrequency>>() {
            @Override
            public void onChanged(List<SensorFrequency> sensorFrequencies) {
                Map<Long, List<SensorFrequency>> sensorFrequencyMap = sensorFrequencies
                                                                           .stream()
                                                                           .collect(Collectors.groupingBy(SensorFrequency::getConfigId));
                adapter.setSensorFrequencyMap(sensorFrequencyMap);
            }
        });

        mAddButton = findViewById(R.id.add_new_label);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LabelOptionsActivity.class);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, NEW_LABEL_CONFIG_ACTIVITY);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        TimeSync.startServerTimeUpdates(findViewById(R.id.server_time), this);

        permissions.askPermissions();
    }

    @Override
    protected void onResume() {
        adapter.notifyDataSetChanged();
        super.onResume();
    }


    @Override
    protected void onDestroy() {
        if (br != null) {
            unregisterReceiver(br);
        }

        TimeSync.stopServerTimeUpdates();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}