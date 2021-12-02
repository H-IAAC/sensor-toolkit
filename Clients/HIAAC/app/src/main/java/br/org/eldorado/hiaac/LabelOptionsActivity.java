package br.org.eldorado.hiaac;

import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import br.org.eldorado.hiaac.data.LabelConfig;
import br.org.eldorado.hiaac.data.LabelConfigViewModel;

public class LabelOptionsActivity extends AppCompatActivity {
    private LabelConfigViewModel mLabelConfigViewModel;
    private LabelConfig mCurrentConfig;
    private boolean mIsUpdating;

    private EditText mLabelTile;
    private Button mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_options);
        mLabelTile = findViewById(R.id.edit_label_name);
        mSaveButton = findViewById(R.id.save_label_button);
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);

        Bundle extras = getIntent().getExtras();
        int activityType = extras.getInt(LABEL_CONFIG_ACTIVITY_TYPE);
        switch (activityType) {
            case UPDATE_LABEL_CONFIG_ACTIVITY:
                mIsUpdating = true;
                String lableId = extras.getString(LABEL_CONFIG_ACTIVITY_ID);
                mLabelConfigViewModel.getLabelConfigById(lableId)
                        .observe(this, new Observer<LabelConfig>() {
                    @Override
                    public void onChanged(LabelConfig labelConfig) {
                        mCurrentConfig = labelConfig;
                        updateFields();
                    }
                });
                break;
            default:
                mIsUpdating = false;
        }

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSaveButtonClick();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsUpdating) {
            getMenuInflater().inflate(R.menu.menu_label_options, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete_label_button:
                if (mCurrentConfig != null) {
                    mLabelConfigViewModel.deleteConfig(mCurrentConfig);
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateFields() {
        if (mCurrentConfig != null) {
            mLabelTile.setText(mCurrentConfig.label);
        }
    }

    private void onSaveButtonClick() {
        String label = mLabelTile.getText().toString();
        LabelConfig newConfig = new LabelConfig(label);
        if (mIsUpdating && mCurrentConfig != null) {
            if (label == mCurrentConfig.label) {
                mLabelConfigViewModel.updateConfig(newConfig);
            } else {
                mLabelConfigViewModel.deleteConfig(mCurrentConfig);
                mLabelConfigViewModel.insertNewConfig(newConfig);
            }
        } else {
            mLabelConfigViewModel.insertNewConfig(newConfig);
        }
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}