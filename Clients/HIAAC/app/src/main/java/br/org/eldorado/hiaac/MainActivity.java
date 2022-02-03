package br.org.eldorado.hiaac;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.org.eldorado.hiaac.data.LabelConfig;
import br.org.eldorado.hiaac.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.data.SensorFrequency;
import br.org.eldorado.hiaac.view.adapter.LabelRecyclerViewAdapter;

public class MainActivity extends AppCompatActivity {
    public static final int NEW_LABEL_CONFIG_ACTIVITY = 1;
    public static final int UPDATE_LABEL_CONFIG_ACTIVITY = 2;
    public static final String LABEL_CONFIG_ACTIVITY_TYPE = "label_config_type";
    public static final String LABEL_CONFIG_ACTIVITY_ID = "label_config_id";

    private FloatingActionButton mAddButton;
    private LabelConfigViewModel mLabelConfigViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.label_recycle_view);
        LabelRecyclerViewAdapter adapter = new LabelRecyclerViewAdapter(this);
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
                Map<String, List<SensorFrequency>> sensorFrequencyMap = sensorFrequencies.stream()
                        .collect(Collectors.groupingBy(SensorFrequency::getLabel_id));
                adapter.setSensorFrequencyMap(sensorFrequencyMap);
            }
        });

        mAddButton = findViewById(R.id.add_new_label);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), LabelOptionsActivity.class);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, NEW_LABEL_CONFIG_ACTIVITY);
                startActivity(intent);
            }
        });

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_plot) {
            Intent intent = new Intent(getApplicationContext(), PlotsNavigationActivity.class);
            startActivity(intent);

            return true;
        }

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}