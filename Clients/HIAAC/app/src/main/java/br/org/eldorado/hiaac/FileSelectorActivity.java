package br.org.eldorado.hiaac;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import br.org.eldorado.hiaac.view.adapter.FileSelectorViewAdapter;

public class FileSelectorActivity extends AppCompatActivity {
    public final static String PLOT_TYPE = "plot_type";
    public final static String SELECTED_FILES = "selected_files";

    private Button createButton;
    private List<File> selectedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_selector);
        selectedFiles = new ArrayList<>();
        createButton = findViewById(R.id.create_plot_button);

        Bundle extras = getIntent().getExtras();
        String plotType = extras.getString(PLOT_TYPE);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.file_selector_recycle_view);
        FileSelectorViewAdapter adapter = new FileSelectorViewAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        String path = this.getFilesDir().getAbsolutePath() + File.separator;
        File directory = new File(path);
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(".csv");
            }
        });
        adapter.setFiles(files);

        adapter.setListener(new FileSelectorViewAdapter.FileSelectedListener() {
            @Override
            public void onFileChecked(File file, boolean ischecked) {
                if (ischecked) {
                    selectedFiles.add(file);
                } else {
                    selectedFiles.remove(file);
                }
            }
        });

        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] args = selectedFiles.stream()
                        .map(file -> file.getPath())
                        .collect(Collectors.toList())
                        .toArray(new String[0]);

                Intent intent = new Intent(getApplicationContext(), PlotActivity.class);
                intent.putExtra(PLOT_TYPE, plotType);
                intent.putExtra(SELECTED_FILES, args);
                startActivity(intent);
            }
        });
    }
}