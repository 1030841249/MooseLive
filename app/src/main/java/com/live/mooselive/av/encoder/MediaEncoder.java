package com.live.mooselive.av.encoder;

import android.view.Surface;

public class MediaEncoder {

    BaseEncoder mVideoEncoder;
    AVMuxer avMuxer;
    public MediaEncoder(String path) {
        avMuxer = new AVMuxer(path);
        mVideoEncoder = new VideoEncoder(avMuxer,640,480);
    }

    public Surface getSurface() {
        return mVideoEncoder.getSurface();
    }
}
