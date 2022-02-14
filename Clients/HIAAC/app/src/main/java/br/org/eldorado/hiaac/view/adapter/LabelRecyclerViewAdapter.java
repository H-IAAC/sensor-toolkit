package br.org.eldorado.hiaac.view.adapter;

import static android.content.Context.MEDIA_SESSION_SERVICE;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import br.org.eldorado.hiaac.LabelOptionsActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.data.LabelConfig;
import br.org.eldorado.hiaac.data.SensorFrequency;
import br.org.eldorado.hiaac.firebase.FirebaseListener;
import br.org.eldorado.hiaac.firebase.FirebaseUploadController;
import br.org.eldorado.hiaac.layout.AnimatedLinearLayout;
import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.service.ExecutionService;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListenerAdapter;
import br.org.eldorado.hiaac.util.Log;
import br.org.eldorado.hiaac.util.Tools;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "LabelRecyclerViewAdapter";
    private final LayoutInflater mInflater;
    private List<LabelConfig> labelConfigs;
    private Map<String, List<SensorFrequency>> sensorFrequencyMap;
    private Context mContext;
    private ExecutionService execService;
    private ServiceConnection svc;
    private Log log;

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
        LabelConfig labelConfig = labelConfigs.get(holder.getAdapterPosition());
        String labelTitle = labelConfig.label;

        RecyclerView csvList = holder.getCsvRecyclerView();
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        labelTitle);
        List<File> filesList = new ArrayList<File>();
        if (directory.exists()) {
            filesList = Arrays.asList(directory.listFiles());
        }
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
                    if (filesSize > 0) {
                        holder.getButtonContainer().expand(300);
                    } else {
                        holder.getButtonContainer().expand(60);
                    }
                    holder.setOpened(true);
                }
            }
        });
        holder.getLabelTimer().setText(
                Tools.getFormatedTime(labelConfigs.get(holder.getAdapterPosition()).stopTime).substring(3));

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
                sendData(holder);
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

    private void sendData(ViewHolder holder) {
        final ProgressDialog dialog = ProgressDialog.show(mContext, "Export to CSV File", "Creating CSV", true);
        FirebaseUploadController firebase = new FirebaseUploadController(mContext);
        firebase.registerListener(new FirebaseListener() {

            @Override
            public void onProgress(String message) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (message.equals(mContext.getString(R.string.share_with_firebase))) {

                            AlertDialog.Builder aDialogBuilder = new AlertDialog.Builder(mContext);
                            AlertDialog aDialog = aDialogBuilder.create();
                            aDialog.setTitle(mContext.getString(R.string.share_with_firebase_title));
                            aDialog.setMessage(message);

                            aDialog.setButton(DialogInterface.BUTTON_NEGATIVE, mContext.getString(R.string.no), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInt, int which) {
                                    aDialog.dismiss();
                                    onSendDataCompleted(mContext.getString(R.string.success), dialog);
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

                        } else {
                            dialog.setMessage(message);
                        }
                    }
                });
            }

            @Override
            public void onCompleted(String message) {
                onSendDataCompleted(message, dialog);
            }
        });
        firebase.exportToCSV(labelConfigs.get(holder.getAdapterPosition()).label);
    }

    private void onSendDataCompleted(String message, ProgressDialog dialog) {
        ((Activity)mContext).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                dialog.cancel();
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setTitle(message);
                builder.setIcon(R.drawable.ic_baseline_success);
                AlertDialog dl = builder.create();
                dl.show();
            }
        });
    }

    AlertDialog dialog;
    private void startExecution(ViewHolder holder) {
        svc = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                try {
                    log.d("Connected");
                    ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                    execService = binder.getServer();

                    DataTrack dt = new DataTrack();
                    String label = labelConfigs.get(holder.getAdapterPosition()).label;
                    int stopTime = labelConfigs.get(holder.getAdapterPosition()).stopTime;

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
                                    String labelTimer = Tools.getFormatedTime((int)remainingTime/1000);
                                    holder.getLabelTimer().setText(labelTimer.substring(3));
                                }
                            });
                        }

                        @Override
                        public void onStopped() {
                            // Enable buttons
                            ((Activity)mContext).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    holder.getEditButton().setEnabled(true);
                                    holder.getStartButton().setEnabled(true);
                                    holder.getStopButton().setEnabled(false);
                                    holder.getLabelTimer().setText(
                                            Tools.getFormatedTime(labelConfigs.get(holder.getAdapterPosition()).stopTime).substring(3));
                                }
                            });
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


        AlertDialog.Builder timer = new AlertDialog.Builder(mContext);
        //timer.setTitle();
        dialog = timer.create();
        dialog.setTitle(mContext.getString(R.string.experiment_timer_title));
        dialog.setMessage("\t 10");
        dialog.setCancelable(false);
        CountDownTimer countDown = new CountDownTimer(9000, 1000) {
            @Override
            public void onTick(long timeRemaining) {
                ((Activity)mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.setMessage("\t" + timeRemaining/1000);
                    }
                });
            }
            @Override
            public void onFinish() {
                dialog.dismiss();
                Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
                mContext.startForegroundService(execServiceIntent);
                mContext.bindService(execServiceIntent, svc, Context.BIND_AUTO_CREATE);
            }
        };
        dialog.show();
        TextView messageView = (TextView)dialog.findViewById(android.R.id.message);
        messageView.setGravity(Gravity.CENTER);
        messageView.setTextSize(30);
        countDown.start();
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

                    dt.setStopTime(stopTime);
                    dt.setLabel(label);
                    dt.addSensorList(sensorFrequencyMap.get(label));
                    if (dt.equals(execService.isRunning())) {
                        holder.getEditButton().setEnabled(false);
                        holder.getStartButton().setEnabled(false);
                        holder.getStopButton().setEnabled(true);
                        startExecution(holder);
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
            csvRecyclerView = itemView.findViewById(R.id.csvfiles_reclyclerView);
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
    }
}
