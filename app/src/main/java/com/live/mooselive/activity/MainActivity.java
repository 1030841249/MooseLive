package com.live.mooselive.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.live.mooselive.R;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.PermissionUtil;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.btn_anchor)
    Button btnAnchor;
    @BindView(R.id.btn_audience)
    Button btnAudience;
    @BindView(R.id.btn_rtmp_test)
    Button btnRtmp;
    @BindView(R.id.iv_main)
    ImageView ivMain;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        PermissionUtil.requestWRPermissions(this);
        PermissionUtil.requestMicoPermisson(this);
    }

    @OnClick({R.id.btn_anchor, R.id.btn_audience,R.id.btn_rtmp_test})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_anchor:
                startActivity(new Intent(this,AnchorActivity.class));
                break;
            case R.id.btn_audience:
                startActivity(new Intent(this,AudienceActivity.class));
                break;
            case R.id.btn_rtmp_test:
                startActivity(new Intent(this,RTMPActivity.class));
                break;
        }
    }
}
