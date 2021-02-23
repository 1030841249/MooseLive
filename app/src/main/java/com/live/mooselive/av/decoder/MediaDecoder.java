package com.live.mooselive.av.decoder;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.os.Handler;
import android.view.Surface;

import com.live.mooselive.R;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class MediaDecoder {

    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;

    private ExecutorService mThreadPool;

    public MediaDecoder(String path, Surface surface) {
        mVideoDecoder = new VideoDecoder(path, surface);
        mAudioDecoder = new AudioDecoder(path);
        mThreadPool = Executors.newFixedThreadPool(5);
    }


    public void start() {
        if (mAudioDecoder != null) {
            mThreadPool.submit(mAudioDecoder);
        }
        if (mVideoDecoder != null) {
            mThreadPool.submit(mVideoDecoder);
        }
    }

    public void pause() {
        if (mAudioDecoder != null) {
            try {
                mAudioDecoder.pause();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (mVideoDecoder != null) {
            try {
                mVideoDecoder.pause();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void resume() {
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                mAudioDecoder.resume();
            }
        });
        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                mVideoDecoder.resume();
            }
        });
    }

    public void release() {
        mAudioDecoder.finish();
        mVideoDecoder.finish();
        mThreadPool.shutdown();
    }

    public VideoDecoder getmVideoDecoder() {
        return mVideoDecoder;
    }

    public AudioDecoder getmAudioDecoder() {
        return mAudioDecoder;
    }
}
