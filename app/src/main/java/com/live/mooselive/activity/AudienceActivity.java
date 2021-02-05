package com.live.mooselive.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.live.mooselive.R;
import com.live.mooselive.av.decoder.MediaDecoder;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.utils.PermissionUtil;

import butterknife.BindView;

public class AudienceActivity extends BaseActivity {

    @BindView(R.id.sfv_audience)
    SurfaceView surfaceView;

    MediaDecoder mediaDecoder;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_audience;
    }

    @Override
    protected void init() {
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
//                mediaDecoder = new MediaDecoder("http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4",holder.getSurface());
                String path = "sdcard/test.mp4";
//                mediaDecoder = new MediaDecoder(path,holder.getSurface());
                mediaDecoder = new MediaDecoder(AudienceActivity.this,R.raw.test,holder.getSurface());
                mediaDecoder.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }
}