package br.org.eldorado.hiaacapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import android.os.Bundle;

import br.org.eldorado.hiaacapp.databinding.ActivityLicenseBinding;

public class LicenseActivity extends AppCompatActivity {

    private ActivityLicenseBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Open source licenses");
        }

        binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.licenses.setText(HtmlCompat.fromHtml(
                "<p><i>TODO</i></p>",
                HtmlCompat.FROM_HTML_MODE_LEGACY));

    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}