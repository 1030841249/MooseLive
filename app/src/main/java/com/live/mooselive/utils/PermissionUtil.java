package com.live.mooselive.utils;

import android.Manifest;
import android.app.Activity;

import androidx.core.app.ActivityCompat;

public class PermissionUtil {

    public static void requestWRPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
    }
}
