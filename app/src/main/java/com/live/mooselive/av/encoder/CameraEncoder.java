package com.live.mooselive.av.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.live.mooselive.utils.RTMPUtil;
import com.live.mooselive.utils.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class CameraEncoder implements Runnable{
    private final int FRAME_RATE_PER_SENCOND = 15;
    private final int I_FRAME_INTERVAL = 3;
    MediaCodec mEncoder;
    protected MediaFormat mFormat;
    private Deque<byte[]> mFrameLists = new LinkedList<>();
    private ByteBuffer[] mInpufBuffers,mOutputBuffers;
    private boolean isRunning = true;
    private int mFrameCount = 0;


    public CameraEncoder(int width,int height) {

        try {
            mEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            initConfigure(width,height);
            mEncoder.start();
            mInpufBuffers = mEncoder.getInputBuffers();
            mOutputBuffers = mEncoder.getOutputBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConfigure(int width,int height) {
        mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3);
        mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE_PER_SENCOND);
        mFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        mEncoder.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @Override
    public void run() {
        while (isRunning) {
            byte[] frame = mFrameLists.getFirst();
            if (frame != null) {
                mFrameCount ++;
                frame = YUVUtil.convertNV21ToNV12(frame);
                encodeFrame(frame);
            }
        }
    }

    private void encodeFrame(byte[] frame) {
        int index = mEncoder.dequeueInputBuffer(0);
        ByteBuffer byteBuffer;
        if (index >= 0) {
            byteBuffer = mInpufBuffers[index];
            byteBuffer.put(frame);
            mEncoder.queueInputBuffer(index, 0, frame.length, getVideoPts(mFrameCount), 0);
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        index = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
        while (index >= 0) {
            byteBuffer = mOutputBuffers[index];
            byteBuffer.position(bufferInfo.offset);
            byte[] video = new byte[bufferInfo.size - bufferInfo.offset];
            byteBuffer.get(video);
            RTMPUtil.sendData(RTMPUtil.RTMP_TYPE_VIDEO, video, video.length, bufferInfo.presentationTimeUs / 1000);
            mEncoder.releaseOutputBuffer(index,false);
            index = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private long getVideoPts(int frameIndex) {
        return (long) (1.0 * frameIndex / (FRAME_RATE_PER_SENCOND * I_FRAME_INTERVAL /* gop */) * 1000000);
    }

    public void addFrame(byte[] frame) {
        mFrameLists.addLast(frame);
    }

}
