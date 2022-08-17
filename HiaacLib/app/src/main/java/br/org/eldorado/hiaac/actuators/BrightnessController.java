package br.org.eldorado.hiaac.actuators;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class BrightnessController {
    private Activity activity;

    protected BrightnessController(Activity activity) {
        this.activity = activity;
    }

    public void BrightnessUp() {
        changeBrightness(20);
    }

    public void BrightnessDown() {
        changeBrightness(-20);
    }

    /**
     * Set brightness level.
     * @param brightness value must be >= 0 and <= 255
     */
    public void setBrightness(int brightness) {
        Context context = activity.getApplicationContext();

        if (brightness > 255) {
            brightness =  255;
        } else if (brightness < 0){
            brightness = 0;
        }

        boolean permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permission = Settings.System.canWrite(context);
        } else {
            permission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
        }
        if (permission) {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS, brightness);
        }  else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                activity.startActivityForResult(intent, 123);
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.WRITE_SETTINGS}, 123);
            }
        }
        Log.d("Brightness", ": " + brightness);
    }

    public int getBrightness() {
        Context context = activity.getApplicationContext();
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.SCREEN_BRIGHTNESS, 0);
    }

    private void changeBrightness(int change) {
        int brightness = getBrightness();
        brightness = brightness + change;
        setBrightness(brightness);
    }
}
