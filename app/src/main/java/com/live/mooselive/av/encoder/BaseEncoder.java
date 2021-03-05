package com.live.mooselive.av.encoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BaseEncoder implements Runnable{

    MediaCodec mEncoder;
    protected AVMuxer mMuxer;
    protected MediaFormat mFormat;
    private Surface mSurface;

    ByteBuffer[] mInpufBuffers,mOutputBuffers;

    boolean isRunning = true;
    private boolean isEOF = false;

    public BaseEncoder(AVMuxer avMuxer) {
        mMuxer = avMuxer;
        try {
            mEncoder = MediaCodec.createEncoderByType(encodeType());
            mFormat = initConfigure(mEncoder);
            mSurface = mEncoder.createInputSurface();
            addFormat(mFormat);
            mEncoder.start();
            mInpufBuffers = mEncoder.getInputBuffers();
            mOutputBuffers = mEncoder.getOutputBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            onEncode();
            writeData();
        }
    }

    private void onEncode() {
        int index = -1;
        while(!isEOF) {
            index = mEncoder.dequeueInputBuffer(0);
            if (index >= 0) {

            }
        }
    }

    public Surface getSurface() {
        return mSurface;
    }

    protected abstract void writeData();

    protected abstract void addFormat(MediaFormat mFormat);

    protected abstract MediaFormat initConfigure(MediaCodec mEncoder);

    protected abstract String encodeType();
}
