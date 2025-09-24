package br.org.eldorado.hiaac.datacollector.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import br.org.eldorado.hiaac.datacollector.util.Log;
import br.org.eldorado.hiaac.poc.ui.poc2.service.IAidlInferenceService;

public class InferenceService {

    private final Log log = new Log("InferenceService");
    private IAidlInferenceService mService;
    private final Context mContext;
    private boolean bound = false;

    public InferenceService(Context ctx) {
        this.mContext = ctx.getApplicationContext();
    }

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IAidlInferenceService.Stub.asInterface(service);
            bound = true;
            log.d("Inference Service Connected!");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            bound = false;
        }
    };

    public void bind() {
        if (bound) return;

        Intent intent = new Intent("InferenceService");
        intent.setPackage("br.org.eldorado.hiaac.poc.ui.poc2");

        boolean ok = mContext.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        log.d( "Trying to bind service br.org.eldorado.hiaac.poc.ui.poc2=" + ok);
    }

    public void unbind() {
        if (bound) {
            mContext.unbindService(mConnection);
            bound = false;
        }
    }

    public boolean isBound() {
        return bound && mService != null;
    }

    public void runInference(String filename) {
        if (!isBound()) {
            log.e("Service is not bounded");
            return;
        }
        try {
            mService.run(filename);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
