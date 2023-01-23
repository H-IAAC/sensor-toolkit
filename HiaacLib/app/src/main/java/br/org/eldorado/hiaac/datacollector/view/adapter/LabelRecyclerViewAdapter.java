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
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Tools;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {

    private final int SEND_DATA_TO_FIREBASE = 0;
    private final int CREATE_CSV_FILE = 1;

    private static final String TAG = "LabelRecyclerViewAdapter";
    private final LayoutInflater mInflater;
    private List<LabelConfig> labelConfigs;
    private Map<String, List<SensorFrequency>> sensorFrequencyMap;
    private Context mContext;
    private ExecutionService execService;
    private ServiceConnection svc;
    private Log log;
    private ProgressDialog sendDataDialog;
    private LabelConfigViewModel mLabelConfigViewModel;

    public LabelRecyclerViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        log = new Log(TAG);
    }

    public void setLabelConfigs(List<LabelConfig> labels) {
        this.labelConfigs = labels;
        notifyDataSetChanged();
    }

    public void setSensorFrequencyMap(Map<String, List<SensorFrequency>> sensorFrequencyMap) {
        this.sensorFrequencyMap = sensorFrequencyMap;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.label_item, parent, false);
        return new ViewHolder(view);
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        log.d("CSVFilesRecyclerAdapter - ActiveThreads: " + Thread.activeCount());
        LabelConfig labelConfig = labelConfigs.get(holder.getAdapterPosition());
        String labelTitle = labelConfig.label;

        mLabelConfigViewModel = ViewModelProvider.AndroidViewModelFactory
                .getInstance((Application)mContext.getApplicationContext()).create(LabelConfigViewModel.class);

        RecyclerView csvList = holder.getCsvRecyclerView();
        List<File> filesList = getCsvFiles(labelConfig.label);
        final int filesSize = filesList.size();
        csvList.setAdapter(new CSVFilesRecyclerAdapter(mContext, filesList));
        csvList.setLayoutManager(new LinearLayoutManager(mContext));

        holder.getLabelTitle().setText(labelTitle);
        holder.getLabelTitle().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.isOpened) {
                    holder.getButtonContainer().close();
                    holder.setOpened(false);
                } else {
                    resizeLabelPanel(holder);
                    holder.setOpened(true);
                }
            }
        });
        holder.getLabelTimer().setText(
                Tools.getFormatedTime(labelConfigs.get(holder.getAdapterPosition()).stopTime, Tools.CHRONOMETER));

        Button editButton = holder.getEditButton();
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(editButton.getContext(), LabelOptionsActivity.class);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_TYPE, UPDATE_LABEL_CONFIG_ACTIVITY);
                intent.putExtra(LABEL_CONFIG_ACTIVITY_ID, labelTitle);
                editButton.getContext().startActivity(intent);
            }
        });

        ImageView shareButton = holder.getShareButton();
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData(holder, SEND_DATA_TO_FIREBASE,true);
            }
        });

        ImageView deleteButton = holder.getDeleteButton();
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
                        aDialog.dismiss();
                        mLabelConfigViewModel.getLabelConfigById(labelTitle)
                                .observe((LifecycleOwner) mContext, new Observer<LabelConfig>() {
                                    @Override
                                    public void onChanged(LabelConfig labelConfig) {
                                        try {
                                            if (labelConfig != null) {
                                                mLabelConfigViewModel.deleteConfig(labelConfig);
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
    }

    private void resizeLabelPanel(ViewHolder holder) {
        if (((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).getItemCount() > 0) {
            holder.getButtonContainer().expand(300);
        } else {
            holder.getButtonContainer().expand(54);
        }
    }

    private List<File> getCsvFiles(String label) {
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        label);
        List<File> filesList = new ArrayList<File>();
        if (directory.exists()) {
            filesList = new ArrayList<>(Arrays.asList(directory.listFiles()));
        }
        return filesList;
    }


    private void sendData(ViewHolder holder, int type, boolean showDialog) {
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
            }
        });

        if (type == CREATE_CSV_FILE) {
            firebase.exportToCSV(labelConfigs.get(holder.getAdapterPosition()).label);
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
                    ((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).updateFileList(getCsvFiles(labelConfigs.get(holder.getAdapterPosition()).label));
                    resizeLabelPanel(holder);
                }
            });
            aDialog.setButton(DialogInterface.BUTTON_POSITIVE, mContext.getString(R.string.yes), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInt, int which) {
                    aDialog.dismiss();
                    firebase.uploadCSVFile(labelConfigs.get(holder.getAdapterPosition()).label);
                }
            });
            aDialog.show();
        }
    }

    private void onSendDataCompleted(String message, ProgressDialog dialog, ViewHolder holder) {
        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null) {
                    dialog.cancel();
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle(message);
                    builder.setIcon(R.drawable.ic_baseline_success);
                    AlertDialog dl = builder.create();
                    dl.show();
                }
                List<File> files = getCsvFiles(labelConfigs.get(holder.getAdapterPosition()).label);
                ((CSVFilesRecyclerAdapter)holder.getCsvRecyclerView().getAdapter()).updateFileList(files);
                if (dialog != null) {
                    resizeLabelPanel(holder);
                }


                /** TEST */

                if (labelConfigs.get(holder.getAdapterPosition()).sendToServer) {
                    sendFilesToServer(files, holder);
                }
            }
        });
    }

    private void sendFilesToServer(List<File> files, ViewHolder holder) {
        log.d("sendFilesToServer - " + files);
        MultipartBody.Part experimentPart =
                MultipartBody.Part.createFormData("experiment", labelConfigs.get(holder.getAdapterPosition()).label);
        MultipartBody.Part namePart =
                MultipartBody.Part.createFormData("name", labelConfigs.get(holder.getAdapterPosition()).activity);
        MultipartBody.Part subjectPart =
                MultipartBody.Part.createFormData("subject", labelConfigs.get(holder.getAdapterPosition()).userId);
        files.forEach((file) -> {
            MultipartBody.Part filePart = filePart = MultipartBody.Part.createFormData(
                    "file", file.getName(),
                    RequestBody.create(MediaType.parse("multipart/form-data"), file));

            ClientAPI apiClient = new ClientAPI();
            ApiInterface apiInterface = apiClient.getClient(Tools.SERVER_HOST, Tools.SERVER_PORT).create(ApiInterface.class);
            Call<StatusResponse> call = apiInterface.uploadFile(filePart, experimentPart, subjectPart, namePart);
            call.enqueue(uploadCallback());
        });
    }

    private Callback<StatusResponse> uploadCallback() {
        return new Callback<StatusResponse>() {
            @Override
            public void onResponse(Call<StatusResponse> call, Response<StatusResponse> response) {
                log.d("RESPONSE BODY " + response.body());
                log.d("RESPONSE FULL " + response.raw());
            }

            @Override
            public void onFailure(Call<StatusResponse> call, Throwable t) {
                t.printStackTrace();
                log.d("FAIL " + t);
                call.cancel();
            }
        };
    }

    private void sendToFirebase(ViewHolder holder) {
        sendData(holder, SEND_DATA_TO_FIREBASE, true);
    }

    private AlertDialog dialog;
    private void startExecution(ViewHolder holder) {
        svc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    log.d("Connected execService");
                    ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                    execService = binder.getServer();
                    DataTrack dt = new DataTrack();
                    String label = labelConfigs.get(holder.getAdapterPosition()).label;
                    int stopTime = labelConfigs.get(holder.getAdapterPosition()).stopTime;

                    dt.setDeviceLocation(labelConfigs.get(holder.getAdapterPosition()).deviceLocation);
                    dt.setUserId(labelConfigs.get(holder.getAdapterPosition()).userId);
                    dt.setSendFilesToServer(labelConfigs.get(holder.getAdapterPosition()).sendToServer);
                    dt.setActivity(labelConfigs.get(holder.getAdapterPosition()).activity);
                    dt.setStopTime(stopTime);
                    dt.setLabel(label);
                    dt.addSensorList(sensorFrequencyMap.get(label));

                    execService.startExecution(new ExecutionServiceListenerAdapter(dt) {
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
                                                sendData(holder, CREATE_CSV_FILE, true);
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
                                                sendData(holder, CREATE_CSV_FILE, true);
                                            }
                                        };
                                        createCSVDialog.show();
                                        TextView messageView = (TextView) createCSVDialog.findViewById(android.R.id.message);
                                        messageView.setGravity(Gravity.CENTER);
                                        messageView.setTextSize(26);
                                        countDown.start();
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
                                    holder.getEditButton().setEnabled(false);
                                    holder.getStartButton().setEnabled(false);
                                    holder.getStopButton().setEnabled(true);
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };

        holder.getStartButton().setEnabled(false);

        execService.setRemoteTime(System.currentTimeMillis() + (1000*60*60));
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
                    String label = labelConfigs.get(holder.getAdapterPosition()).label;
                    int stopTime = labelConfigs.get(holder.getAdapterPosition()).stopTime;

                    dt.setDeviceLocation(labelConfigs.get(holder.getAdapterPosition()).deviceLocation);
                    dt.setUserId(labelConfigs.get(holder.getAdapterPosition()).userId);
                    dt.setSendFilesToServer(labelConfigs.get(holder.getAdapterPosition()).sendToServer);
                    dt.setActivity(labelConfigs.get(holder.getAdapterPosition()).activity);
                    dt.setStopTime(stopTime);
                    dt.setLabel(label);
                    dt.addSensorList(sensorFrequencyMap.get(label));
                    if (dt.equals(execService.isRunning())) {
                        holder.getEditButton().setEnabled(false);
                        holder.getStartButton().setEnabled(false);
                        holder.getStopButton().setEnabled(true);
                        startExecution(holder);
                    } else {
                        sendData(holder, CREATE_CSV_FILE, false);
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

        private TextView labelTitle;
        private TextView labelTimer;
        private AnimatedLinearLayout buttonContainer;
        private Button startButton;
        private Button stopButton;
        private Button editButton;
        private ImageView shareButton;
        private ImageView deleteButton;
        private RecyclerView csvRecyclerView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            isOpened = false;
            labelTitle = itemView.findViewById(R.id.label_title);
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

        public void setOpened(boolean opened) {
            isOpened = opened;
        }

        public TextView getLabelTitle() {
            return labelTitle;
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
}
