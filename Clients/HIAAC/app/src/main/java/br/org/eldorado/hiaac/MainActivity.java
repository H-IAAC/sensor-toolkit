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

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import br.org.eldorado.hiaac.data.LabelConfigViewModel;
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
        mLabelConfigViewModel.getAllLabels().observe(this, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                adapter.setLabelConfigs(strings);
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}