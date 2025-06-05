package br.org.eldorado.hiaac.datacollector.view.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import java.io.File;
import java.util.List;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.StatisticsActivity;
import br.org.eldorado.hiaac.datacollector.data.ExperimentStatistics;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigRepository;
import br.org.eldorado.hiaac.datacollector.util.CsvFiles;
import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.datacollector.util.Tools;

public class CSVFilesRecyclerAdapter extends RecyclerView.Adapter<CSVFilesRecyclerAdapter.ViewHolder> {
    private final LayoutInflater mInflater;
    private final List<File> csvFileList;
    private final Context mContext;
    private final Log log = new Log("CSVFilesRecyclerAdapter");
    private final long configId;
    private final LabelConfigRepository mRepository;

    public CSVFilesRecyclerAdapter(Context context, List<File> f, LabelConfigRepository repository, long id) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        csvFileList = f;
        mRepository = repository;
        configId = id;
    }

    public void updateFileList(List<File> list) {
        this.csvFileList.clear();
        this.csvFileList.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        log.d("onCreateViewHolder");
        View view = mInflater.inflate(R.layout.csv_file_item, parent, false);
        return new CSVFilesRecyclerAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        File csvFile = csvFileList.get(holder.getAdapterPosition());
        TextView fileName = holder.getFileNameTxt();
        fileName.setText(csvFile.getName());

        CardView csvFilePnl = holder.getCsvFile();
        csvFilePnl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Uri uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", csvFile);
                    String mime = mContext.getContentResolver().getType(uri);

                    // Open file with user selected app
                    Intent intent = new Intent();
                    //intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setDataAndType(uri, mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    //mContext.startActivity(intent);
                    mContext.startActivity(Intent.createChooser(intent, "Share File"));
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(mContext, "No application found to open the file", Toast.LENGTH_LONG).show();
                }
            }
        });

        // Delete file
        holder.getDeleteBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog dialog = new AlertDialog.Builder(mContext)
                        .setTitle(mContext.getString(R.string.delete_experiment_file_title))
                        .setMessage(mContext.getString(R.string.delete_experiment_file, csvFile.getName()))
                        .setPositiveButton(mContext.getString(R.string.button_ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteFile(holder, csvFile);
                            }
                        }).setNegativeButton(mContext.getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                dialog.show();
            }
        });

        // Statistics are present only for CSV file
        if ("csv".equals(Tools.getFileExtension(csvFile.getName()))) {
            // Handle statistics click
            holder.getStatisticsBtn().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String clickedFileName = csvFileList.get(holder.getAdapterPosition()).getName();
                    final CsvFiles.CsvFileName csvFileName = CsvFiles.decomposeFileName(clickedFileName);
                    String startTimeEpoch = CsvFiles.CsvFileNameConvertTimestamp(csvFileName.startTime);

                    List<ExperimentStatistics> statistics = mRepository.getExperimentStatistics(configId, startTimeEpoch.substring(0, 9) + '%');

                    if (statistics != null && statistics.size() > 0) {
                        Intent intent = new Intent(mContext, StatisticsActivity.class);
                        intent.putExtra("statistics", new Gson().toJson(statistics));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(intent);
                    }
                }
            });
        } else {
            // Hide statistics button for non-csv files.
            holder.getStatisticsBtn().setVisibility(View.INVISIBLE);
        }
    }

    private void deleteFile(ViewHolder holder, File csvFile) {
        String clickedFileName = csvFileList.get(holder.getAdapterPosition()).getName();

        // If needs to delete a .csv file, then data from db must be removed also.
        if ("csv".equals(Tools.getFileExtension(clickedFileName))) {
            CsvFiles.CsvFileName csvFileName = CsvFiles.decomposeFileName(clickedFileName);
            String startTimeEpoch = CsvFiles.CsvFileNameConvertTimestamp(csvFileName.startTime);
            mRepository.deleteExperimentStatistics(configId, startTimeEpoch.substring(0, 9) + '%');
        }

        csvFileList.remove(holder.getAdapterPosition());
        csvFile.delete();

        notifyItemRemoved(holder.getAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return csvFileList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ImageView fileImg;
        private ImageView deleteImg;
        private ImageView statisticsImg;
        private TextView fileName;
        private CardView csvFile;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            deleteImg = (ImageView) itemView.findViewById(R.id.delete_csv_file);
            fileName = (TextView) itemView.findViewById(R.id.csv_file_txt);
            csvFile = (CardView) itemView.findViewById(R.id.csv_file_pnel);
            statisticsImg = (ImageView) itemView.findViewById(R.id.statistics_file);
        }

        public CardView getCsvFile() {
            return csvFile;
        }

        public TextView getFileNameTxt() {
            return fileName;
        }

        public ImageView getDeleteBtn() {
            return deleteImg;
        }

        public ImageView getStatisticsBtn() {
            return statisticsImg;
        }
    }
}
