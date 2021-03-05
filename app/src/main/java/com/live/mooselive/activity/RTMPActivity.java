package com.live.mooselive.activity;

import android.widget.EditText;

import com.live.mooselive.R;
import com.live.mooselive.base.BaseActivity;
import com.live.mooselive.utils.LogUtil;

import butterknife.BindView;

public class RTMPActivity extends BaseActivity {

    @BindView(R.id.et_rtmp_data)
    EditText etRtmpData;

    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rtmp;
    }

    @Override
    protected void init() {
        new Thread(()->{
            connectRTMP("rtmp://58.200.131.2:1935/livetv/hunantv");
        }).start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeRTMP();
    }

    public native void connectRTMP(String url);

    public native void closeRTMP();


    void callFromNative() {
        LogUtil.e("MainActivity","callFromNative");
    }

    void receiveRtmpData(byte[] bytes) {
        LogUtil.e("RTMPActivity","receive new rtmp data!!");
    }
}