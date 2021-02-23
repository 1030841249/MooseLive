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
    @BindView(R.id.iv_main)
    ImageView ivMain;

    static {
        System.loadLibrary("native-lib");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    public native void connectRTMP(String url);

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
        new Thread(()->{
//            connectRTMP("rtmp://58.200.131.2:1935/livetv/hunantv");
        }).start();
        PermissionUtil.requestWRPermissions(this);
        PermissionUtil.requestMicoPermisson(this);
    }

    @OnClick({R.id.btn_anchor, R.id.btn_audience})
    void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_anchor:
                startActivity(new Intent(this,AnchorActivity.class));
                break;
            case R.id.btn_audience:
                startActivity(new Intent(this,AudienceActivity.class));
                break;
        }
    }

    void callFromNative() {
        LogUtil.e("MainActivity","callFromNative");
    }

    void receiveRtmpData(byte[] bytes) {
        for (byte aByte : bytes) {
            LogUtil.e("MainActivity","receiveRtmpData" + aByte);
        }
//        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//        ivMain.setImageBitmap(bitmap);
    }
}
