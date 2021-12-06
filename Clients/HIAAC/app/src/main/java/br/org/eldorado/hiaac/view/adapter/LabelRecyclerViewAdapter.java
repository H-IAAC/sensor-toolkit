package br.org.eldorado.hiaac.view.adapter;

import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import br.org.eldorado.hiaac.LabelOptionsActivity;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.controller.ExecutionController;
import br.org.eldorado.hiaac.layout.AnimatedLinearLayout;
import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.service.ExecutionService;
import br.org.eldorado.hiaac.service.listener.ExecutionServiceListener;
import br.org.eldorado.hiaac.util.Log;
import br.org.eldorado.sensoragent.model.Accelerometer;
import br.org.eldorado.sensoragent.model.Gyroscope;
import br.org.eldorado.sensoragent.model.ISensorAgent;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "LabelRecyclerViewAdapter";
    private final LayoutInflater mInflater;
    private List<String> labelConfigs;
    private Context mContext;
    private ExecutionService execService;
    private Log log;

    public LabelRecyclerViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        log = new Log(TAG);
    }

    public void setLabelConfigs(List<String> labels) {
        this.labelConfigs = labels;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.label_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        String labelTitle = labelConfigs.get(position);
        holder.getLabelTitle().setText(labelTitle);
        holder.getLabelTitle().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.isOpened) {
                    holder.getButtonContainer().close();
                    holder.setOpened(false);
                } else {
                    holder.getButtonContainer().expand(60);
                    holder.setOpened(true);
                }
            }
        });

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
                ServiceConnection svc = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        try {
                            log.d("Connected");
                            ExecutionService.MyBinder binder = (ExecutionService.MyBinder) service;
                            execService = binder.getServer();

                            DataTrack dt = new DataTrack();
                            dt.addSensor(new Accelerometer());
                            dt.addSensor(new Gyroscope());
                            execService.startExecution(dt, new ExecutionServiceListener() {
                                @Override
                                public void onRunning(long remainingTime) {
                                    // update clock ui
                                }

                                @Override
                                public void onStopped() {
                                    // Enable buttons
                                    holder.getEditButton().setEnabled(true);
                                    holder.getStartButton().setEnabled(true);
                                    holder.getStopButton().setEnabled(false);
                                }

                                @Override
                                public void onStarted() {
                                    // Disable buttons
                                    holder.getEditButton().setEnabled(false);
                                    holder.getStartButton().setEnabled(false);
                                    holder.getStopButton().setEnabled(true);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                    }
                };

                Intent execServiceIntent = new Intent(mContext, ExecutionService.class);
                mContext.startForegroundService(execServiceIntent);
                mContext.bindService(execServiceIntent, svc, Context.BIND_AUTO_CREATE);
            }
        });
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
        private AnimatedLinearLayout buttonContainer;
        private Button startButton;
        private Button stopButton;
        private Button editButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            isOpened = false;
            labelTitle = (TextView) itemView.findViewById(R.id.label_title);
            buttonContainer = (AnimatedLinearLayout) itemView.findViewById(R.id.label_button_container);
            startButton = (Button) itemView.findViewById(R.id.start_sampling_button);
            stopButton = (Button) itemView.findViewById(R.id.stop_sampling_button);
            editButton = (Button) itemView.findViewById(R.id.edit_sampling_button);
        }

        public void setOpened(boolean opened) {
            isOpened = opened;
        }

        public TextView getLabelTitle() {
            return labelTitle;
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
    }
}
