package com.live.mooselive.av.decoder;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaExtractor;
import android.view.Surface;

import com.live.mooselive.R;

import java.io.FileDescriptor;
import java.io.IOException;

public class MediaDecoder {

    private MediaExtractor mExtractor;
    private VideoDecoder mVideoDecoder;
    private AudioDecoder mAudioDecoder;

    public MediaDecoder(String path, Surface surface) {
        try {
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(path);
            mVideoDecoder = new VideoDecoder(mExtractor, surface);
            mAudioDecoder = new AudioDecoder(mExtractor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MediaDecoder(Context context,int rawId, Surface surface) {
        try {
            mExtractor = new MediaExtractor();
//            mExtractor.setDataSource(path);
            AssetFileDescriptor fileDescriptor = context.getResources().openRawResourceFd(rawId);
            mExtractor.setDataSource(fileDescriptor);
            mVideoDecoder = new VideoDecoder(mExtractor, surface);
            mAudioDecoder = new AudioDecoder(mExtractor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void start() {
        if (mAudioDecoder != null) {
            new Thread(mAudioDecoder).start();
        }
        if (mVideoDecoder != null) {
//            new Thread(mVideoDecoder).start();
        }
    }

    public VideoDecoder getmVideoDecoder() {
        return mVideoDecoder;
    }

    public AudioDecoder getmAudioDecoder() {
        return mAudioDecoder;
    }
}
