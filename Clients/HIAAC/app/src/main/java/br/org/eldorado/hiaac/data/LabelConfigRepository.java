package br.org.eldorado.hiaac.data;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class LabelConfigRepository {
    private LabelConfigDao mLabelConfigDao;
    private LiveData<List<String>> mAllLabels;

    public LabelConfigRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mLabelConfigDao = db.labelConfigDao();
        mAllLabels = mLabelConfigDao.getAllLabels();
    }

    LiveData<List<String>> getAllLabels() {
        return mAllLabels;
    }

    LiveData<LabelConfig> getLabelConfigById(String id) {
        return mLabelConfigDao.getLabelConfigById(id);
    }

    public void insertNewConfig(LabelConfig config) {
        new insertAsyncTask(mLabelConfigDao).execute(config);
    }

    public void updateConfig(LabelConfig config) {
        new updateAsyncTask(mLabelConfigDao).execute(config);
    }

    public void deleteConfig(LabelConfig config) {
        new deleteAsyncTask(mLabelConfigDao).execute(config);
    }

    private static class insertAsyncTask extends AsyncTask<LabelConfig, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;

        public insertAsyncTask(LabelConfigDao dao) {
            this.mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(LabelConfig... labelConfigs) {
            mAsyncTaskDao.insert(labelConfigs[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<LabelConfig, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;

        public updateAsyncTask(LabelConfigDao dao) {
            this.mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(LabelConfig... labelConfigs) {
            mAsyncTaskDao.update(labelConfigs[0]);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<LabelConfig, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;

        public deleteAsyncTask(LabelConfigDao dao) {
            this.mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(LabelConfig... labelConfigs) {
            mAsyncTaskDao.delete(labelConfigs[0]);
            return null;
        }
    }
}
