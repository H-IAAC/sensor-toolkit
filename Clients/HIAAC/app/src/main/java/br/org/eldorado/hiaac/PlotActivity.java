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
import android.webkit.WebView;
import android.widget.ImageView;

import com.chaquo.python.PyException;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.File;
import java.util.List;

public class PlotActivity extends AppCompatActivity {
    public static final String HTML_FILE = "plot.html";
    private static final String TAG = "PlotActivity";

    private WebView webView;
    private String filePath;

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

        webView = findViewById(R.id.plot_web_view);
        filePath = this.getDataDir().getAbsolutePath() + File.separator + HTML_FILE;
        Log.d(TAG, "Filepath: " + filePath);
        final ProgressDialog dialog = ProgressDialog.show(this, "Plot", "Ploting image...", true);
        new PyExecuteAsyncTask(dialog, webView, plotType).execute(args);
    }

    private class PyExecuteAsyncTask extends AsyncTask<String, Void, Boolean> {
        private ProgressDialog dialog;
        private WebView webView;
        private String plotType;

        public PyExecuteAsyncTask(ProgressDialog dialog, WebView webView, String plotType) {
            this.dialog = dialog;
            this.webView = webView;
            this.plotType = plotType;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            this.dialog.cancel();
            if (success) {
                webView.loadUrl(filePath);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setAllowContentAccess(true);
                webView.getSettings().setAllowFileAccess(true);

                View focus = getCurrentFocus();
                if (focus != null) {
                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                            .hideSoftInputFromWindow(focus.getWindowToken(), 0);
                }
            }
        }

        @Override
        protected Boolean doInBackground(String... args) {
            try {
                Python py = Python.getInstance();
                PyObject module = py.getModule("plot");

                module.callAttr(plotType, args);
                return true;
            } catch (PyException e) {
                Log.e(TAG, e.toString());
            }
            return false;
        }
    }
}