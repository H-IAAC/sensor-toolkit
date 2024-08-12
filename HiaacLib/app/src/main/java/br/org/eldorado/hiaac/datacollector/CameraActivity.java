package br.org.eldorado.hiaac.datacollector;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.util.Permissions;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.hiaac.datacollector.util.VideoMetadata;
import br.org.eldorado.sensorsdk.SensorSDK;
import br.org.eldorado.sensorsdk.util.Log;

public class CameraActivity extends AppCompatActivity {
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private final Log log = new Log("H-IAAC Camera");
    private final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};
    PreviewView viewFinder;
    Button captureButton;
    final ExecutorService executor = Executors.newSingleThreadExecutor();
    Boolean isRecording = false;
    long labelId;
    private Long startEpochMilli;
    private Long endEpochMilli;
    private Boolean epochTimeIsServerBased;
    private long serverTimeDiff;
    private PowerManager.WakeLock mWakeLock;
    private Context appContext;
    private Permissions permissions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = this;
        appContext = this.getApplicationContext();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_camera);
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_camera);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                labelId = extras.getLong("LABEL_ID");
            }
        } else {
            labelId = (Long) savedInstanceState.getSerializable("LABEL_ID");
        }

        permissions = new Permissions(this, this.getApplicationContext());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "HIAAC:VIDEO_RECORD");
        mWakeLock.acquire();

        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startFilming(ctx);
                } catch (Exception e) {
                    isRecording = false;
                    log.d("Exception while filming: " + e.getMessage());
                    Toast.makeText(ctx, "Fail: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setButtonAsStop();
                }
            }
        });

        if (videoFileExists())
            showDialog();

        if (permissions.isCameraPermissionsGranted()) {
            // Only starts camera if all permission has been granted
            startCamera();
        } else {
            Toast.makeText(appContext, "Missing CAMERA permission.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onPause() {
        // If activity goes to pause state, and is recording
        if (isRecording) {
            // then stop the record
            Toast.makeText(appContext, "Filming is now stopped!", Toast.LENGTH_LONG).show();
            stopRecording();
        }
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (isRecording) {
            log.d("Ignore back button while filming");
            Toast.makeText(appContext, "Can't go back while filming.", Toast.LENGTH_SHORT).show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (isRecording) {
            log.d("Ignore back button while filming");
            Toast.makeText(appContext, "Can't go back while filming.", Toast.LENGTH_SHORT).show();
            return false;
        }

        finish();
        return true;
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {
        Context ctx = this.getApplicationContext();
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                            Preview preview = (new Preview.Builder()).build();
                            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                            Recorder recorder = new Recorder.Builder()
                                    .setQualitySelector(QualitySelector.from(
                                            Quality.SD,
                                            FallbackStrategy.higherQualityOrLowerThan(Quality.LOWEST)))
                                    .build();
                            videoCapture = VideoCapture.withOutput(recorder);

                            ImageCapture imageCapture = new ImageCapture.Builder().build();
                            cameraProvider.unbindAll();

                            cameraProvider.bindToLifecycle(CameraActivity.this,
                                                           cameraSelector,
                                                           preview,
                                                           imageCapture,
                                                           videoCapture);

                        } catch (Exception e) {
                            Toast.makeText(ctx, "Failed: " + e.getCause(), Toast.LENGTH_SHORT).show();
                            log.i("Failed to start camera: " + e.getMessage());
                        }
                    }
                }, ContextCompat.getMainExecutor(this)
        );
    }

    @SuppressLint({"RestrictedApi", "MissingPermission"})
    public final void startFilming(Context ctx) throws IOException {

        if (videoCapture == null) return;
        if (executor == null) return;

        if (isRecording) {
             new AlertDialog.Builder(ctx)
                    .setMessage(R.string.stop_filming_msg)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Cancel filming
                            stopRecording();
                        }})
                    .setNegativeButton(android.R.string.no, null).create().show();
        } else {
            final File outputFile = File.createTempFile("video_", ".mp4", getPath());

            FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(outputFile).build();

            currentRecording = videoCapture.getOutput()
                    .prepareRecording(CameraActivity.this, fileOutputOptions)
                    .start(executor, new Consumer<VideoRecordEvent>() {
                        @Override
                        public void accept(VideoRecordEvent videoRecordEvent) {
                            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                                startEpochMilli = TimeSync.getTimestamp();
                                epochTimeIsServerBased = TimeSync.isUsingServerTime();
                                serverTimeDiff = 0L;

                                if (TimeSync.isUsingServerTime()) {
                                    serverTimeDiff = TimeSync.getTimestampDiffFromServerAndLocal();
                                }
                            }
                            else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                endEpochMilli = TimeSync.getTimestamp();
                            }

                            // Filming has stop
                            currentRecording = null;

                            List<File> filesList;
                            File directory = getPath();
                            if (directory.exists()) {
                                filesList = new ArrayList<>(Arrays.asList(directory.listFiles()));

                                // Each experiment must have only 1 video,
                                // so when filming ends, previous video must be deleted.
                                for (File file: filesList) {
                                    // Ignore csv file, and ignore the file with same name
                                    // from the one created in the latest film.
                                    if (!"csv".equals(Tools.getFileExtension(file.getName())) &&
                                        !file.getName().equals(outputFile.getName())) {
                                        file.delete();
                                    }
                                }

                                try {
                                    VideoMetadata.create(outputFile.getName(),
                                                         endEpochMilli - startEpochMilli,
                                                         startEpochMilli,
                                                         endEpochMilli,
                                                         epochTimeIsServerBased,
                                                         serverTimeDiff,
                                                         getPath());
                                } catch (Exception e) {
                                    Toast.makeText(ctx, "Failed to access video metadata", Toast.LENGTH_SHORT).show();
                                }
                            }

                            // After stop filming, return to previous activity
                            finish();
                        }
                    });

            isRecording = true;
            setButtonAsStop();
        }
    }

    private File getPath() {
        File directory = new File(
                this.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        labelId +
                        File.separator);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        return directory;
    }

    private Boolean videoFileExists() {
        for (File file : getPath().listFiles()) {
            if ("mp4".equals(Tools.getFileExtension(file.getName())))
                return true;
        }
        return false;
    }

    private void showDialog() {
        new AlertDialog.Builder(this)
        .setTitle(R.string.camera_dialog_title)
        .setMessage(R.string.camera_dialog_msg)
        .setPositiveButton(R.string.camera_dialog_btn, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        })
        .show();
    }

    private void stopRecording() {
        log.d("Stop filming confirmed");
        currentRecording.stop();
        isRecording = false;
        setButtonAsRecord();
    }
    private void setButtonAsRecord() {
        captureButton.setBackground(this.getResources().getDrawable(R.drawable.baseline_record_24));
    }

    private void setButtonAsStop() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setStroke(10, Color.WHITE);
        drawable.setColor(Color.BLACK);
        drawable.setAlpha(150);
        captureButton.setBackground(drawable);
    }

    @Override
    protected void onDestroy() {
        if (mWakeLock != null) {
            mWakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong("LABEL_ID", labelId);
    }
}