package br.org.eldorado.hiaac.datacollector;


import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.util.Preferences;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_settings);
        }

        setContentView(R.layout.activity_settings);

        // Insert the settings fragment in the FrameLayout we added earlier
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings_content, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        private ListPreference serverAddressPreference;
        private String actualSelectedServerAddress;
        private String actualSelectedServerAddressValue;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings, rootKey);
            serverAddressPreference = findPreference(getString(R.string.settings_server_config));
            serverAddressPreference.setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
            actualSelectedServerAddress = preference.getSummary().toString();
            actualSelectedServerAddressValue = ((ListPreference)preference).getValue();
            if ("custom:8080".equals(newValue)) {
                showServerAddressDialog(getContext());
            }
            return true;
        }

        private void showServerAddressDialog(Context c) {
            final EditText serverAddress = new EditText(c);
            AlertDialog dialog = new AlertDialog.Builder(c)
                    .setTitle(getString(R.string.settings_custom_server_dialog_title))
                    .setMessage(getString(R.string.settings_custom_server_dialog_message))
                    .setView(serverAddress)
                    .setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String addr = String.valueOf(serverAddress.getText());
                            try {
                                if (addr.split(":").length == 2) {
                                    Preferences.setCustomServerAddress(addr);
                                    return;
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            serverAddressPreference.setSummary(actualSelectedServerAddress);
                            serverAddressPreference.setValue(actualSelectedServerAddressValue);
                        }
                    }).setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            serverAddressPreference.setSummary(actualSelectedServerAddress);
                            serverAddressPreference.setValue(actualSelectedServerAddressValue);
                        }
                    })
                    .create();
            dialog.show();
        }
    }
}