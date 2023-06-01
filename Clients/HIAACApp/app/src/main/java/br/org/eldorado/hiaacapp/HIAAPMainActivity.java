package br.org.eldorado.hiaacapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.navigation.ui.AppBarConfiguration;

import br.org.eldorado.hiaac.HIAACLibrary;
import br.org.eldorado.hiaacapp.databinding.HiaacActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;

public class HIAAPMainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private HiaacActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = HiaacActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        binding.btnDataCollector.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HIAACLibrary.openDataCollector(getApplicationContext());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}