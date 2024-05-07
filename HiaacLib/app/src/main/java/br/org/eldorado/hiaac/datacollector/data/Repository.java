package br.org.eldorado.hiaac.datacollector.data;

import android.app.Application;

public class Repository {
    private static Repository uniqueInstance;
    private static LabelConfigRepository labelConfigRepository;
    private static Application application;

    public Repository(Application application) {
        this.application = application;
    }

    public static synchronized LabelConfigRepository getLabelConfigRepositoryInstance() {
        if (labelConfigRepository == null)
            labelConfigRepository = new LabelConfigRepository(application);

        return labelConfigRepository;
    }

}
