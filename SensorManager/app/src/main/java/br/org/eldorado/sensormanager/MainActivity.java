package br.org.eldorado.sensormanager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;

import br.org.eldorado.sensormanager.view.adapter.SensorRecyclerViewAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.sensor_recycle_view);
        int numberOfColumns = 2;
        recyclerView.setLayoutManager(new GridLayoutManager(this, numberOfColumns));
        SensorRecyclerViewAdapter adapter = new SensorRecyclerViewAdapter(this);
        recyclerView.setAdapter(adapter);
    }
}