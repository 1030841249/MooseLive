package com.live.mooselive.activity;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.live.mooselive.R;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.av.camera.CameraLive;
import com.live.mooselive.utils.ToastUtil;

import butterknife.BindView;

public class AnchorActivity extends BaseActivity {

    @BindView(R.id.sfv_anchor)
    SurfaceView sfvAnchor;
    @BindView(R.id.btn_switch_camera)
    Button btnSwitch;
    @BindView(R.id.btn_anchor_start_live)
    Button btnStartLive;
    @BindView(R.id.btn_anchor_stop_live)
    Button btnStopLive;
    @BindView(R.id.iv_cover)
    ImageView ivCover;
    CameraLive cameraUtil;

    String url = "rtmp://tx.direct.huya.com/huyalive/1640117789-1640117789-0-3280359034-10057-A-1615440186-1?seq=1615440187407&type=simple";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_anchor;
    }

    @Override
    protected void init() {
        btnSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraUtil != null) {
                    cameraUtil.switchCamera();
                }
                ToastUtil.showShortToast("切换摄像头，请重新开播");
            }
        });
        btnStartLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraUtil.startLive(url);
                ToastUtil.showShortToast("开始直播");
                btnStartLive.setEnabled(false);
                btnStopLive.setEnabled(true);
            }
        });
        btnStopLive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cameraUtil.stopLive();
                ToastUtil.showShortToast("已关闭直播");
                btnStartLive.setEnabled(true);
                btnStopLive.setEnabled(false);
            }
        });
        sfvAnchor.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                cameraUtil = new CameraLive(AnchorActivity.this,holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//                cameraUtil.initParameters();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraUtil.stopPreview();
            }
        });
    }
}