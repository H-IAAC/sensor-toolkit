package br.org.eldorado.hiaac.datacollector.view.adapter;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Application;
import android.app.PendingIntent;
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
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.org.eldorado.hiaac.datacollector.LabelOptionsActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.api.ApiInterface;
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
import br.org.eldorado.hiaac.datacollector.util.Tools;
import br.org.eldorado.sensorsdk.SensorSDK;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {

    private final int SEND_DATA_TO_FIREBASE = 0;
    private final int CREATE_CSV_FILE = 1;
    private final int SEND_DATA_TO_HIAAC = 2;


    private static final String TAG = "LabelRecyclerViewAdapter";
    private final LayoutInflater mInflater;
    private List<LabelConfig> labelConfigs;
    private Set<Integer> labelsConflicts = new HashSet<>();
    private Map<Long, List<SensorFrequency>> sensorFrequencyMap;
    private Context mContext;
    private ExecutionService execService;
    private ServiceConnection svc;
    private Log log;
    private ProgressDialog sendDataDialog;
    private LabelConfigViewModel mLabelConfigViewModel;
    private Map<String, ViewHolder> holdersMap = new HashMap<>();
    private boolean deleteButtonClicked;

    private CsvFiles csvFiles;

    public LabelRecyclerViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        csvFiles = new CsvFiles(context);
        log = new Log(TAG);
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

    private void expandOption (LabelRecyclerViewAdapter.ViewHolder holder, View v) {
        if (holder.isOpened) {
            holder.getButtonContainer().close();
            holder.setOpened(false);
        } else {
            resizeLabelPanel(holder);
            holder.setOpened(true);
        }
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        log.d("CSVFilesRecyclerAdapter - ActiveThreads: " + Thread.activeCount());
        LabelConfig labelConfig = labelConfigs.get(holder.getAdapterPosition());
        String labelTitle = labelConfig.experiment;
        holdersMap.put(labelTitle, holder);

        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance((Application)mContext.getApplicationContext()).create(LabelConfigViewModel.class);

        RecyclerView csvList = holder.getCsvRecyclerView();
        List<File> filesList = csvFiles.getFiles(labelConfig.id);

        csvList.setAdapter(new CSVFilesRecyclerAdapter(mContext, filesList, mLabelConfigViewModel.getLabelConfigRepository(), labelConfig.id));
        csvList.setLayoutManager(new LinearLayoutManager(mContext));

        holder.getLabelTitle().setText(labelTitle);
        holder.getLabelTitle().setOnClickListener(v -> { expandOption(holder, v); });

        holder.getLabelActivity().setOnClickListener(v -> { expandOption(holder, v); });

        holder.getLabelActivity().setText(labelConfig.activity);
        holder.getLabelDeviceLocation().setText(labelConfig.userId + " - " + labelConfig.deviceLocation);
        holder.getLabelDeviceLocation().setOnClickListener(v -> { expandOption(holder, v); });

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
                shareButton.setEnabled(false);
                sendData(holder, SEND_DATA_TO_HIAAC,false, "0");
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
                                            e.printStackTrace();
                                        }
                                    }
                                });
                    }
                });
                aDialog.show();
            }
        });

        Button stopButton = holder.getStopButton();
        stopButton.setEnabled(false);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (execService != null) {
                    execService.stopExecution();
                }
            }
        });

        Button startButton = holder.getStartButton();
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExecution(holder);
            }
        });

        checkExecution(holder);

        if (labelConfig.scheduledTime > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(labelConfig.scheduledTime);

            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(SensorSDK.getInstance().getRemoteTime());
            if (c.after(now)) {
                String action = "br.org.eldorado.schedule_collect_data";
                Intent i = new Intent(action);
                i.putExtra("holder", labelConfig.experiment);
                AlarmManager mgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
                long startsTime = labelConfig.scheduledTime - SensorSDK.getInstance().getRemoteTime();
                mgr.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + startsTime, pi);
            }

            startButton.setEnabled(false);
        }
    }

    private void resizeLabelPanel(ViewHolder holder) {
        if (((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).getItemCount() > 0) {
            holder.getButtonContainer().expand(300);
        } else {
            holder.getButtonContainer().expand(54);
        }
    }

    private void sendData(ViewHolder holder, int type, boolean showDialog, String dataUid) {
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
        } else if (type == SEND_DATA_TO_HIAAC) {
            sendFilesToServer(csvFiles.getFiles(labelConfigs.get(holder.getAdapterPosition()).id), holder);
        }
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

    private int filesToUpload = 0;
    private void sendFilesToServer(List<File> files, ViewHolder holder) {
        log.d("sendFilesToServer - " + files);
        MultipartBody.Part experimentPart =
                MultipartBody.Part.createFormData("experiment", labelConfigs.get(holder.getAdapterPosition()).experiment);
        MultipartBody.Part namePart =
                MultipartBody.Part.createFormData("activity", labelConfigs.get(holder.getAdapterPosition()).activity);
        MultipartBody.Part subjectPart =
                MultipartBody.Part.createFormData("subject", labelConfigs.get(holder.getAdapterPosition()).userId);
        filesToUpload = files.size();
        files.forEach((file) -> {
            MultipartBody.Part filePart = filePart = MultipartBody.Part.createFormData(
                    "file", file.getName(),
                    RequestBody.create(MediaType.parse("multipart/form-data"), file));

            ClientAPI apiClient = new ClientAPI();
            String address = Preferences.getPreferredServer().split(":")[0];
            String port = Preferences.getPreferredServer().split(":")[1];
            ApiInterface apiInterface = apiClient.getClient(address, port).create(ApiInterface.class);
            Call<StatusResponse> call = apiInterface.uploadFile(filePart, experimentPart, subjectPart, namePart);
            call.enqueue(uploadCallback(file, holder));

        });
    }

    private synchronized void updateFilesUpdated(ViewHolder holder, String errorMessage) {
        filesToUpload--;
        if (filesToUpload <= 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            if (errorMessage != null) {
                builder.setTitle("Error");
                builder.setMessage(errorMessage);
            } else {
                builder.setTitle("Success");
            }
            builder.setIcon(R.drawable.ic_baseline_success);
            AlertDialog dl = builder.create();
            dl.show();
            holder.getShareButton().setEnabled(true);
        }
    }

    private Callback<StatusResponse> uploadCallback(final File file, final ViewHolder holder) {
        return new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                if (response.body().getStatus().equals("200") || response.body().getStatus().equalsIgnoreCase("success")) {
                    updateFilesUpdated(holder, null);
                } else {
                    onFailure(call, new Exception("File: " + file.getName() + " -\n" + response.body().toString()));
                }
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                filesToUpload = 0;
                updateFilesUpdated(holder, t.getMessage());
                t.printStackTrace();
                log.d("FAIL " + t);
                call.cancel();
            }
        };
    }

    private void sendToFirebase(ViewHolder holder) {
        sendData(holder, SEND_DATA_TO_FIREBASE, true, "0");
    }

    public ViewHolder getViewHolder(String label) {
        if (holdersMap.containsKey(label)) {
            return holdersMap.get(label);
        }
        return null;
    }

    private AlertDialog dialog;
    public void startExecution(ViewHolder holder) {
        if (holder.isStarted() || (execService != null && execService.isRunning() != null)) return;
        holder.setStarted(true);
        svc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    log.d("Connected execService");
                    ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                    execService = binder.getServer();
                    DataTrack dt = new DataTrack();
                    String label = labelConfigs.get(holder.getAdapterPosition()).experiment;
                    int stopTime = labelConfigs.get(holder.getAdapterPosition()).stopTime;
                    long configId = labelConfigs.get(holder.getAdapterPosition()).id;

                    dt.setDeviceLocation(labelConfigs.get(holder.getAdapterPosition()).deviceLocation);
                    dt.setUserId(labelConfigs.get(holder.getAdapterPosition()).userId);
                    dt.setSendFilesToServer(labelConfigs.get(holder.getAdapterPosition()).sendToServer);
                    dt.setActivity(labelConfigs.get(holder.getAdapterPosition()).activity);
                    dt.setStopTime(stopTime);
                    dt.setConfigId(configId);
                    dt.setLabel(label);
                    dt.addSensorList(sensorFrequencyMap.get(configId));
                    execService.startExecution(new MyExecutionListener(dt, holder));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
        log.d("startExecution - disabling start button");
        holder.getStartButton().setEnabled(false);
        holder.getExpCard().setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.inprogress));

        //execService.setRemoteTime(System.currentTimeMillis() + (1000*60*60));

        if (execService.isRunning() == null) {
            AlertDialog.Builder timer = new AlertDialog.Builder(mContext);
            dialog = timer.create();
            dialog.setTitle(mContext.getString(R.string.experiment_timer_title));
            dialog.setMessage("\t 10");
            dialog.setCancelable(false);
            CountDownTimer countDown = new CountDownTimer(9000, 1000) {
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
                    Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
                    mContext.startForegroundService(execServiceIntent);
                    mContext.bindService(execServiceIntent, svc, Context.BIND_AUTO_CREATE);
                }
            };
            try {
                dialog.show();
            } catch (Exception e) {
                log.e("App is not running!");
            }
            TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
            messageView.setTextSize(30);
            countDown.start();
        } else {
            Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
            mContext.startForegroundService(execServiceIntent);
            mContext.bindService(execServiceIntent, svc, Context.BIND_AUTO_CREATE);
        }
    }

    private void checkExecution(ViewHolder holder) {
        svc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    log.d("checking");
                    ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                    execService = binder.getServer();
                    DataTrack dt = new DataTrack();
                    String label = labelConfigs.get(holder.getAdapterPosition()).experiment;
                    int stopTime = labelConfigs.get(holder.getAdapterPosition()).stopTime;
                    long labelId = labelConfigs.get(holder.getAdapterPosition()).id;

                    dt.setDeviceLocation(labelConfigs.get(holder.getAdapterPosition()).deviceLocation);
                    dt.setUserId(labelConfigs.get(holder.getAdapterPosition()).userId);
                    dt.setSendFilesToServer(labelConfigs.get(holder.getAdapterPosition()).sendToServer);
                    dt.setActivity(labelConfigs.get(holder.getAdapterPosition()).activity);
                    dt.setConfigId(labelId);
                    dt.setStopTime(stopTime);
                    dt.setLabel(label);
                    dt.addSensorList(sensorFrequencyMap.get(label));
                    if (dt.equals(execService.isRunning())) {
                        log.d("Experiment already running " + dt.getLabel());
                        holder.getEditButton().setEnabled(false);
                        holder.getStartButton().setEnabled(false);
                        holder.getStopButton().setEnabled(true);
                        execService.changeExecutionServiceListener(new MyExecutionListener(dt, holder));
                        //startExecution(holder);
                    } else {
                        sendData(holder, CREATE_CSV_FILE, false, "0");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };

        Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
        mContext.startForegroundService(execServiceIntent);
        mContext.bindService(execServiceIntent, svc, Context.BIND_AUTO_CREATE);
    }

    @Override
    public int getItemCount() {
        if (labelConfigs != null) {
            return labelConfigs.size();
        }
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
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
            stopButton = itemView.findViewById(R.id.stop_sampling_button);
            editButton = itemView.findViewById(R.id.edit_sampling_button);
            shareButton = itemView.findViewById(R.id.share_sampling_button);
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

        public Button getStopButton() {
            return stopButton;
        }

        public Button getEditButton() {
            return editButton;
        }

        public ImageView getShareButton() {
            return shareButton;
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
                    holder.getStartButton().setEnabled(true);
                    holder.getStopButton().setEnabled(true);
                    holder.getExpCard().setCardBackgroundColor(holder.getExpCardColor());
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
                                        /*execService.stopSelf();
                                        execService.stopForeground(true);*/
                        holder.getEditButton().setEnabled(true);
                        holder.getStartButton().setEnabled(true);
                        holder.getStopButton().setEnabled(false);
                        holder.getExpCard().setCardBackgroundColor(holder.getExpCardColor());
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
                                                /*createCSVDialog.setCancelable(true);
                                                createCSVDialog.setMessage("OK");*/
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
                    log.d("MyExecutionListener - disbling buttons");
                    holder.getEditButton().setEnabled(false);
                    holder.getStartButton().setEnabled(false);
                    holder.getStopButton().setEnabled(true);
                    holder.getExpCard().setCardBackgroundColor(ContextCompat.getColor(mContext, R.color.inprogress));
                }
            });
        }
    }
}
