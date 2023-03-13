package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class LabelConfigRepository {

    public static final int TYPE_FIREBASE = 0;
    public static final int TYPE_CSV = 1;

    private LabelConfigDao mLabelConfigDao;
    private LiveData<List<LabelConfig>> mAllLabels;

    public LabelConfigRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mLabelConfigDao = db.labelConfigDao();
        mAllLabels = mLabelConfigDao.getAllLabels();
    }

    LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    LiveData<LabelConfig> getLabelConfigById(String id) {
        return mLabelConfigDao.getLabelConfigById(id);
    }

    LiveData<List<SensorFrequency>> getAllSensorsFromLabel(String label) {
        return mLabelConfigDao.getAllSensorsFromLabel(label);
    }

    LiveData<List<SensorFrequency>> getAllSensorFrequencies() {
        return mLabelConfigDao.getAllSensorFrequencies();
    }

    public void insertNewConfig(LabelConfig config) {
        new LabelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.insert(labelConfig);
            return null;})).execute(config);
    }

    public void updateConfig(LabelConfig config) {
        new LabelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.update(labelConfig);
            return null;})).execute(config);
    }

    public void deleteConfig(LabelConfig config) {
        new LabelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.delete(labelConfig);
            return null;})).execute(config);
    }

    public void insertAllSensorFrequencies(List<SensorFrequency> frequencies) {
        new SensorFrequencyAsyncTask(mLabelConfigDao, (sensorFrequencies -> {
            mLabelConfigDao.insertAllSensorFrequencies(sensorFrequencies);
            return null;})).execute(frequencies);
    }

    public void deleteAllSensorFrequencies(List<SensorFrequency> frequencies) {
        new SensorFrequencyAsyncTask(mLabelConfigDao, (sensorFrequencies -> {
            mLabelConfigDao.deleteAllSensorFrequencies(sensorFrequencies);
            return null;})).execute(frequencies);
    }

    public void insertLabeledData(List<LabeledData> data) {
        new LabeledDataAsyncTask(mLabelConfigDao, (labeledData -> {
            mLabelConfigDao.insertLabeledData(labeledData);
            return null;})).execute(data);
    }

    public List<LabeledData> getLabeledData(int labelId, int type, long offset) {
        if (type == TYPE_FIREBASE) {
            return mLabelConfigDao.getLabeledData(labelId, offset);
        } else {
            return mLabelConfigDao.getLabeledDataCsv(labelId);
        }
    }


    public void updateLabeledData( List<LabeledData> dt) {
        mLabelConfigDao.updateLabeledData(dt);
    }

    void deleteLabeledData(LabeledData label) {
        LinkedList<LabeledData> l = new LinkedList();
        l.add(label);
        new LabeledDataAsyncTask(mLabelConfigDao, (labeledData -> {
            mLabelConfigDao.deleteLabeledData(label.getLabel());
            return null;})).execute(l);
        //mLabelConfigDao.deleteLabeledData(label);
    }

    private static class LabelConfigAsyncTask extends AsyncTask<LabelConfig, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<LabelConfig, Void> mFunction;

        public LabelConfigAsyncTask(LabelConfigDao dao, Function<LabelConfig, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(LabelConfig... labelConfigs) {
            mFunction.apply(labelConfigs[0]);
            return null;
        }
    }

    private static class SensorFrequencyAsyncTask extends AsyncTask<List<SensorFrequency>, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<List<SensorFrequency>, Void> mFunction;

        public SensorFrequencyAsyncTask(LabelConfigDao dao, Function<List<SensorFrequency>, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(List<SensorFrequency>... lists) {
            mFunction.apply(lists[0]);
            return null;
        }
    }

    private static class LabeledDataAsyncTask extends AsyncTask<List<LabeledData>, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<List<LabeledData>, Void> mFunction;

        public LabeledDataAsyncTask(LabelConfigDao dao, Function<List<LabeledData>, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(List<LabeledData>... lists) {
            mFunction.apply(lists[0]);
            return null;
        }
    }
}
