package br.org.eldorado.hiaac.datacollector.firebase;

import static br.org.eldorado.hiaac.datacollector.DataCollectorActivity.FOLDER_NAME;

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
import java.io.IOException;
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

    public void uploadCSVFile(String labelName, int labelId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                /* Create the CSV file if there are data for that and upload to firebase
                 *  If the upload is successful, delete the data from database */
                List<LabeledData> labeledData = dbView.getLabeledData(labelId, LabelConfigRepository.TYPE_FIREBASE, 0);
                if (labeledData == null || labeledData.size() == 0) {
                    fireListener(ERROR, mContext.getString(R.string.error_no_data_upload_firebase));
                    return;
                }

                LabeledData toBeDeleted = labeledData.get(0);
                fireListener(ON_PROGRESS, mContext.getString(R.string.creating_csv_file));

                FirebaseStorage storage = FirebaseStorage.getInstance();
                storage.setMaxUploadRetryTimeMillis(2000);
                StorageReference csvRef = storage.getReference().child(labelName+ File.separator + "csv");

                File csvFile = createCSVFile(labeledData);
                labeledData.clear();
                long offset = labeledData.size();
                while ((labeledData = dbView.getLabeledData(labelId, LabelConfigRepository.TYPE_FIREBASE, offset)).size() > 0) {
                    appendDataToCsvFile(csvFile, labeledData, 1);
                    offset += labeledData.size();
                    labeledData.clear();
                }

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
                        dbView.deleteLabeledData(toBeDeleted);
                        fireListener(SUCCESS, mContext.getString(R.string.success_csv_uploaded_firebase));
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

    public void uploadFile(File file, String firebasePath) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                FirebaseStorage storage = FirebaseStorage.getInstance();
                storage.setMaxUploadRetryTimeMillis(2000);
                StorageReference csvRef = storage.getReference().child(firebasePath);

                Uri uriFile = Uri.fromFile(file);
                fireListener(ON_PROGRESS, mContext.getString(R.string.starting_upload_file));

                UploadTask uploadTask = csvRef.child(uriFile.getLastPathSegment()).putFile(uriFile);

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
                        fireListener(SUCCESS, mContext.getString(R.string.success_csv_uploaded_firebase));
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        log.d("onFailure " + e);
                        fireListener(ERROR, e.getMessage());
                    }
                });
            }
        }).start();
    }

    public void exportToCSV(String label, int labelId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                log.d("Starting creating csv file");
                List<LabeledData> labeledData = dbView.getLabeledData(labelId, LabelConfigRepository.TYPE_CSV, 0);
                if (labeledData == null || labeledData.size() == 0) {
                    fireListener(ERROR, mContext.getString(R.string.error_no_data_create_csc));
                    return;
                }
                fireListener(ON_PROGRESS, mContext.getString(R.string.creating_csv_file));
                File csvFile = createCSVFile(labeledData);
                labeledData.clear();
                int index = 0;
                while ((labeledData = dbView.getLabeledData(labelId, LabelConfigRepository.TYPE_CSV, 0)).size() > 0) {
                    appendDataToCsvFile(csvFile, labeledData, 1);
                    labeledData.clear();
                }
                long end = System.currentTimeMillis();
                log.d("Csv file created. Time consumed: " + ((end-start)/1000)/60 + "m" + ((end-start)/1000)%60+"s");
                fireListener(SUCCESS, mContext.getString(R.string.success_csv_file));
            }
        }).start();
    }

    private void appendDataToCsvFile(File csvFile, List<LabeledData> data, int type) {
        try {
            log.d("Appending data " + data.get(0).getCSVFormattedString()[1] + " - " + data.get(0).getCSVFormattedString()[3] + " " + data.size());
            CSVWriter writer = null;
            Locale l = Locale.getDefault();
            try {
                Locale.setDefault(new Locale("pt", "BR"));
                writer = new CSVWriter(new FileWriter(csvFile, true),
                                      ';',
                                       CSVWriter.NO_QUOTE_CHARACTER,
                                       CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                                       CSVWriter.DEFAULT_LINE_END);
                if (type == 0) {
                    writer.writeNext(data.get(0).getCSVHeaders());
                }
                for (LabeledData dt : data) {
                    writer.writeNext(dt.getCSVFormattedString());
                    dt.setIsDataUsed(1);
                }
                dbView.updateLabeledData(data);
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        Locale.setDefault(l);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File createCSVFile(List<LabeledData> data) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd.HHmmss");
        File directory = new File(
                mContext.getFilesDir().getAbsolutePath() +
                File.separator +
                FOLDER_NAME +
                File.separator +
                data.get(0).getLabel());
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File csvFile = new File(
                mContext.getFilesDir().getAbsolutePath() +
                        File.separator +
                        FOLDER_NAME +
                        File.separator +
                        data.get(0).getLabel() +
                        File.separator +
                        data.get(0).getUserId() + "_" +
                        data.get(0).getLabel() + "_" +
                        data.get(0).getActivity() + "_" +
                        data.get(0).getDevicePosition() + "__" +
                        df.format(new Date(data.get(0).getTimestamp())) +
                        //df.format(new Date(System.currentTimeMillis())) +
                        ".csv");
        appendDataToCsvFile(csvFile, data, 0);
        return csvFile;
    }

    private void getLabeledData() {

    }

    private void fireListener(int type, String msg) {
        if (listener != null) {
            switch (type) {
                case SUCCESS:
                    listener.onCompleted(msg);
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
