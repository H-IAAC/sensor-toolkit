package br.org.eldorado.hiaac.datacollector;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

        TextView textView = (TextView) findViewById(R.id.statistics_date);
        textView.setText(convertDate(statistics.get(0).getStartTime()));

        textView = (TextView) findViewById(R.id.sensor_period);
        textView.setText(convertHours(statistics.get(0).getStartTime()) + " ~ " + convertHours(statistics.get(0).getEndTime()));

        for ( ExperimentStatistics statistic : statistics) {
            getSupportFragmentManager().beginTransaction().add(R.id.statistics_elements,
                                                                new StatisticsPlaceholderFragment(statistic),
                                                            "statisticsFrag").commit();
        }
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

    private String convertDate(long time) {
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL,
                                                           getResources().getConfiguration().getLocales().get(0));
        return dateFormat.format(new Date(time));
    }

    private String convertHours(long seconds) {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        return formatter.format(new Date(seconds));
    }

    public static class StatisticsPlaceholderFragment extends Fragment {
        private ExperimentStatistics statistic;

        public StatisticsPlaceholderFragment(ExperimentStatistics statistic) {
            this.statistic = statistic;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            View statisticsElementView = inflater.inflate(R.layout.statistics_element, container,false);

            TextView textView = (TextView) statisticsElementView.findViewById(R.id.sensor_name);
            textView.setText(statistic.getSensorName());

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_frequency);
            textView.setText(" (" + statistic.getSensorFrequency() + " Hz)");

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_valid_data);
            textView.setText(statistic.getCollectedData() + "");

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_invalid_data);
            textView.setText(statistic.getInvalidData() + "");

            // Calculate the time frame with "start time - end time"
            long totalTimeFrame = statistic.getEndTime() - statistic.getStartTime();
            String timeFrame = String.format("%02d min, %02d sec",
                    TimeUnit.MILLISECONDS.toMinutes(totalTimeFrame),
                    TimeUnit.MILLISECONDS.toSeconds(totalTimeFrame) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeFrame))
            );
            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_timeframe);
            textView.setText(timeFrame);

            // If there is no valid samples, then ignore the remaining fields
            if (statistic.getCollectedData() == 0) return statisticsElementView;

            // Set the detailed time frame values
            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_timestamp_average);
            textView.setText(statistic.getTimestampAverage() + " ms");

            textView = (TextView) statisticsElementView.findViewById(R.id.max_timestamp_difference);
            textView.setText(statistic.getMaxTimestampDifference() + " ms");

            textView = (TextView) statisticsElementView.findViewById(R.id.min_timestamp_difference);
            textView.setText(statistic.getMinTimestampDifference() + " ms");

            return statisticsElementView;
        }
    }
}


