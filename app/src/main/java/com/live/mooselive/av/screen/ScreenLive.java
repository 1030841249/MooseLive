package com.live.mooselive.av.screen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.live.mooselive.utils.LogUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenLive implements Runnable {

    public static final String TAG = "ScreenLive";

    private static final int REQUEST_CODE = 0X01;

    private static final int RTMP_TYPE_VIDEO = 0X00;
    private static final int RTMP_TYPE_ADUIO_HEADER = 0X01;
    private static final int RTMP_TYPE_AUDIO_DATA = 0X02;

    public interface ScreenCodecCallback {
        void onEncodedVideo(byte[] data,long tms);
        void onEncodedAudio(byte[] data,long tms);
        void onFormatChanged(MediaFormat mediaFormat);
    }

    static {
        System.loadLibrary("native-lib");
    }

    private MediaProjectionManager mediaProjectionManager = null;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private ScreenCodecCallback mCallback;
    private ScreenVideo mScreenVideo;
    private ScreenAudio mScreenAudio;

    private String mUrl;
    private int width,height;

    Context mContext;

    boolean isFirstAudio = true;

    File file;
    OutputStream outputStream;
    public ScreenLive(Context mContext,String url) {
        this.mContext = mContext;
        mUrl = url;
        file = new File("/data/data/com.live.mooselive/fle.h264");
        file.delete();
        try {
            outputStream = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        mCallback = new ScreenCodecCallback() {
            @Override
            public void onEncodedVideo(byte[] data, long tms) {
                LogUtil.e(TAG,"Video tms " + tms);
                sendData(RTMP_TYPE_VIDEO, data, data.length, tms);
                try {
                    outputStream.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onEncodedAudio(byte[] data, long tms) {
                if (isFirstAudio) {
                    isFirstAudio = false;
                    sendData(RTMP_TYPE_ADUIO_HEADER, new byte[]{(byte)0x11,(byte)0x90}, 2, 0);
                    return;
                }
                sendData(RTMP_TYPE_AUDIO_DATA, data, data.length, tms);
            }

            @Override
            public void onFormatChanged(MediaFormat mediaFormat) {
//                ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-0");
//                byte[] sps = new byte[byteBuffer.remaining()];
//                byteBuffer.get(sps);
//                byteBuffer = mediaFormat.getByteBuffer("csd-1");
//                byte[] pps = new byte[byteBuffer.remaining()];
//                byteBuffer.get(pps);
//                byte[] buffer = new byte[sps.length + pps.length];
//                System.arraycopy(sps, 0, buffer, 0, sps.length);
//                System.arraycopy(pps, 0, buffer, sps.length, pps.length);
//                sendSPSPPS(buffer,buffer.length);
            }
        };
//        mFile = new File("/data/data/com.live.mooselive/projection.h264");
//        if (mFile.exists()) {
//            mFile.delete();
//        }
//        try {
//            mFile.createNewFile();
//            outputStream = new FileOutputStream(mFile);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void initScreenCodec() {
        mScreenVideo = new ScreenVideo(width, height,mCallback);
        mScreenAudio = new ScreenAudio(mCallback);
    }


    private void initMediaProjection() {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;
//        width = 480;
//        height = 720;
        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent screenIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity)mContext).startActivityForResult(screenIntent,REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                initScreenCodec();
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection != null) {
                    DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                    virtualDisplay = mediaProjection.createVirtualDisplay("-Anchor", width, height, 1,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mScreenVideo.getSurface(), null, null);
                    new Thread(this).start();
                }
            }
        }
    }

    @Override
    public void run() {
        LogUtil.e("ScreenLive","连接 RTMP");
        if (connectRTMP(mUrl) == 1) {
            LogUtil.e("ScreenLive","开启编码器");
            // 连接成功后开始编码，并传输数据
            mScreenVideo.start();
//            mScreenAudio.start();
        } else {
            LogUtil.e("ScreenLive","RTMP 连接失败");
        }
    }

    /**
     * 连接到 RTMP 地址，开始直播
     */
    public void startLive() {
        initMediaProjection();
    }

    public void stopLive(){
        closeRTMP();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (mScreenVideo != null) {
            mScreenVideo.stop();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
    }

    private native int connectRTMP(String url);

    private native int closeRTMP();

    public native void sendData(int type,byte[] data,int len,long tms);

    private native void sendSPSPPS(byte[] data,int len);
}
