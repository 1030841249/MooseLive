package com.live.mooselive.av.camera;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.live.mooselive.av.bean.BaseLive;
import com.live.mooselive.av.bean.RTMPPacket;
import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.RTMPUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class CameraMedia {

    private static final String TAG = "CameraMedia";
    private BaseLive mVideoLive;
    private BaseLive mAudioLive;
    private Future mTask;
    private ScheduledExecutorService mExecutorService;
    boolean isRunning = true;
    public static long mTotalReduceTime = 0;
    public CameraMedia(BaseLive mVideoLive, BaseLive mAudioLive) {
        this.mVideoLive = mVideoLive;
        this.mAudioLive = mAudioLive;
        mExecutorService = Executors.newScheduledThreadPool(2);
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                while (isRunning) {
                    RTMPPacket videoPacket = null ,audioPacket = null;
                    if (mVideoLive != null && mVideoLive.isNotEmpty() ) {
                        videoPacket = mVideoLive.getFristFrame();
                    }
                    if (mAudioLive != null && mAudioLive.isNotEmpty()) {
                        audioPacket = mAudioLive.getFristFrame();
                    }
                    if (videoPacket != null && audioPacket != null) {
                        mVideoLive.removeFirst();
                        mAudioLive.removeFirst();
                        if ((videoPacket.tms - audioPacket.tms) > 50L) {
                            long diff = (videoPacket.tms - audioPacket.tms-mTotalReduceTime) / 4*3;
                            mTotalReduceTime +=diff;
                            LogUtil.e(TAG,"diff " + diff + "    mtotalTime " + mTotalReduceTime);
                            videoPacket.tms -= diff;
                        }
                        if (videoPacket.tms < audioPacket.tms) {
                            RTMPUtil.sendData(videoPacket.type, videoPacket.data, videoPacket.len, videoPacket.tms);
                            LogUtil.e(TAG, "VideoPakcet tms " + videoPacket.tms);
                        } else {
                            RTMPUtil.sendData(audioPacket.type,audioPacket.data,audioPacket.len,audioPacket.tms);
                            LogUtil.e(TAG,"AudioPacket tms " + audioPacket.tms);
                        }
                    } else {
//                        if (videoPacket != null) {
//                            RTMPUtil.sendData(videoPacket.type, videoPacket.data, videoPacket.len, videoPacket.tms);
//                            LogUtil.e(TAG, "VideoPakcet tms " + videoPacket.tms);
//                        }
//                        if (audioPacket != null) {
//                            RTMPUtil.sendData(audioPacket.type,audioPacket.data,audioPacket.len,audioPacket.tms);
//                            LogUtil.e(TAG,"AudioPacket tms " + audioPacket.tms);
//                        }
                    }
                }
            }
        });
    }

    public void startLive() {
        isRunning = true;
        if (mVideoLive != null) {
            mVideoLive.start();
//            mExecutorService.submit(mVideoLive);
        }
        if (mAudioLive != null) {
            mAudioLive.start();
//            mExecutorService.submit(mAudioLive);
        }
    }

    public void stopLive() {
        isRunning = false;
        if (mVideoLive != null) {
            mVideoLive.stop();
        }
        if (mAudioLive != null) {
            mAudioLive.stop();
        }
    }

    public void release() {
        mVideoLive = null;
        mAudioLive = null;
    }
}
