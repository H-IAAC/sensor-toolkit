package br.org.eldorado.hiaac.data;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.function.Function;

public class LabelConfigRepository {
    private LabelConfigDao mLabelConfigDao;
    private LiveData<List<LabelConfig>> mAllLabels;

    public LabelConfigRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mLabelConfigDao = db.labelConfigDao();
        mAllLabels = mLabelConfigDao.getAll();
    }

    LiveData<List<LabelConfig>> getAllLabels() {
        return mAllLabels;
    }

    LiveData<LabelConfig> getLabelConfigById(String id) {
        return mLabelConfigDao.getLabelConfigById(id);
    }

    public void insertNewConfig(LabelConfig config) {
        new labelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.insert(labelConfig);
            return null;})).execute(config);
    }

    public void updateConfig(LabelConfig config) {
        new labelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.update(labelConfig);
            return null;})).execute(config);
    }

    public void deleteConfig(LabelConfig config) {
        new labelConfigAsyncTask(mLabelConfigDao, (labelConfig -> {
            mLabelConfigDao.delete(labelConfig);
            return null;})).execute(config);
    }

    private static class labelConfigAsyncTask extends AsyncTask<LabelConfig, Void, Void> {

        private LabelConfigDao mAsyncTaskDao;
        private Function<LabelConfig, Void> mFunction;

        public labelConfigAsyncTask(LabelConfigDao dao, Function<LabelConfig, Void> function) {
            this.mAsyncTaskDao = dao;
            this.mFunction = function;
        }

        @Override
        protected Void doInBackground(LabelConfig... labelConfigs) {
            mFunction.apply(labelConfigs[0]);
            return null;
        }
    }
}
