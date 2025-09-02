package br.org.eldorado.hiaac.datacollector;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
import br.org.eldorado.hiaac.datacollector.controller.ExecutionController;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.hiaac.datacollector.util.Permissions;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.util.Utils;
import br.org.eldorado.hiaac.datacollector.view.adapter.LabelRecyclerViewAdapter;

public class DataCollectorActivity extends AppCompatActivity {
    public static final int NEW_LABEL_CONFIG_ACTIVITY = 1;
    public static final int UPDATE_LABEL_CONFIG_ACTIVITY = 2;
    public static final String LABEL_CONFIG_ACTIVITY_TYPE = "label_config_type";
    public static final String LABEL_CONFIG_ACTIVITY_ID = "label_config_id";
    public static final String FOLDER_NAME = "datacollector";
    //private static boolean isActivityVisible;
    private FloatingActionButton mAddButton;
    private LabelConfigViewModel mLabelConfigViewModel;
    private static LabelRecyclerViewAdapter adapter = null;
    private static final Log log = new Log("DataCollectorActivity");
    private Permissions permissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Preferences.init(this.getApplicationContext());

        permissions = new Permissions(this, this.getApplicationContext());

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.data_collector_activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        TextView footer = findViewById(R.id.version);
        footer.setText(BuildConfig.HIAAC_VERSION);

        AlarmConfig.init(this.getApplicationContext(), findViewById(R.id.scheduler));

        RecyclerView recyclerView = findViewById(R.id.label_recycle_view);
        adapter = new LabelRecyclerViewAdapter(this);

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //registerReceiver(br, new IntentFilter(AlarmConfig.SCHEDULER_ACTIONS));

        try {
            mLabelConfigViewModel = ViewModelProvider
                    .AndroidViewModelFactory
                    .getInstance(getApplication())
                    .create(LabelConfigViewModel.class);
        } catch (Exception e){
            Toast.makeText(this, "Need to clean app storage.", Toast.LENGTH_LONG).show();
        }

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
                showPopupMenu(v);
            }
        });

        permissions.askPermissions();
    }

    private void showPopupMenu(View v) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.fab_options_menu, null);

        final PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        ImageButton btnUnicamp = popupView.findViewById(R.id.btn_unicamp);
        ImageButton btnEldorado = popupView.findViewById(R.id.option_eldorado);

        btnUnicamp.setOnClickListener(view -> {
            popupWindow.dismiss();

            Intent intent = new Intent(getApplicationContext(), LabelOptionsActivity.class);
            intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, NEW_LABEL_CONFIG_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        btnEldorado.setOnClickListener(view -> {
            popupWindow.dismiss();

            Intent intent = new Intent(getApplicationContext(), EldoradoLabelOptionsActivity.class);
            intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, NEW_LABEL_CONFIG_ACTIVITY);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        // Popup configurations
        popupWindow.setElevation(10);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        popupWindow.showAsDropDown(mAddButton, -40, -440);

    }

    @Override
    protected void onResume() {
        TimeSync.startServerTimeUpdates(findViewById(R.id.server_time),
                findViewById(R.id.time_diff),
                this);

        adapter.notifyDataSetChanged();

        AlarmConfig.init(this.getApplicationContext(), findViewById(R.id.scheduler));

        super.onResume();
    }


    @Override
    protected void onDestroy() {
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

    public static LabelRecyclerViewAdapter getAdapter() {
        if (adapter == null) {
            log.d("Adapter is null!!!");
            Utils.emitErrorBeep();
        }

        return adapter;
    }
}