package com.live.mooselive.av.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.view.Surface;

import java.nio.ByteBuffer;

public class VideoDecoder extends BaseDecoder{

    public VideoDecoder(MediaExtractor mExtractor, Surface surface) {
        super(mExtractor, surface);
    }

    @Override
    protected String getFormatType() {
        return "video/";
    }

    @Override
    protected void renderData(MediaCodec.BufferInfo bufferInfo, ByteBuffer byteBuffer) {

    }
}
