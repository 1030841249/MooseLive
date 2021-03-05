package com.live.mooselive.av;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.live.mooselive.R;
import com.live.mooselive.av.encoder.MediaEncoder;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

public class ScreenLive {

    private static final int REQUEST_CODE = 0X01;

    static {
        System.loadLibrary("native-lib");
    }

    private MediaProjectionManager mediaProjectionManager = null;
    private MediaProjection mediaProjection;
    private MediaEncoder mediaEncoder;
    private VirtualDisplay virtualDisplay;

    int width,height;


    Context mContext;

    public ScreenLive(Context mContext) {
        this.mContext = mContext;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void init() {
        mediaEncoder = new MediaEncoder("");
        initMediaProjection();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaProjection() {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent screenIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity)mContext).startActivityForResult(screenIntent,REQUEST_CODE);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection == null) {
                    DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                    virtualDisplay = mediaProjection.createVirtualDisplay("Anchor", width, height, displayMetrics.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mediaEncoder.getSurface(), null, null);
                }
            }
        }
    }

    public native void sendData(int type,byte[] data,int len,long tms);
}
