package br.org.eldorado.hiaac;

import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

import br.org.eldorado.hiaac.data.LabelConfig;
import br.org.eldorado.hiaac.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.util.Tools;

public class LabelOptionsActivity extends AppCompatActivity {
    public static final int MINUTE = 60;
    public static final int HOUR = 3600;

    private final int[] stopTimeOptions = {
            5,
            30,
            1 * MINUTE,
            5 * MINUTE,
            10 * MINUTE
    };
    private LabelConfigViewModel mLabelConfigViewModel;
    private LabelConfig mCurrentConfig;
    private boolean mIsUpdating;

    private EditText mLabelTile;
    private Button mSaveButton;
    private Spinner mStopTimeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_label_options);
        mLabelTile = findViewById(R.id.edit_label_name);
        mSaveButton = findViewById(R.id.save_label_button);
        mStopTimeSpinner = findViewById(R.id.stops_at_spinner);
        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance(getApplication()).create(LabelConfigViewModel.class);

        ArrayList<String> list = Tools.createTimeFormatedList(stopTimeOptions);
        ArrayAdapter<String> adapter = new ArrayAdapter(this,
                R.layout.support_simple_spinner_dropdown_item, list);
        mStopTimeSpinner.setAdapter(adapter);

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
                DeleteDialogFragment deleteDialogFragment = new DeleteDialogFragment(
                        new DeleteDialogListener() {
                            @Override
                            public void onConfirmClick() {
                                Toast.makeText(getApplicationContext(),
                                        R.string.configuration_deleted, Toast.LENGTH_LONG).show();
                                deleteCurrentConfig();
                            }
                        }
                );
                deleteDialogFragment.show(getSupportFragmentManager().beginTransaction(),
                        DeleteDialogFragment.class.toString());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteCurrentConfig() {
        if (mCurrentConfig != null) {
            mLabelConfigViewModel.deleteConfig(mCurrentConfig);
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        }
    }

    private void updateFields() {
        if (mCurrentConfig != null) {
            mLabelTile.setText(mCurrentConfig.label);
            int position = 0;
            for (int i = 0; i < stopTimeOptions.length; i++) {
                if (mCurrentConfig.stopTime == stopTimeOptions[i]) {
                    position = i;
                    break;
                }
            }
            mStopTimeSpinner.setSelection(position);
        }
    }

    private void onSaveButtonClick() {
        String label = mLabelTile.getText().toString();
        int spinnerPosition = mStopTimeSpinner.getSelectedItemPosition();
        int stopTime = stopTimeOptions[spinnerPosition];
        LabelConfig newConfig = new LabelConfig(label, stopTime);
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

    public static class DeleteDialogFragment extends DialogFragment {
        private DeleteDialogListener mListener;

        public DeleteDialogFragment(DeleteDialogListener listener) {
            this.mListener = listener;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.delete_config_confirmation)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mListener.onConfirmClick();
                        }
                    })
                    .setNegativeButton(R.string.no, null);

            return builder.create();
        }
    }

    public interface DeleteDialogListener {
        void onConfirmClick();
    }
}