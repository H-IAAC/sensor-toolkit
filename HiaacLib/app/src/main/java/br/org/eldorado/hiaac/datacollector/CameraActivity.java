package br.org.eldorado.hiaac.datacollector;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.sensorsdk.util.Log;

public class CameraActivity extends AppCompatActivity {
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private Log log = new Log("H-IAAC Camera");
    ActivityResultLauncher<String[]> rpl;
    private String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    PreviewView viewFinder;
    Button captureButton;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Boolean recording = false;
    long labelId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context ctx = this.getApplicationContext();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_camera);
        }
        setContentView(R.layout.activity_camera);

        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                labelId = extras.getLong("LABEL_ID");
            }
        } else {
            labelId = (Long) savedInstanceState.getSerializable("LABEL_ID");
        }

        viewFinder = findViewById(R.id.viewFinder);
        captureButton = findViewById(R.id.capture_button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startFilming();
                } catch (Exception e) {
                    Toast.makeText(ctx, "Check permissions", Toast.LENGTH_SHORT).show();
                    setButtonAsStop(captureButton);
                }
            }
        });

        if (videoFileExists())
            showDialog();

        rpl = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        if (allPermissionsGranted()) {
                            startCamera();
                        } else {
                            Toast.makeText(getApplicationContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }
        );
        if (allPermissionsGranted()) {
            // Only starts camera if all permission has been granted
            startCamera();
        } else {
            rpl.launch(REQUIRED_PERMISSIONS);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
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
                            ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();
                            Preview preview = (new Preview.Builder()).build();
                            preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                            CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                            Recorder recorder = new Recorder.Builder()
                                    .setQualitySelector(QualitySelector.from(
                                            Quality.HD,
                                            FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
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
    public final void startFilming() throws IOException {

        Context ctx = this.getApplicationContext();

        if (videoCapture == null) return;
        if (executor == null) return;

        if (recording) {
            currentRecording.stop();
            recording = false;
            setButtonAsRecord(captureButton);
        } else {
            final File outputFile = File.createTempFile("video_", ".mp4", getPath());

            FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(outputFile).build();

            currentRecording = ((Recorder) videoCapture.getOutput())
                    .prepareRecording(CameraActivity.this, fileOutputOptions)
                    .withAudioEnabled()
                    .start(executor, new Consumer<VideoRecordEvent>() {
                        @Override
                        public void accept(VideoRecordEvent videoRecordEvent) {
                            if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                                // Filming has stop
                                currentRecording = null;

                                try {
                                    BasicFileAttributes attr = Files.readAttributes(Paths.get(outputFile.getAbsolutePath()), BasicFileAttributes.class);
                                    long modifiedAt = attr.lastModifiedTime().toMillis();
                                    long lastAccessAt = attr.lastAccessTime().toMillis();

                                    createVideoMetadataFile(outputFile.getName(),
                                                            outputFile.getName(),
                                                            modifiedAt - lastAccessAt,
                                                            lastAccessAt,
                                                            modifiedAt);
                                } catch (Exception e) {
                                    Toast.makeText(ctx, "Failed to access video metadata", Toast.LENGTH_SHORT).show();
                                }

                                List<File> filesList = new ArrayList<File>();
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
                                }

                            }
                        }
                    });

            recording = true;
            setButtonAsStop(captureButton);
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

    private void createVideoMetadataFile(String filename,
                                         String videoFilename,
                                         float videoDuration,
                                         long startTime,
                                         long endTime) {

        String content = "[Metadata]\n";
        content += "filename = " + videoFilename + "\n";
        content += "videoDuration = " + videoDuration + "\n";
        content += "startTimestamp = " + startTime + "\n";
        content += "endTimestamp = " + endTime;

        File metadataFile = new File(getPath().getAbsoluteFile() + File.separator + filename + ".video");

        try {
            metadataFile.deleteOnExit();
            metadataFile.createNewFile();

            FileOutputStream writer = new FileOutputStream(metadataFile);
            writer.write(content.getBytes());
            writer.close();
        } catch (IOException e) {
            log.i("Error creating video metadata file.");
        }

        log.i("File: " + metadataFile.getAbsolutePath() + " created.");
    }

    private void setButtonAsRecord(Button button) {
        button.setBackground(this.getResources().getDrawable(R.drawable.baseline_record_24));
    }

    private void setButtonAsStop(Button button) {
        button.setBackground(this.getResources().getDrawable(R.drawable.baseline_stop_24));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}