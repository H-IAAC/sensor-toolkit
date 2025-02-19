package br.org.eldorado.hiaac.audiocollector;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * This is an utility and invisible Fragment class just to handle the audio permissions.
 * This approach make it easier use this library, since the application that uses it, wont need to implement
 * permissions logic on its side.
 */
public class PermissionFragment extends Fragment {
    private static final int REQUEST_RECORD_AUDIO = 1001;
    private PermissionCallback callback;

    public interface PermissionCallback {
        void onPermissionResult(boolean granted);
    }

    public PermissionFragment() {}

    public static PermissionFragment newInstance(PermissionCallback callback) {
        PermissionFragment fragment = new PermissionFragment();
        fragment.callback = callback;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (callback != null) {
                callback.onPermissionResult(granted);
            }
        }
        getParentFragmentManager().beginTransaction().remove(this).commit(); // Remove this fragment after the result
    }
}

