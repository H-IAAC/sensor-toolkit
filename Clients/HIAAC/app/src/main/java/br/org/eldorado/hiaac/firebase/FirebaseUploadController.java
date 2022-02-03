package br.org.eldorado.hiaac.firebase;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import br.org.eldorado.hiaac.R;
import br.org.eldorado.hiaac.controller.ExecutionController;
import br.org.eldorado.hiaac.data.LabelConfigViewModel;
import br.org.eldorado.hiaac.data.LabeledData;
import br.org.eldorado.hiaac.model.DataTrack;
import br.org.eldorado.hiaac.util.Log;

public class FirebaseUploadController {

    private static final String TAG = "FirebaseUploadController";
    private Log log;
    private static final int SUCCESS = 0;
    private static final int ERROR = 1;
    private static final int ON_PROGRESS = 2;

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
                List<LabeledData> labeledData = dbView.getLabeledData(labelName);
                if (labeledData == null || labeledData.size() == 0) {
                    fireListener(ERROR, "No data");
                    return;
                }

                fireListener(ON_PROGRESS, mContext.getString(R.string.creating_csv_file));

                FirebaseStorage storage = FirebaseStorage.getInstance();
                StorageReference csvRef = storage.getReference().child(labelName+ File.separator + "csv");


                /*File f = new File(mContext.getFilesDir().getAbsolutePath()+File.separator+"newfile.txt");
                try {
                    OutputStreamWriter o = new OutputStreamWriter(new FileOutputStream(f));
                    o.write("testando essa bagaca\n".toCharArray());
                    o.write("testando essa bagaca\n".toCharArray());
                    o.write("testando essa bagaca\n".toCharArray());
                    o.write("testando essa bagaca\n".toCharArray());
                    o.write("testando essa bagaca\n".toCharArray());
                    o.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
                File csvFile = createCSVFile(labeledData, labelName);
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
//                        csvFile.delete();
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

    private File createCSVFile(List<LabeledData> data, String labelName) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmm");
        File csvFile = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        labelName + "_" +
                        df.format(new Date(System.currentTimeMillis())) +
                        ".csv");
        try {
            Locale l = Locale.getDefault();
            Locale.setDefault(new Locale("pt", "BR"));
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile), ';');
            writer.writeNext(data.get(0).getCSVHeaders());
            for (LabeledData dt : data) {
                writer.writeNext(dt.getCSVFormattedString());
            }
            writer.close();
            Locale.setDefault(l);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return csvFile;
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
