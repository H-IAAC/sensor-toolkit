package br.org.eldorado.hiaac.datacollector;

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
                                                               "statisticsFrag")
                                                               .commit();
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
        super.onBackPressed();
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

            // Calculate the time frame with "start time - end time"
            long totalTimeFrame = statistic.getEndTime() - statistic.getStartTime();
            String timeFrame = String.format("%02d min, %02d sec",
                    TimeUnit.MILLISECONDS.toMinutes(totalTimeFrame),
                    TimeUnit.MILLISECONDS.toSeconds(totalTimeFrame) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(totalTimeFrame))
            );

            View statisticsElementView = inflater.inflate(R.layout.statistics_element, container,false);

            TextView usingServerTime = (TextView) statisticsElementView.findViewById(R.id.usingServerTime);
            usingServerTime.setText(statistic.isUsingServerTime() ? "Server Time" : "Local Time");

            TextView textView = (TextView) statisticsElementView.findViewById(R.id.sensor_name);
            textView.setText(statistic.getSensorName());

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_frequency);
            textView.setText(" (" + statistic.getSensorFrequency() + " Hz)");

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_expected_data);
            long total_expected = statistic.getSensorFrequency() * TimeUnit.MILLISECONDS.toSeconds(totalTimeFrame);
            textView.setText("" + total_expected);

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_collected_data);
            float collected_percentage = (float)(statistic.getCollectedData()) * 100 / (float)total_expected;
            textView.setText(statistic.getCollectedData() + " (" + String.format("%.2f", collected_percentage) + "%)");

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_valid_data);
            long validData = statistic.getCollectedData() - statistic.getInvalidData();
            float valid_percentage = (float)(validData) * 100 / statistic.getCollectedData();
            textView.setText(validData + " (" + String.format("%.2f", valid_percentage) + "%)");

            textView = (TextView) statisticsElementView.findViewById(R.id.sensor_invalid_data);
            float invalid_percentage = (float)statistic.getInvalidData() * 100 / statistic.getCollectedData();
            textView.setText(statistic.getInvalidData() + " (" + String.format("%.2f", invalid_percentage) + "%)");

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


