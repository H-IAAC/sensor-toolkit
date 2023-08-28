package br.org.eldorado.hiaacapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import br.org.eldorado.hiaac.HIAACLibrary;
import br.org.eldorado.hiaacapp.databinding.HiaacActivityMainBinding;

public class HIAAPMainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private HiaacActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = HiaacActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnDataCollector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HIAACLibrary.openDataCollector(getApplicationContext());
            }
        });

        binding.license.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LicenseActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getApplicationContext().startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}