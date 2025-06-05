package br.org.eldorado.hiaac.actuators;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.util.Log;

public class BluetoothController {
    Activity activity;

    protected BluetoothController(Activity activity) {
        this.activity = activity;
    }

    @SuppressLint("MissingPermission")
    public void toogle() {
        Log.d("BT", "toogle");

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Log.d("BT", "Bluetooth disable");
        } else {
            mBluetoothAdapter.enable();
            Log.d("BT", "Bluetooth enable");
        }
    }

    public boolean getBluetoothStatus() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return mBluetoothAdapter.isEnabled();
    }
}
