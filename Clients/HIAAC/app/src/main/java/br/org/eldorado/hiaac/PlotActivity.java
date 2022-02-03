package br.org.eldorado.hiaac;

import static br.org.eldorado.hiaac.FileSelectorActivity.PLOT_TYPE;
import static br.org.eldorado.hiaac.FileSelectorActivity.SELECTED_FILES;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.List;

public class PlotActivity extends AppCompatActivity {
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plot);

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        Python py = Python.getInstance();
        PyObject module = py.getModule("plot");

        Bundle extras = getIntent().getExtras();
        String plotType = extras.getString(PLOT_TYPE);
        String[] args = extras.getStringArray(SELECTED_FILES);

        imageView = findViewById(R.id.plot_image);
        final ProgressDialog dialog = ProgressDialog.show(this, "Plot", "Ploting image...", true);
        new PyExecuteAsyncTask(dialog, imageView, plotType).execute(args);
    }

    private class PyExecuteAsyncTask extends AsyncTask<String, Void, Bitmap> {
        private ProgressDialog dialog;
        private ImageView imageView;
        private String plotType;

        public PyExecuteAsyncTask(ProgressDialog dialog, ImageView imageView, String plotType) {
            this.dialog = dialog;
            this.imageView = imageView;
            this.plotType = plotType;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            this.dialog.cancel();
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);

                View focus = getCurrentFocus();
                if (focus != null) {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            }
        }

        @Override
        protected Bitmap doInBackground(String... args) {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("plot");

                byte[] bytes = module.callAttr(plotType, args).toJava(byte[].class);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                return bitmap;
            } catch (PyException e) {
                Log.e("PlotActivity", e.toString());
            }
            return null;
        }
    }
}