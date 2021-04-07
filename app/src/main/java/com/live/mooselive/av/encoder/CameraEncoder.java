package com.live.mooselive.av.encoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;

import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.RTMPUtil;
import com.live.mooselive.utils.YUVUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class CameraEncoder implements Runnable{
    private static final String TAG = "CameraEncoder";

    private final int FRAME_RATE_PER_SENCOND = 30;
    private final int I_FRAME_INTERVAL = 1;
    MediaCodec mEncoder;
    protected MediaFormat mFormat;
    private Deque<byte[]> mFrameLists = new LinkedList<>();
    private ByteBuffer[] mInpufBuffers,mOutputBuffers;

    private boolean isRunning = true;
    private int mFrameCount = 0;
    private int mCurCameraType = CAMERA_FACING_BACK;

    int width = 1920;
    int height = 1080;

    public CameraEncoder(int width,int height) {
        this.width = width;
        this.height = height;
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
        // 交换宽高是因为，手机上捕获的图像需要旋转，旋转后宽高也就不同了
        mFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, height, width);
        mFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 3/2);
        mFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE_PER_SENCOND);
        mFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        mEncoder.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    @Override
    public void run() {
        isRunning = true;
        while (isRunning) {
            if (!mFrameLists.isEmpty()) {
                byte[] frame = mFrameLists.getFirst();
                mFrameCount ++;
                frame = YUVUtil.convertNV21ToNV12(frame, width, height);
                if (mCurCameraType == CAMERA_FACING_FRONT) { // 前置
                    frame = YUVUtil.rotateYUVDegree270(frame, width, height);
                } else {
                    frame = YUVUtil.rotateYUVDegree90(frame, width, height);
                }

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
//            byteBuffer.position(bufferInfo.offset);
            byte[] video = new byte[bufferInfo.size];
            byteBuffer.get(video);
            RTMPUtil.sendData(RTMPUtil.RTMP_TYPE_VIDEO, video, video.length, bufferInfo.presentationTimeUs / 1000);
//            RTMPUtil.sendDataNeedRotate(RTMPUtil.RTMP_TYPE_VIDEO, video, video.length, bufferInfo.presentationTimeUs / 1000,width,height);
            mEncoder.releaseOutputBuffer(index,false);
            index = mEncoder.dequeueOutputBuffer(bufferInfo, 0);
        }
    }

    private long getVideoPts(int frameIndex) {
        return (long) (1.0 * frameIndex / (FRAME_RATE_PER_SENCOND * I_FRAME_INTERVAL /* gop */) * 1000000);
    }

    public void stop() {
        RTMPUtil.closeRTMP();
        mFrameCount = 0;
        isRunning = false;
        mEncoder.stop();
        mEncoder.release();
        mInpufBuffers = null;
        mOutputBuffers = null;
        cleanFrame();
    }

    public void setCameraType(int type) {
        mCurCameraType = type;
    }

    public void addFrame(byte[] frame) {
        mFrameLists.addLast(frame);
//        LogUtil.e(TAG,"AddFrame");
    }

    public void cleanFrame() {
        mFrameLists.clear();
    }
}
