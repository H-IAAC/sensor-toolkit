package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
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

    LiveData<LabelConfig> getLabelConfigById(long id) {
        return mLabelConfigDao.getLabelConfigById(id);
    }

    LiveData<List<SensorFrequency>> getAllSensorsFromLabel(long id) {
        return mLabelConfigDao.getAllSensorsFromLabel(id);
    }

    void deleteSensorFromLabel(LabelConfig label) {
        new LabelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.deleteSensorFromLabel(label.id);
            return null;})).execute(label);

    }

    LiveData<List<SensorFrequency>> getAllSensorFrequencies() {
        return mLabelConfigDao.getAllSensorFrequencies();
    }

    public Long insertNewConfig(LabelConfig config) throws ExecutionException, InterruptedException {
        //LabelConfigInsertAsyncTask runner = new LabelConfigInsertAsyncTask(mLabelConfigDao, config);
        //runner.execute();

        return new LabelConfigInsertAsyncTask(mLabelConfigDao, (labelConfig -> {
            return mLabelConfigDao.insert(labelConfig);
        })).execute(config).get();
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
            mLabelConfigDao.deleteSensorFromLabel(sensorFrequencies.get(0).getConfigId());
            mLabelConfigDao.insertAllSensorFrequencies(sensorFrequencies);
            return null;})).execute(frequencies);
    }

    public void deleteAllSensorFrequencies(List<SensorFrequency> frequencies) {
        new SensorFrequencyAsyncTask(mLabelConfigDao,
                (sensorFrequencies -> {
            mLabelConfigDao.deleteAllSensorFrequencies(sensorFrequencies);
            return null;})).execute(frequencies);
    }

    public void insertLabeledData(List<LabeledData> data) {
        new LabeledDataAsyncTask(mLabelConfigDao, (labeledData -> {
            mLabelConfigDao.insertLabeledData(labeledData);
            return null;})).execute(data);
    }

    public void insertExperimentStatistics(List<ExperimentStatistics> data) {
        new ExperimentStatisticsAsyncTask(mLabelConfigDao, (statistics -> {
            mLabelConfigDao.insertExperimentStatistics(statistics);
            return null;})).execute(data);
    }

    public LiveData<List<ExperimentStatistics>> getExperimentStatisticsByExpId(long configId, String startTime) {
        return mLabelConfigDao.getStatisticsByExpId(configId, startTime);
    }

    public void deleteExperimentStatistics(long configId) {
        List<ExperimentStatistics> st = new ArrayList<>();
        new ExperimentStatisticsAsyncTask(mLabelConfigDao, (statistics -> {
            mLabelConfigDao.deleteExperimentStatistics(configId);
            return null;})).execute(st);
    }

    public void deleteExperimentStatistics(long configId, String startTime) {
        List<ExperimentStatistics> st = new ArrayList<>();
        new ExperimentStatisticsAsyncTask(mLabelConfigDao, (statistics -> {
            mLabelConfigDao.deleteExperimentStatistics(configId, startTime);
            return null;})).execute(st);
    }

    public List<LabeledData> getLabeledData(long labelId, int type, long offset) {
        if (type == TYPE_FIREBASE) {
            return mLabelConfigDao.getLabeledData(labelId, offset);
        } else {
            return mLabelConfigDao.getLabeledDataCsv(labelId);
        }
    }

    public Integer countLabeledDataCsv(long labelId) {
        return mLabelConfigDao.countLabeledDataCsv(labelId);
    }

    public void updateLabeledData( List<LabeledData> dt) {
        mLabelConfigDao.updateLabeledData(dt);
    }

    void deleteLabeledData(LabeledData label) {
        LinkedList<LabeledData> l = new LinkedList();
        l.add(label);
        new LabeledDataAsyncTask(mLabelConfigDao, (labeledData -> {
            mLabelConfigDao.deleteLabeledData(label.getConfigId());
            return null;})).execute(l);
        //mLabelConfigDao.deleteLabeledData(label);
    }

    void deleteLabeledData(long configId) {
        new LongAsyncTask(mLabelConfigDao, (id -> {
            mLabelConfigDao.deleteLabeledData(id);
            return null;})).execute(configId);
    }

    private static class LongAsyncTask extends AsyncTask<Long, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<Long, Void> mFunction;

        public LongAsyncTask(LabelConfigDao dao, Function<Long, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(Long... id) {
            mFunction.apply(id[0]);
            return null;
        }
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

    private static class LabelConfigInsertAsyncTask extends AsyncTask<LabelConfig, Long, Long> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<LabelConfig, Long> mFunction;

        public LabelConfigInsertAsyncTask(LabelConfigDao dao, Function<LabelConfig, Long> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Long doInBackground(LabelConfig... labelConfigs) {
            return mFunction.apply(labelConfigs[0]);
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

    private static class ExperimentStatisticsAsyncTask extends AsyncTask<List<ExperimentStatistics>, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<List<ExperimentStatistics>, Void> mFunction;

        public ExperimentStatisticsAsyncTask(LabelConfigDao dao, Function<List<ExperimentStatistics>, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(List<ExperimentStatistics>... lists) {
            mFunction.apply(lists[0]);
            return null;
        }
    }
}
