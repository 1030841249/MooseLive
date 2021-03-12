package com.live.mooselive.activity;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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
        int audioSize = AudioRecord.getMinBufferSize(44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                44100,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                audioSize);
//        new Thread(()->{
//            connectRTMP("rtmp://live-push.bilivideo.com/live-bvc/?streamname=live_11852946_29221900&key=e8d63f1642891cfa3b342276147f62ca&schedule=rtmp");
////            connectRTMP("rtmp://58.200.131.2:1935/livetv/cctv1");
//        }).start();

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