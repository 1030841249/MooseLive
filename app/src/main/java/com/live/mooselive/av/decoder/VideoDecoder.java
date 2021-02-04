package com.live.mooselive.av.decoder;

import android.media.MediaExtractor;
import android.view.Surface;

public class VideoDecoder extends BaseDecoder{

    public VideoDecoder(MediaExtractor mExtractor, Surface surface) {
        super(mExtractor, surface);
    }

    @Override
    protected String getFormatType() {
        return "video/";
    }
}
