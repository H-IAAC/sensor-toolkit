package br.org.eldorado.hiaac.datacollector.view.adapter;


import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.org.eldorado.hiaac.datacollector.CameraActivity;
import br.org.eldorado.hiaac.datacollector.LabelOptionsActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ClientAPI;
import br.org.eldorado.hiaac.datacollector.api.StatusResponse;
import br.org.eldorado.hiaac.datacollector.data.LabelConfig;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.SensorFrequency;
import br.org.eldorado.hiaac.datacollector.firebase.FirebaseListener;
import br.org.eldorado.hiaac.datacollector.firebase.FirebaseUploadController;
import br.org.eldorado.hiaac.datacollector.layout.AnimatedLinearLayout;
import br.org.eldorado.hiaac.datacollector.model.DataTrack;
import br.org.eldorado.hiaac.datacollector.service.ExecutionService;
import br.org.eldorado.hiaac.datacollector.service.listener.ExecutionServiceListenerAdapter;
import br.org.eldorado.hiaac.datacollector.util.CsvFiles;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Preferences;
import br.org.eldorado.hiaac.datacollector.util.TimeSync;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.hiaac.datacollector.util.Utils;
import br.org.eldorado.hiaac.datacollector.util.VideoMetadata;
import br.org.eldorado.hiaac.datacollector.util.AlarmConfig;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {
    private final int SEND_DATA_TO_FIREBASE = 0;
    private final int CREATE_CSV_FILE = 1;
    private final LayoutInflater mInflater;
    private List<LabelConfig> labelConfigs;
    private Set<Integer> labelsConflicts = new HashSet<>();
    private Map<Long, List<SensorFrequency>> sensorFrequencyMap;
    private Context mContext;
    private ExecutionService execService;
    private ServiceConnection serviceConnection;
    private ServiceConnection checkingServiceConnection;
    private final Log log = new Log("LabelRecyclerViewAdapter");
    private ProgressDialog sendDataDialog;
    private LabelConfigViewModel mLabelConfigViewModel;
    private static final Map<String, ViewHolder> holdersMap = new HashMap<>();
    private boolean deleteButtonClicked;
    private final CsvFiles csvFiles;
    private AlertDialog dialog;
    private Boolean isSendingData = false;

    public LabelRecyclerViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        csvFiles = new CsvFiles(context);
    }

    public void setLabelConfigs(List<LabelConfig> labels) {
        // Check if there is conflicts between the labels
        labelsConflicts = new HashSet<>();
        for (int i = 0; i < labels.size() - 1; ++i) {
            for (int j = 1; j < labels.size(); ++j) {
                if (i == j) continue;
                if (labels.get(i).experiment.equals(labels.get(j).experiment) &&
                        labels.get(i).activity.equals(labels.get(j).activity) &&
                        labels.get(i).userId.equals(labels.get(j).userId)) {
                    labelsConflicts.add(i);
                    labelsConflicts.add(j);
                }
            }
        }

        this.labelConfigs = labels;
        notifyDataSetChanged();
    }

    public void setSensorFrequencyMap(Map<Long, List<SensorFrequency>> sensorFrequencyMap) {
        this.sensorFrequencyMap = sensorFrequencyMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.label_item, parent, false);
        return new ViewHolder(view);
    }

    private void expandOption(LabelRecyclerViewAdapter.ViewHolder holder, View v) {
        if (holder.isOpened) {
            holder.getButtonContainer().close();
            holder.setOpened(false);
        } else {
            resizeLabelPanel(holder);
            holder.setOpened(true);
        }
    }

    private String getHolderKey(String experiment, String activity, String user) {
        return experiment + '_' + activity + '_' + user;
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        log.d("CSVFilesRecyclerAdapter - ActiveThreads: " + Thread.activeCount());
        LabelConfig labelConfig = labelConfigs.get(holder.getAdapterPosition());

        holdersMap.put(getHolderKey(labelConfig.experiment,
                                    labelConfig.activity,
                                    labelConfig.userId), holder);

        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance((Application) mContext.getApplicationContext()).create(LabelConfigViewModel.class);

        RecyclerView csvList = holder.getCsvRecyclerView();
        List<File> filesList = csvFiles.getFiles(labelConfig.id);

        csvList.setAdapter(new CSVFilesRecyclerAdapter(mContext, filesList, mLabelConfigViewModel.getLabelConfigRepository(), labelConfig.id));
        csvList.setLayoutManager(new LinearLayoutManager(mContext));

        holder.getLabelTitle().setText(labelConfig.experiment);
        holder.getLabelTitle().setOnClickListener(v -> {
            expandOption(holder, v);
        });

        holder.getLabelActivity().setOnClickListener(v -> {
            expandOption(holder, v);
        });

        holder.getLabelActivity().setText(labelConfig.activity);
        holder.getLabelDeviceLocation().setText(labelConfig.userId + " - " + labelConfig.deviceLocation);
        holder.getLabelDeviceLocation().setOnClickListener(v -> {
            expandOption(holder, v);
        });

        if (labelsConflicts.contains(position))
            holder.getLabelConflict().setVisibility(View.VISIBLE);
        else
            holder.getLabelConflict().setVisibility(View.INVISIBLE);

        holder.getLabelTimer().setText(
                Tools.getFormatedTime(labelConfigs.get(holder.getAdapterPosition()).stopTime, Tools.CHRONOMETER));

        Button editButton = holder.getEditButton();
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(editButton.getContext(), LabelOptionsActivity.class);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, UPDATE_LABEL_CONFIG_ACTIVITY);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_ID, labelConfig.id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                editButton.getContext().startActivity(intent);
            }
        });

        ImageView shareButton = holder.getShareButton();
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog alert = new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.
                                share_with_server))
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        shareButton.setEnabled(false);
                                        upload(holder);
                                    }
                                }
                        )
                        .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                alert.setTitle(mContext.getString(R.string.share_with_server_title));
                alert.show();

            }
        });

        ImageView filmButton = holder.getFilmButton();
        filmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(filmButton.getContext(), CameraActivity.class);
                intent.putExtra("LABEL_ID", labelConfig.id);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_ID, labelConfig.id);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                filmButton.getContext().startActivity(intent);
            }
        });

        ImageView deleteButton = holder.getDeleteButton();
        deleteButtonClicked = false;
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(mContext);
                AlertDialog aDialog = aDialogBuilder.create();
                aDialog.setTitle(mContext.getString(R.string.delete_config_title));
                aDialog.setMessage(mContext.getString(R.string.delete_config_confirmation));

                aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInt, int which) {
                        aDialog.dismiss();
                    }
                });
                aDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInt, int which) {
                        deleteButtonClicked = true;
                        aDialog.dismiss();
                        mLabelConfigViewModel.getLabelConfigById(labelConfig.id)
                                .observe((LifecycleOwner) mContext, new Observer<LabelConfig>() {
                                    @Override
                                    public void onChanged(LabelConfig labelConfig) {
                                        try {
                                            if (deleteButtonClicked && labelConfig != null) {
                                                deleteButtonClicked = false;
                                                mLabelConfigViewModel.deleteConfig(labelConfig);
                                                mLabelConfigViewModel.deleteSensorsFromLabel(labelConfig);
                                                csvFiles.deleteDirectory(labelConfig.id);
                                            }
                                        } catch (Exception e) {
                                            log.d("Delete experiment exception: " + e.getMessage());
                                        }
                                    }
                                });
                    }
                });
                aDialog.show();
            }
        });

        Button startButton = holder.getStartButton();
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startButton.getText().equals(mContext.getResources().getString(R.string.start))) {
                    startExecution(holder);
                } else {
                    if (execService != null) {
                        execService.stopExecution();
                    }
                }
            }
        });

        Date alarmSchedule = AlarmConfig.configureScheduler(labelConfig,
                                                            getHolderKey(labelConfig.experiment,
                                                                         labelConfig.activity,
                                                                         labelConfig.userId));

        if (alarmSchedule == null) {
            log.d("Scheduler: No alarm configured for [" + labelConfig.experiment + "] id: " + labelConfig.id);
        } else {
            log.d("Scheduler: Alarm configured for [" + labelConfig.experiment + "] id: " + labelConfig.id);
        }

        checkExecution(holder);
    }

    private void resizeLabelPanel(ViewHolder holder) {
        if (((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).getItemCount() > 0) {
            holder.getButtonContainer().expand(300);
        } else {
            holder.getButtonContainer().expand(54);
        }
    }

    private void upload(ViewHolder holder) {
        sendFilesToServer(csvFiles.getFiles(labelConfigs.get(holder.getAdapterPosition()).id),
                                            holder);
    }

    private void sendData(ViewHolder holder, int type, boolean showDialog, String dataUid) {

        if (isSendingData) return;

        isSendingData = true;

        if (showDialog) {
            sendDataDialog = ProgressDialog.show(mContext, "Export to CSV File", "Creating CSV", true);
        }
        FirebaseUploadController firebase = new FirebaseUploadController(mContext);
        firebase.registerListener(new FirebaseListener() {

            @Override
            public void onProgress(String message) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (showDialog) {
                            if (sendDataDialog != null)
                                sendDataDialog.setMessage(message);
                        }
                    }
                });
            }

            @Override
            public void onCompleted(String message) {
                onSendDataCompleted(message, sendDataDialog, holder);
                sendDataDialog = null;
            }
        });

        if (type == CREATE_CSV_FILE) {
            firebase.exportToCSV(dataUid, labelConfigs.get(holder.getAdapterPosition()).id);
        } else if (type == SEND_DATA_TO_FIREBASE) {
            AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(mContext);
            AlertDialog aDialog = aDialogBuilder.create();
            aDialog.setTitle(mContext.getString(R.string.share_with_firebase_title));
            aDialog.setMessage(mContext.getString(R.string.share_with_firebase));

            aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.no), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInt, int which) {
                    sendDataDialog.dismiss();
                    aDialog.dismiss();
                    ((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).updateFileList(csvFiles.getFiles(labelConfigs.get(holder.getAdapterPosition()).id));
                    resizeLabelPanel(holder);
                }
            });
            aDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInt, int which) {
                    aDialog.dismiss();
                    firebase.uploadCSVFile(labelConfigs.get(holder.getAdapterPosition()).experiment, labelConfigs.get(holder.getAdapterPosition()).id);
                }
            });
            aDialog.show();
        }

        isSendingData = false;
    }

    private void onSendDataCompleted(String message, ProgressDialog dialog, ViewHolder holder) {
        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (holder.getAdapterPosition() == -1 || holder.getAdapterPosition() >= labelConfigs.size()) return;
                if (dialog != null) {
                    dialog.cancel();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(message);
                    builder.setIcon(R.drawable.ic_baseline_success);
                    AlertDialog dl = builder.create();
                    dl.show();
                }
                List<File> files = csvFiles.getFiles(labelConfigs.get(holder.getAdapterPosition()).id);
                ((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).updateFileList(files);
                if (dialog != null) {
                    resizeLabelPanel(holder);
                }

                if (dialog != null && labelConfigs.get(holder.getAdapterPosition()).sendToServer) {
                    AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(mContext);
                    AlertDialog aDialog = aDialogBuilder.create();
                    aDialog.setTitle(mContext.getString(R.string.share_with_server_title));
                    aDialog.setMessage(mContext.getString(R.string.share_with_server));

                    aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.no), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInt, int which) {
                            aDialog.dismiss();
                        }
                    });
                    aDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.yes), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInt, int which) {
                            aDialog.dismiss();
                            sendFilesToServer(files, holder);
                        }
                    });
                    aDialog.show();
                }
            }
        });
    }

    private int numberOfFilesToUpload = 0;

    private void sendFilesToServer(List<File> files, ViewHolder holder) {
        log.d("sendFilesToServer - " + files);
        numberOfFilesToUpload = files.size();
        if (numberOfFilesToUpload == 0) {
            holder.getShareButton().setEnabled(true);
            AlertDialog alert = new AlertDialog.Builder(mContext).setMessage(mContext.getString(R.string.
                            share_with_server_no_files))
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
            alert.setTitle(mContext.getString(R.string.share_with_server_title));
            alert.show();
            return;
        }

        String experiment = labelConfigs.get(holder.getAdapterPosition()).experiment;
        String activity = labelConfigs.get(holder.getAdapterPosition()).activity;
        String userId = labelConfigs.get(holder.getAdapterPosition()).userId;

        files.forEach((file) -> {
            if ("mp4".equals(Tools.getFileExtension(file.getName()))) {
                log.i("sendFilesToServer sending video: " + file.getName());

                try {
                    VideoMetadata.Metadata metadata = VideoMetadata.read(file.getAbsoluteFile() + ".video");

                    String directory = experiment + " [" + activity + "] [" + userId + "]";

                    MultipartBody.Part directoryPart =
                            MultipartBody.Part.createFormData("directory", directory);
                    MultipartBody.Part startTimePart =
                            MultipartBody.Part.createFormData("startTimestamp", metadata.startTimestamp);
                    MultipartBody.Part endtTimePart =
                            MultipartBody.Part.createFormData("endTimestamp", metadata.endTimestamp);
                    MultipartBody.Part videoDurationPart =
                            MultipartBody.Part.createFormData("videoduration", metadata.videoDuration);

                    uploadVideo(file, directoryPart, startTimePart, endtTimePart, videoDurationPart, holder);

                } catch (FileNotFoundException e) {
                    Toast.makeText(mContext, "Video missing metadata file ('<file>.video')", Toast.LENGTH_SHORT).show();
                    holder.getShareButton().setEnabled(true);
                } catch (IOException e) {
                    Toast.makeText(mContext, "Error reading Video metadata file ('<file>.video')", Toast.LENGTH_SHORT).show();
                    holder.getShareButton().setEnabled(true);
                }

            } else if ("csv".equals(Tools.getFileExtension(file.getName()))) {
                MultipartBody.Part experimentPart =
                        MultipartBody.Part.createFormData("experiment", experiment);
                MultipartBody.Part namePart =
                        MultipartBody.Part.createFormData("activity", activity);
                MultipartBody.Part subjectPart =
                        MultipartBody.Part.createFormData("subject", userId);

                try {
                    File zippedFile = Tools.zipFile(file);
                    log.i("sendFilesToServer sending file: " + file.getName());
                    log.i("sendFilesToServer File was compressed! Original size: " + Files.size(file.toPath()) + " Compressed Size: " + Files.size(zippedFile.toPath()));
                    uploadFile(zippedFile, experimentPart, subjectPart, namePart, holder);
                } catch (IOException e) {
                    numberOfFilesToUpload--;
                    log.d("Send zip file exception: " + e.getMessage());
                }
            } else {
                // Files different from mp4 and csv arent sent to the server.
                numberOfFilesToUpload--;
            }
        });
    }

    private void uploadFile(File file, MultipartBody.Part experiment, MultipartBody.Part subject, MultipartBody.Part name, ViewHolder holder) {
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file", file.getName(),
                RequestBody.create(MediaType.parse("multipart/form-data"), file));

        Call<StatusResponse> call = ClientAPI.get(ClientAPI.httpHighTimeout()).uploadFile(filePart, experiment, subject, name);
        call.enqueue(uploadCallback(file, holder));
    }

    private void uploadVideo(File file, MultipartBody.Part directory, MultipartBody.Part start, MultipartBody.Part end, MultipartBody.Part videoDuration, ViewHolder holder) {
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file", file.getName(),
                RequestBody.create(MediaType.parse("multipart/form-data"), file));

        Call<StatusResponse> call = ClientAPI.get(ClientAPI.httpHighTimeout()).uploadVideo(filePart, directory, start, end, videoDuration);
        call.enqueue(uploadCallback(file, holder));
    }

    private synchronized void uploadMessage(ViewHolder holder, String filename, String errorMessage) {
        numberOfFilesToUpload--;
        if (numberOfFilesToUpload <= 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            if (errorMessage != null) {
                builder.setTitle("Error");
                String message = filename + "\n" + errorMessage;
                builder.setMessage(message);
                builder.setIcon(R.drawable.ic_baseline_error);
            } else {
                builder.setTitle("Success");
                builder.setIcon(R.drawable.ic_baseline_success);
            }

            AlertDialog dl = builder.create();
            dl.show();
            holder.getShareButton().setEnabled(true);
        }
    }

    private Callback<StatusResponse> uploadCallback(final File file, final ViewHolder holder) {
        return new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {

                if (response.code() == 200) {
                    // Upload success
                    uploadMessage(holder, file.getName(), null);
                } else {
                    // Upload error
                    try {
                        JSONObject jObjError = new JSONObject(response.errorBody().string());
                        uploadMessage(holder, file.getName(), jObjError.getString("status"));
                        log.d("upload status body: " + jObjError.getString("status"));
                    } catch (Exception e) {
                        uploadMessage(holder, file.getName(), "Upload failed.");
                        log.d(file.getName() + " upload failed: " + e.getMessage());
                    }
                }

                // If upload is .zip, delete it.
                if (file.getName().endsWith(".zip")) {
                    file.delete();
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                numberOfFilesToUpload = 0;
                uploadMessage(holder, file.getName(), t.getMessage());
                log.d("FAIL " + t.getMessage());
                call.cancel();
            }
        };
    }

    private void sendToFirebase(ViewHolder holder) {
        sendData(holder, SEND_DATA_TO_FIREBASE, true, "0");
    }

    public static ViewHolder getViewHolder(String holderKey) {
        if (holdersMap.containsKey(holderKey)) {
            return holdersMap.get(holderKey);
        }
        return null;
    }

    private boolean labelConfigContains(ViewHolder holder) {
        for (LabelConfig lbl : labelConfigs) {
            if (lbl.experiment.equals(holder.getLabelTitle().getText())
                    && lbl.activity.equals(holder.getLabelActivity().getText()))
                return true;
        }
        return false;
    }

    public void startExecution(ViewHolder holder) {
        if (!labelConfigContains(holder) || holder.isStarted() || (execService != null && execService.isRunning() != null)) {
            log.d("startExecution could not start! holder.isStarted() is [" + holder.isStarted() + "]");
            log.d("startExecution could not start! labelConfigContains(holder) is [" + labelConfigContains(holder) + "]");
            log.d("startExecution could not start! (execService != null) is [" + (execService != null) + "]");
            log.d("startExecution could not start! execService.isRunning() is [" + ((execService != null) ? execService.isRunning() : "execService is null") + "]");
            return;
        }

        holder.setStarted(true);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                log.d("startExecution - onServiceConnected");
                ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                execService = binder.getServer();
                DataTrack dt = getDataTrack(holder);

                dt.addSensorList(sensorFrequencyMap.get(dt.getConfigId()));
                execService.startExecution(new MyExecutionListener(dt, holder));
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                log.d("startExecution - onServiceDisconnected");
            }
        };

        if (execService == null || execService.isRunning() == null) {
            AlertDialog.Builder timer = new AlertDialog.Builder(mContext);
            dialog = timer.create();
            dialog.setTitle(mContext.getString(R.string.experiment_timer_title));
            dialog.setMessage("\t 10");
            dialog.setCancelable(false);

            String serviceTitle = holder.labelTitle.getText().toString() + " " + holder.labelActivity.getText().toString();
            CountDownTimer countDown = launchCounterToStartService(serviceTitle);

            try {
                dialog.show();
            } catch (Exception e) {
                log.e("App is not running!");
                Toast.makeText(mContext, "App is not running!", Toast.LENGTH_LONG).show();
                return;
            }

            TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
            messageView.setTextSize(30);
            countDown.start();
        } else {
            log.d("startExecution - startForegroundService");
            Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
            execServiceIntent.setAction(ExecutionService.ACTION_START_ANOTHER_FOREGROUND_SERVICE);
            execServiceIntent.putExtra("Title", holder.labelTitle.toString());
            mContext.startForegroundService(execServiceIntent);
            mContext.bindService(execServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private CountDownTimer launchCounterToStartService(String title) {
        return new CountDownTimer((Preferences.getPreferredStartDelay() * 1000), 1000) {
            @Override
            public void onTick(long timeRemaining) {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage("\t" + timeRemaining / 1000);
                    }
                });
            }

            @Override
            public void onFinish() {
                try {
                    dialog.dismiss();
                } catch (IllegalArgumentException e) {
                    log.e("App is not running");
                }

                Utils.emitStartBeep();

                Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
                execServiceIntent.setAction(ExecutionService.ACTION_START_FOREGROUND_SERVICE);
                execServiceIntent.putExtra("Title", title);
                log.d("startExecution - Counter finished! starting Foreground services");

                if (mContext.startForegroundService(execServiceIntent) == null)
                    log.d("startExecution - Failed to startForegroundService");

                if (!mContext.bindService(execServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE))
                    log.d("startExecution - Failed to bindService, system couldn't find the service or client doesn't have permission to bind to it.");
            }
        };
    }

    private DataTrack getDataTrack(ViewHolder holder) {
        DataTrack dt = new DataTrack();
        try {
            dt.setDeviceLocation(labelConfigs.get(holder.getAdapterPosition()).deviceLocation);
            dt.setUserId(labelConfigs.get(holder.getAdapterPosition()).userId);
            dt.setSendFilesToServer(labelConfigs.get(holder.getAdapterPosition()).sendToServer);
            dt.setActivity(labelConfigs.get(holder.getAdapterPosition()).activity);
            dt.setConfigId(labelConfigs.get(holder.getAdapterPosition()).id);
            dt.setStopTime(labelConfigs.get(holder.getAdapterPosition()).stopTime);
            dt.setLabel(labelConfigs.get(holder.getAdapterPosition()).experiment);
        } catch (Exception e) {
            log.e("getDataTrack - Failed to get data from ViewHolder: " + e.getMessage());
        }
        return dt;
    }

    private void checkExecution(ViewHolder holder) {

        if (!Preferences.shouldRunChecking())
            return;

        checkingServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                log.d("checkExecution - onServiceConnected");
                ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                execService = binder.getServer();
                DataTrack dt = getDataTrack(holder);

                dt.addSensorList(sensorFrequencyMap.get(dt.getLabel()));

                if (dt.equals(execService.isRunning())) {
                    log.d("checkExecution - Experiment already running " + dt.getLabel());
                    holder.getEditButton().setEnabled(false);
                    setAsStop(holder.getStartButton(), holder);
                    execService.changeExecutionServiceListener(new MyExecutionListener(dt, holder));
                } else {
                    log.d("checkExecution - sending data to " + dt.getLabel());
                    sendData(holder, CREATE_CSV_FILE, false, "0");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) { }
        };

        Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
        execServiceIntent.setAction(ExecutionService.ACTION_CHECK_FOREGROUND_SERVICE);
        execServiceIntent.putExtra("Title", holder.labelTitle.getText().toString());
        mContext.startForegroundService(execServiceIntent);
        mContext.bindService(execServiceIntent, checkingServiceConnection, Context.BIND_AUTO_CREATE);
        Preferences.setToRunChecking(false);
    }

    @Override
    public int getItemCount() {
        if (labelConfigs != null) {
            return labelConfigs.size();
        }
        return 0;
    }

    private void setAsStop(Button button, LabelRecyclerViewAdapter.ViewHolder holder) {
        holder.getExpCard().setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.inprogress));

        button.setText(mContext.getResources().getString(R.string.stop));
        button.setBackgroundTintList(mContext.getResources().getColorStateList(android.R.color.holo_red_light));

        holder.filmButton.setClickable(false);
        holder.shareButton.setClickable(false);
        holder.deleteButton.setClickable(false);
        holder.deleteButton.setEnabled(false);
        holder.editButton.setClickable(false);
    }

    private void setAsStart(Button button, LabelRecyclerViewAdapter.ViewHolder holder) {
        holder.getExpCard().setCardBackgroundColor(holder.getExpCardColor());

        button.setText(mContext.getResources().getString(R.string.start));
        button.setBackgroundTintList(mContext.getResources().getColorStateList(android.R.color.holo_green_light));

        holder.filmButton.setClickable(true);
        holder.shareButton.setClickable(true);
        holder.deleteButton.setClickable(true);
        holder.deleteButton.setEnabled(true);
        holder.editButton.setClickable(true);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private boolean isOpened;

        private CardView expCard;
        private ColorStateList expCardColor;
        private TextView labelTitle;
        private TextView labelActivity;
        private TextView labelConflict;
        private TextView labelDeviceLocation;
        private TextView labelTimer;
        private AnimatedLinearLayout buttonContainer;
        private Button startButton;
        private Button stopButton;
        private Button editButton;
        private ImageView shareButton;
        private ImageView filmButton;
        private ImageView deleteButton;
        private ImageView statisticsButton;
        private RecyclerView csvRecyclerView;
        private boolean started;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            started = false;
            isOpened = false;
            expCard = itemView.findViewById(R.id.exp_card);
            expCardColor = expCard.getCardBackgroundColor();
            labelTitle = itemView.findViewById(R.id.label_title);
            labelActivity = itemView.findViewById(R.id.label_activity);
            labelConflict = itemView.findViewById(R.id.label_conflict);
            labelDeviceLocation = itemView.findViewById(R.id.label_device_location);
            labelTimer = itemView.findViewById(R.id.label_timer);
            buttonContainer = itemView.findViewById(R.id.label_button_container);
            startButton = itemView.findViewById(R.id.start_sampling_button);
            editButton = itemView.findViewById(R.id.edit_sampling_button);
            shareButton = itemView.findViewById(R.id.share_sampling_button);
            filmButton = itemView.findViewById(R.id.film_sampling_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
            csvRecyclerView = itemView.findViewById(R.id.csvfiles_reclyclerView);
            csvRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                @Override
                public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                    int action = e.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_MOVE:
                            rv.getParent().requestDisallowInterceptTouchEvent(true);
                            break;
                    }
                    return false;
                }

                @Override
                public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

                @Override
                public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
            });
        }

        public void setStarted(boolean b) {
            this.started = b;
        }

        public boolean isStarted() {
            return started;
        }

        public void setOpened(boolean opened) {
            isOpened = opened;
        }

        public TextView getLabelTitle() {
            return labelTitle;
        }

        public TextView getLabelActivity() {
            return labelActivity;
        }

        public TextView getLabelConflict() {
            return labelConflict;
        }

        public TextView getLabelDeviceLocation() {
            return labelDeviceLocation;
        }

        public TextView getLabelTimer() {
            return labelTimer;
        }

        public RecyclerView getCsvRecyclerView() {
            return csvRecyclerView;
        }

        public AnimatedLinearLayout getButtonContainer() {
            return buttonContainer;
        }

        public CardView getExpCard() { return expCard; };

        public ColorStateList getExpCardColor() { return expCardColor; };

        public Button getStartButton() {
            return startButton;
        }

        public Button getEditButton() {
            return editButton;
        }

        public ImageView getShareButton() {
            return shareButton;
        }

        public ImageView getFilmButton() {
            return filmButton;
        }

        public ImageView getDeleteButton() {
            return deleteButton;
        }
    }

    private class MyExecutionListener extends ExecutionServiceListenerAdapter {

        private ViewHolder holder;
        public MyExecutionListener(DataTrack dt, ViewHolder h) {
            super(dt);
            this.holder = h;
        }

        @Override
        public void onError(String message) {
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.d("MyExecutionListener - onError - " + message);
                    holder.getEditButton().setEnabled(true);
                    setAsStart(holder.getStartButton(), holder);
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Error");
                    builder.setMessage(message);
                    AlertDialog dl = builder.create();
                    dl.show();
                }
            });
        }

        @Override
        public void onRunning(long remainingTime) {
            // update clock ui
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String labelTimer = Tools.getFormatedTime((int)remainingTime/1000, Tools.CHRONOMETER);
                    holder.getLabelTimer().setText(labelTimer);
                }
            });
        }

        @Override
        public void onStopped() {
            // Enable buttons
            try {
                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        holder.getEditButton().setEnabled(true);
                        setAsStart(holder.getStartButton(), holder);
                        holder.getLabelTimer().setText(
                                Tools.getFormatedTime(labelConfigs.get(holder.getAdapterPosition()).stopTime, Tools.CHRONOMETER));

                        AlertDialog.Builder timer = new AlertDialog.Builder(mContext);
                        AlertDialog createCSVDialog;
                        createCSVDialog = timer.create();
                        createCSVDialog.setMessage(mContext.getString(R.string.before_create_csv_file));
                        createCSVDialog.setCancelable(false);
                        createCSVDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                sendData(holder, CREATE_CSV_FILE, true, getDataTrack().getUid());
                            }
                        });
                        CountDownTimer countDown = new CountDownTimer(5000, 1000) {
                            @Override
                            public void onTick(long timeRemaining) {
                            }

                            @Override
                            public void onFinish() {
                                createCSVDialog.dismiss();
                                sendData(holder, CREATE_CSV_FILE, true, getDataTrack().getUid());
                            }
                        };
                        createCSVDialog.show();
                        TextView messageView = (TextView) createCSVDialog.findViewById(android.R.id.message);
                        messageView.setGravity(Gravity.CENTER);
                        messageView.setTextSize(26);
                        countDown.start();
                        holder.setStarted(false);
                    }
                });
            } catch (WindowManager.BadTokenException e) {
                log.e("App is not running");
            }
        }

        @Override
        public void onStarted() {
            // Disable buttons
            ((Activity)mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    log.d("MyExecutionListener - disabling buttons");
                    holder.getEditButton().setEnabled(false);
                    setAsStop(holder.getStartButton(), holder);
                }
            });
        }

    }
}
