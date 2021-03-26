package com.live.mooselive.activity;

import androidx.annotation.Nullable;

import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.live.mooselive.R;
import com.live.mooselive.av.screen.ScreenLive;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.utils.PermissionUtil;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.OnClick;

public class MainActivity extends BaseActivity {

    @BindView(R.id.btn_anchor)
    Button btnAnchor;
    @BindView(R.id.btn_audience)
    Button btnAudience;
    @BindView(R.id.btn_rtmp_test)
    Button btnRtmp;
    @BindView(R.id.btn_mediaProjection)
    Button btnProjection;
    @BindView(R.id.iv_main)
    ImageView ivMain;
    @BindView(R.id.et_main_rtmp_addr)
    EditText etRTMPAddr;
    ScreenLive screenLive;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void init() {
//        PermissionUtil.requestRecordAudioPermission(this);
        PermissionUtil.requestWRPermissions(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent,123);
            }
        }
        etRTMPAddr.setText("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11852946_29221900&key=e8d63f1642891cfa3b342276147f62ca&schedule=rtmp");
//        etRTMPAddr.setText("rtmp://tx.direct.huya.com/huyalive/1640117789-1640117789-0-3280359034-10057-A-1615440186-1?seq=1615440187407&type=simple");
    }

    @OnClick({R.id.btn_anchor, R.id.btn_audience,R.id.btn_rtmp_test,R.id.btn_mediaProjection})
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
            case R.id.btn_mediaProjection:
                if (btnProjection.getText().toString().startsWith("开始")) {
                    btnProjection.setText("结束屏幕投影");
                    startLive();
                } else{
                    btnProjection.setText("开始屏幕投影");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        screenLive.stopLive();
                    }
                }


                break;
        }
    }

    private void startLive() {
        if (screenLive == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String url = etRTMPAddr.getText().toString();
                if (!TextUtils.isEmpty(url)) {
                    screenLive = new ScreenLive(this, url);
                } else {
                    Toast.makeText(this, "请输入 RTMP 推流地址", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenLive.startLive();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (screenLive != null) {
                screenLive.onActivityResult(requestCode, resultCode, data);
            }
        }

    }
}
