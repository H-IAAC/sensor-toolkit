package br.org.eldorado.hiaac.view.adapter;

import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_ID;
import static br.org.eldorado.hiaac.MainActivity.LABEL_CONFIG_ACTIVITY_TYPE;
import static br.org.eldorado.hiaac.MainActivity.UPDATE_LABEL_CONFIG_ACTIVITY;

import android.content.Context;
import android.content.Intent;
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
import br.org.eldorado.hiaac.layout.AnimatedLinearLayout;

public class LabelRecyclerViewAdapter extends RecyclerView.Adapter<LabelRecyclerViewAdapter.ViewHolder> {
    private final LayoutInflater mInflater;
    private List<String> labelConfigs;

    public LabelRecyclerViewAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
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
