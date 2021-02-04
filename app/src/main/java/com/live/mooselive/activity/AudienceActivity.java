package com.live.mooselive.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.live.mooselive.R;
import com.live.mooselive.av.decoder.MediaDecoder;
import com.live.mooselive.base.BaseActivity;

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
                mediaDecoder = new MediaDecoder("rtmp://58.200.131.2:1935/livetv/cctv1",holder.getSurface());
                mediaDecoder.start();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
//                params.width = width;
//                params.height = height;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });

    }
}