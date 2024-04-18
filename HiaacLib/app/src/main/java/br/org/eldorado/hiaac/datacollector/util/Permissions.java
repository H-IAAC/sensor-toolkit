package br.org.eldorado.hiaac.datacollector.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.TooManyListenersException;

public class Permissions {
    private final Context context;
    private final ActivityResultLauncher<String[]> rpl;
    private final String[] REQUIRED_PERMISSIONS = new String[]{ Manifest.permission.POST_NOTIFICATIONS,
                                                                Manifest.permission.CAMERA};

    public Permissions(ComponentActivity activity, Context context) {
        this.context = context;

        rpl = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        if (!areAllPermissionsGranted()) {
                            Toast.makeText(context, "Not all permissions granted by the user.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    public boolean areAllPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean isCameraPermissionsGranted() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    public void askPermissions() {
        if (!areAllPermissionsGranted()) {
            rpl.launch(REQUIRED_PERMISSIONS);
        }
    }
}
