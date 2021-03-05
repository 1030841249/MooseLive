package com.live.mooselive.av.encoder;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

public class VideoEncoder extends BaseEncoder {
    int mWidth,mHeight;
    public VideoEncoder(AVMuxer avMuxer,int width,int height) {
        super(avMuxer);
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected void writeData() {

    }

    @Override
    protected void addFormat(MediaFormat mFormat) {
        mMuxer.addVideoFormat(mFormat);
    }

    @Override
    protected MediaFormat initConfigure(MediaCodec mEncoder) {
        MediaFormat videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, mWidth, mHeight);
        videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, mWidth * mHeight * 3);
        videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 24);
        videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        return videoFormat;
    }

    @Override
    protected String encodeType() {
        return null;
    }
}
