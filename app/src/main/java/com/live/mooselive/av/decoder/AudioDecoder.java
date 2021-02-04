package com.live.mooselive.av.decoder;

import android.media.MediaExtractor;

public class AudioDecoder extends BaseDecoder {

    public AudioDecoder(MediaExtractor mExtractor) {
        super(mExtractor,null);
    }

    @Override
    protected String getFormatType() {
        return "audio/";
    }
}
