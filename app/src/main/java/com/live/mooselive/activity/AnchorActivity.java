package com.live.mooselive.activity;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceView;
import android.view.TextureView;

import com.live.mooselive.R;
import com.live.mooselive.av.encoder.MediaEncoder;
import com.live.mooselive.base.BaseActivity;

import butterknife.BindView;

public class AnchorActivity extends BaseActivity {
    @Override
    protected int getLayoutId() {
        return R.layout.activity_anchor;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void init() {
    }
}