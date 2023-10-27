package br.org.eldorado.hiaac.datacollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import com.google.gson.JsonObject;

import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.org.eldorado.hiaac.BuildConfig;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ApiInterface;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
import br.org.eldorado.hiaac.datacollector.data.ExperimentStatistics;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.view.adapter.LabelRecyclerViewAdapter;
import br.org.eldorado.sensorsdk.SensorSDK;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DataCollectorActivity extends AppCompatActivity {
    public static final int NEW_LABEL_CONFIG_ACTIVITY = 1;
    public static final int UPDATE_LABEL_CONFIG_ACTIVITY = 2;
    public static final String LABEL_CONFIG_ACTIVITY_TYPE = "label_config_type";
    public static final String LABEL_CONFIG_ACTIVITY_ID = "label_config_id";
    public static final String FOLDER_NAME = "datacollector";

    private static boolean isActivityVisible;
    private FloatingActionButton mAddButton;
    private LabelConfigViewModel mLabelConfigViewModel;
    private TextView serverTimeTxt;
    private DateFormat df;
    private LabelRecyclerViewAdapter adapter;
    private BroadcastReceiver br;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isActivityVisible = true;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setContentView(R.layout.data_collector_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView footer = findViewById(R.id.version);
        footer.setText(BuildConfig.HIAAC_VERSION);

        RecyclerView recyclerView = findViewById(R.id.label_recycle_view);
        adapter = new LabelRecyclerViewAdapter(this);
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                LabelRecyclerViewAdapter.ViewHolder holder = adapter.getViewHolder(i.getStringExtra("holder"));
                if (holder != null && !holder.isStarted()) {
                    adapter.startExecution(holder);
                }
            }
        };
        String action = "br.org.eldorado.schedule_collect_data";
        registerReceiver(br, new IntentFilter(action));

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);
        mLabelConfigViewModel.getAllLabels().observe(this, new Observer<List<LabelConfig>>() {
            @Override
            public void onChanged(List<LabelConfig> labels) {
                adapter.setLabelConfigs(labels);
            }
        });

        mLabelConfigViewModel.getAllSensorFrequencies().observe(this, new Observer<List<SensorFrequency>>() {
            @Override
            public void onChanged(List<SensorFrequency> sensorFrequencies) {
                Map<Long, List<SensorFrequency>> sensorFrequencyMap = sensorFrequencies.stream()
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

        serverTimeTxt = findViewById(R.id.server_time);
        df = new SimpleDateFormat("HH:mm:ss");
        ClientAPI api = new ClientAPI();
        String address = Preferences.getPreferredServer().split(":")[0];
        String port = Preferences.getPreferredServer().split(":")[1];
        ApiInterface apiInterface = api.getClient(address, port).create(ApiInterface.class);
        updateServerTime(apiInterface);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        setRemoteTimeText(SensorSDK.getInstance().getRemoteTime());
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (br != null) {
            unregisterReceiver(br);
        }
    }

    private void updateServerTime(ApiInterface apiInterface) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int resync = 0;
                    Date date = new Date(System.currentTimeMillis());
                    while (true) {
                        /** Resync with server every 2 minutes */
                        if (resync++ % 2 == 0) {
                            Call<JsonObject> call = apiInterface.getServerTime();
                            call.enqueue(new Callback<JsonObject>() {
                                @Override
                                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                                    long timeInMillis = response.body().get("currentTimeMillis").getAsLong();
                                    SensorSDK.getInstance().setRemoteTime(timeInMillis +
                                            (response.raw().receivedResponseAtMillis() - response.raw().sentRequestAtMillis())/2);
                                    //setRemoteTimeText(SensorSDK.getInstance().getRemoteTime());
                                    /*if (isActivityVisible) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                serverTimeTxt.setText(getString(R.string.server_time) + " " + df.format(new Date(timeInMillis)));
                                            }
                                        });
                                    }*/
                                }
                                @Override
                                public void onFailure(Call<JsonObject> call, Throwable t) {
                                    t.printStackTrace();
                                    call.cancel();
                                }
                            });
                            Thread.sleep(2000);
                        } else {
                            long timeInMillis = SensorSDK.getInstance().getRemoteTime();
                            //setRemoteTimeText(timeInMillis);
                            /*date.setTime(timeInMillis);
                            String time = df.format(date);
                            if (isActivityVisible) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        serverTimeTxt.setText(getString(R.string.server_time) + " " + time);
                                    }
                                });
                            }*/
                            long next = (60000 - timeInMillis % 60000);
                            Thread.sleep(Math.max(next, next-50));
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        long time = SensorSDK.getInstance().getRemoteTime();
                        setRemoteTimeText(time);
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void setRemoteTimeText(long timeInMillis) {
        if (isActivityVisible) {
            Date date = new Date(timeInMillis);
            String time = df.format(date);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    serverTimeTxt.setText(getString(R.string.server_time) + " " + time);
                }
            });
        }
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