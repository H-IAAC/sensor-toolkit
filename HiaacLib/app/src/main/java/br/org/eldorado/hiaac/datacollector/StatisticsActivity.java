package br.org.eldorado.hiaac.datacollector;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.data.ExperimentStatistics;
import br.org.eldorado.hiaac.datacollector.util.Log;

public class StatisticsActivity extends AppCompatActivity {

    private static final String TAG = "StatisticsFragment";
    private Log log;
    private List<ExperimentStatistics> statistics;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        log = new Log(TAG);
        setContentView(R.layout.statistics_content_fragment);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        String statisticsJson = getIntent().getExtras().getString("statistics");
        statistics = new Gson().fromJson(statisticsJson, new TypeToken<ArrayList<ExperimentStatistics>>() {}.getType());
    }

    @Override
    public boolean onSupportNavigateUp() {
        closeActivity();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    private void closeActivity() {
        this.finish();
        Intent intent = new Intent(getApplicationContext(), DataCollectorActivity.class);
        startActivity(intent);
    }
}
