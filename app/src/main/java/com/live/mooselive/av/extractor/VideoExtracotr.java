package com.live.mooselive.av.extractor;

import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.live.mooselive.bean.RTMPPacket;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoExtracotr {

    private MediaExtractor mExtractor;
    private MediaFormat mFormat;
    int mWidth,mHeight;

    long mStartTime = 0;

    public VideoExtracotr(String path) {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        for(int i = 0; i < mExtractor.getTrackCount();i++) {
            MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
            if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith("video")) {
                mExtractor.selectTrack(i);
                mFormat = mediaFormat;
                mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
            }
        }
    }

    public byte[] readSPSPPS() {
        ByteBuffer byteBuffer = mFormat.getByteBuffer("csd-0");
        byte[] sps = new byte[byteBuffer.remaining()];
        byteBuffer.get(sps);
        byteBuffer = mFormat.getByteBuffer("csd-1");
        byte[] pps = new byte[byteBuffer.remaining()];
        byteBuffer.get(pps);
        byte[] buffer = new byte[sps.length + pps.length];
        System.arraycopy(sps, 0, buffer, 0, sps.length);
        System.arraycopy(pps, 0, buffer, sps.length, pps.length);
        return buffer;
    }

    public RTMPPacket readOneFrame() {
        byte[] buffer;
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(mWidth * mHeight * 3);
        byteBuffer.clear();
        byteBuffer.position(0);
        int readNums = mExtractor.readSampleData(byteBuffer, 0);
        if (readNums > 0) {
            buffer = new byte[readNums];
            byteBuffer.get(buffer);
            if (mStartTime == 0) {
                mStartTime = getPTS();
            }
            long tms = getPTS() - mStartTime;
            mStartTime = getPTS();
            RTMPPacket packet = new RTMPPacket(buffer, readNums,tms);
            return packet;
        }
        return null;
    }

    public long getPTS() {
        return mExtractor.getSampleTime() / 1000;
    }
}
