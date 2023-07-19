package br.org.eldorado.hiaac.datacollector.view.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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

public class CSVFilesRecyclerAdapter extends RecyclerView.Adapter<CSVFilesRecyclerAdapter.ViewHolder> {

    private static final String TAG = "CSVFilesRecyclerAdapter";
    private final LayoutInflater mInflater;
    private List<File> csvFileList;
    private Context mContext;
    private Log log;
    private long configId;

    private LabelConfigRepository mRepository;

    public CSVFilesRecyclerAdapter(Context context, List<File> f, LabelConfigRepository repository, long id) {
        mInflater = LayoutInflater.from(context);
        mContext = context;
        csvFileList = f;
        log = new Log(TAG);
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

        holder.getDeleteBtn().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                csvFileList.remove(holder.getAdapterPosition());
                csvFile.delete();

                String clickedFileName = csvFileList.get(holder.getAdapterPosition()).getName();
                CsvFiles.CsvFileName csvFileName = CsvFiles.decomposeFileName(clickedFileName);
                String startTimeEpoch = CsvFiles.CsvFileNameConvertTimestamp(csvFileName.startTime);
                mRepository.deleteExperimentStatistics(configId, startTimeEpoch.substring(0, 9) + '%');

                notifyItemRemoved(holder.getAdapterPosition());
            }
        });

        holder.getStatisticsBtn().setOnClickListener(new View.OnClickListener() {
            String clickedFileName = csvFileList.get(holder.getAdapterPosition()).getName();
            CsvFiles.CsvFileName csvFileName = CsvFiles.decomposeFileName(clickedFileName);
            String startTimeEpoch = CsvFiles.CsvFileNameConvertTimestamp(csvFileName.startTime);

             @Override
             public void onClick(View v) {
                 // startTime parameter is used to match a db field that contains its value, as startTime for different sensors may vary
                 // here is consider only the first 10 chars, the addictional '%' is used as SQL wildcards.
                 mRepository.getExperimentStatisticsByExpId(configId, startTimeEpoch.substring(0, 9) + '%').observe((LifecycleOwner)mContext,
                         new Observer<List<ExperimentStatistics>>() {
                             @Override
                             public void onChanged(List<ExperimentStatistics> statistics) {
                                 if (statistics != null && statistics.size() > 0) {
                                     Intent intent = new Intent(mContext, StatisticsActivity.class);
                                     intent.putExtra("statistics", new Gson().toJson(statistics));
                                     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                     mContext.startActivity(intent);
                                 }
                             }
                         });
             }
        });
    }

    @Override
    public int getItemCount() {
        return csvFileList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

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
