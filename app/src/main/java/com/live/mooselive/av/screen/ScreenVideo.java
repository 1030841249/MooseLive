package com.live.mooselive.av.screen;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Bundle;
import android.view.Surface;

import com.live.mooselive.av.extractor.VideoExtracotr;
import com.live.mooselive.bean.RTMPPacket;
import com.live.mooselive.utils.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ScreenVideo implements Runnable {

    private final int BIT_RATE = 800000;
    private final int FRAME_RATE = 15;
    private final int I_FRAME_INTERVAL = 5;


    private MediaCodec mCodec;
    private MediaFormat mFormat;
    MediaCodec.BufferInfo mBufferInfo;
    ByteBuffer[] mOutputBuffers;
    private Surface mSurface;

    private boolean isRunning = true;

    int mWidth, mHeight;

    private long mStartTime;
    private long mTimeStamp;

    VideoExtracotr videoExtracotr;

    private ScreenLive.ScreenCodecCallback mCallback;

    public ScreenVideo(int width, int height,ScreenLive.ScreenCodecCallback callback) {
        mWidth = width;
        mHeight = height;
        mCallback = callback;

        initCodec();
    }

    private void initCodec() {
        try {
            mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            initConfiguration();
            mCodec.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mCodec.createInputSurface();
            mCodec.start();
            mOutputBuffers = mCodec.getOutputBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConfiguration() {
        mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
//        mFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        // bitrate
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        // fps
        mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        // i-INTERVAL
        mFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        // color format
        mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
    }

    @Override
    public void run() {
        while (isRunning) {
//            onReadVideo();
            onEncode();
        }
    }

    private void onReadVideo() {
        if (videoExtracotr == null) {
            videoExtracotr = new VideoExtracotr("/data/data/com.live.mooselive/test.mp4");
            if (mCallback != null) {
                mCallback.onEncodedVideo(videoExtracotr.readSPSPPS(),0);
            }
        }
        RTMPPacket packet = videoExtracotr.readOneFrame();
        if(packet != null) {
            if (mCallback != null) {
                mCallback.onEncodedVideo(packet.buffer, 30);
            }
        }
    }

    private void onEncode() {
        if (mBufferInfo == null) {
            mBufferInfo = new MediaCodec.BufferInfo();
        }
        if (mTimeStamp == 0) {
            mTimeStamp = System.currentTimeMillis();
        }
        if (System.currentTimeMillis() - mTimeStamp >= 5000) {
            Bundle bundle = new Bundle();
            bundle.putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0);
            mCodec.setParameters(bundle);
            mTimeStamp = System.currentTimeMillis();
        }
        int index = mCodec.dequeueOutputBuffer(mBufferInfo, 1000);
        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            mFormat = mCodec.getOutputFormat();
            if (mCallback != null) {
                mCallback.onFormatChanged(mFormat);
            }
        }
        if (index >= 0) {
            if (mStartTime == 0) {
                mStartTime = mBufferInfo.presentationTimeUs / 1000; // 微妙转换为毫秒
            }
            if (mBufferInfo.offset < mBufferInfo.size) {
                ByteBuffer outputBuffer = mOutputBuffers[index];
                outputBuffer.position(mBufferInfo.offset);
                outputBuffer.limit(mBufferInfo.size + mBufferInfo.offset);
                if (mCallback != null) {
                    byte[] data = new byte[mBufferInfo.size - mBufferInfo.offset];
                    outputBuffer.get(data);
                    long tms = (mBufferInfo.presentationTimeUs / 1000) - mStartTime;
                    mCallback.onEncodedVideo(data, tms);
                }
            }
            mCodec.releaseOutputBuffer(index, false);
        }
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        isRunning = false;
        mCodec.release();
        mCodec = null;
        mFormat = null;
        mSurface = null;
        mOutputBuffers = null;
    }

    public Surface getSurface() {
        return mSurface;
    }

}
