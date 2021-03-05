package com.live.mooselive.av.encoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AVMuxer {

    private MediaMuxer mMuxer;
    private int mAudioIndex = -1;
    private int mVideoIndex = -1;
    private boolean addAudio = false;
    private boolean addVideo = false;
    private boolean isStart = false;

    public AVMuxer(String path) {
        try {
            mMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addAudioFormat(MediaFormat audioFormat) {
        if (audioFormat != null) {
            addAudio = true;
            mAudioIndex = mMuxer.addTrack(audioFormat);
            startMuxer();
        }
    }

    public void addVideoFormat(MediaFormat videoFormat) {
        if (videoFormat != null) {
            addVideo = true;
            mVideoIndex = mMuxer.addTrack(videoFormat);
            startMuxer();
        }
    }

    public void startMuxer() {
        if (addAudio && addVideo) {
            mMuxer.start();
            isStart = true;
        }
    }

    public void wirteVideoData(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (isStart) {
            mMuxer.writeSampleData(mVideoIndex,byteBuffer,bufferInfo);
        }
    }

    public void wirteAudioData(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
        if (isStart) {
            mMuxer.writeSampleData(mAudioIndex,byteBuffer,bufferInfo);
        }
    }

}
