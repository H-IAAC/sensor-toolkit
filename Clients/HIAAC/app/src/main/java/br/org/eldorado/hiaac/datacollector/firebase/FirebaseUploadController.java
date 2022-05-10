package br.org.eldorado.hiaac.datacollector.firebase;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opencsv.CSVWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigRepository;
import br.org.eldorado.hiaac.datacollector.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.datacollector.data.LabeledData;
import br.org.eldorado.hiaac.datacollector.util.Log;

public class FirebaseUploadController {

    private static final String TAG = "FirebaseUploadController";
    private Log log;
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;
    private static final int ON_PROGRESS = 2;
    private static final int TYPE_FIREBASE = 3;
    private static final int TYPE_CSV = 4;

    private Context mContext;
    private FirebaseListener listener;
    private LabelConfigViewModel dbView;

    public FirebaseUploadController(Context ctx) {
        log = new Log(TAG);
        mContext = ctx;
        dbView = ViewModelProvider.AndroidViewModelFactory.getInstance(
                (Application) ctx.getApplicationContext()).create(LabelConfigViewModel.class);
    }

    public void registerListener(FirebaseListener l) {
        this.listener = l;
    }

    public void uploadCSVFile(String labelName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /* Create the CSV file if there are data for that and upload to firebase
                 *  If the upload is successful, delete the data from database */
                List<LabeledData> labeledData = dbView.getLabeledData(labelName, LabelConfigRepository.TYPE_FIREBASE);
                if (labeledData == null || labeledData.size() == 0) {
                    fireListener(ERROR, "No data");
                    return;
                }

                fireListener(ON_PROGRESS, mContext.getString(R.string.creating_csv_file));

                FirebaseStorage storage = FirebaseStorage.getInstance();
                storage.setMaxUploadRetryTimeMillis(2000);
                StorageReference csvRef = storage.getReference().child(labelName+ File.separator + "csv");

                File csvFile = createCSVFile(labeledData);
                Uri file = Uri.fromFile(csvFile);
                fireListener(ON_PROGRESS, mContext.getString(R.string.starting_upload_file));

                UploadTask uploadTask = csvRef.child(file.getLastPathSegment()).putFile(file);

                uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        log.d("Progress " + progress);
                        fireListener(ON_PROGRESS, mContext.getString(R.string.download_progress, (int)progress));
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        log.d("onSuccess ");
                        csvFile.delete();
                        dbView.deleteLabeledData(labeledData.get(0));
                        fireListener(SUCCESS, null);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        log.d("onFailure " + e);
                        csvFile.delete();
                        fireListener(ERROR, e.getMessage());
                    }
                });
            }
        }).start();
    }

    public void exportToCSV(String label) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<LabeledData> labeledData = dbView.getLabeledData(label, LabelConfigRepository.TYPE_CSV);
                if (labeledData == null || labeledData.size() == 0) {
                    fireListener(ERROR, "No data");
                    return;
                }
                fireListener(ON_PROGRESS, mContext.getString(R.string.creating_csv_file));
                File csvFile = createCSVFile(labeledData);
                fireListener(ON_PROGRESS, mContext.getString(R.string.share_with_firebase));
            }
        }).start();
    }

    private File createCSVFile(List<LabeledData> data) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                File.separator +
                data.get(0).getLabel());
        if (!directory.exists()) {
            directory.mkdir();
        }

        File csvFile = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        data.get(0).getLabel() +
                        File.separator +
                        data.get(0).getLabel() + "_" +
                        df.format(new Date(System.currentTimeMillis())) +
                        ".csv");
        try {
            Locale l = Locale.getDefault();
            Locale.setDefault(new Locale("pt", "BR"));
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile), ';');
            writer.writeNext(data.get(0).getCSVHeaders());
            for (LabeledData dt : data) {
                writer.writeNext(dt.getCSVFormattedString());
                dt.setIsDataUsed(1);
            }
            writer.close();
            Locale.setDefault(l);
            dbView.updateLabeledData(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvFile;
    }

    private void getLabeledData() {

    }

    private void fireListener(int type, String msg) {
        if (listener != null) {
            switch (type) {
                case SUCCESS:
                    listener.onCompleted(mContext.getString(R.string.success));
                    break;
                case ERROR:
                    listener.onCompleted(msg);
                    break;
                case ON_PROGRESS:
                    listener.onProgress(msg);
                    break;
            }
        }
    }
}
