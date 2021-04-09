package com.live.mooselive.av.camera;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.live.mooselive.av.bean.BaseLive;
import com.live.mooselive.av.bean.RTMPPacket;
import com.live.mooselive.av.screen.ScreenLive;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.live.mooselive.utils.RTMPUtil.RTMP_TYPE_ADUIO_HEADER;
import static com.live.mooselive.utils.RTMPUtil.RTMP_TYPE_AUDIO_DATA;

public class CameraAudio extends BaseLive {

    private static final String TAG = "CameraAudio";
    private MediaCodec mCodec;
    private MediaFormat mFormat;
    private AudioRecord mAudioRecord;

    private ByteBuffer[] mInputBuffers,mOutputBuffers;
    private long mAudioPTS = 0;
    private long mStarTime = 0;
    private boolean isFirst = true;

    private int mBufferSizeInBytes;

    public CameraAudio() {
        initCodec();
    }

    private void initCodec() {
        try {
            initConfiguration();
            mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mCodec.configure(mFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mCodec.start();
            mInputBuffers = mCodec.getInputBuffers();
            mOutputBuffers = mCodec.getOutputBuffers();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConfiguration() {
        int sampleRate = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int format = AudioFormat.ENCODING_PCM_16BIT;
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, format);
        mFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1);
        mFormat.setInteger(MediaFormat.KEY_BIT_RATE, sampleRate * 2);
        mFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT, mBufferSizeInBytes);
    }

    @Override
    public void running() {
        onEncode();
    }

    private void onEncode() {
        byte[] buffer = new byte[mBufferSizeInBytes];
        while (true) {
            //                Thread.sleep(1);
            int readNums = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
            if (readNums > 0) {
                int index = mCodec.dequeueInputBuffer(10);
                if (index >= 0) {
                    ByteBuffer inputBuffer = mInputBuffers[index];
                    inputBuffer.limit(readNums);
                    inputBuffer.put(buffer);
                    mCodec.queueInputBuffer(index, 0, readNums, getAudioPts(readNums,44100), 0);
                }
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                index = mCodec.dequeueOutputBuffer(bufferInfo, 0);
                if (index >= 0) {
                    ByteBuffer outputBuffer = mOutputBuffers[index];
                    outputBuffer.position(bufferInfo.offset);
                    byte[] audio = new byte[bufferInfo.size];
                    outputBuffer.get(audio);
                    if (isFirst) {
                        addFrame(new RTMPPacket(RTMP_TYPE_ADUIO_HEADER, new byte[]{(byte) 0x11, (byte) 0x90}, 2, 0));
                        isFirst = false;
                    }
                    addFrame(new RTMPPacket(RTMP_TYPE_AUDIO_DATA, audio, audio.length, bufferInfo.presentationTimeUs / 1000));
                    mCodec.releaseOutputBuffer(index,false);
                }
            }
        }
    }

    private long getAudioPts(int size, int sampleRate) {
        mAudioPTS += (long) (1.0 * size / (sampleRate * 2) * 1000000.0);
        return mAudioPTS;
    }

    @Override
    public void onStart() {
        mAudioRecord.startRecording();
        new Thread(this).start();
    }

    @Override
    public void onStop() {
        isFirst = true;
        mFormat = null;
        if (mCodec != null) {
            mCodec.release();
        }
        if (mAudioRecord != null) {
        }
        mAudioRecord.stop();
        mAudioRecord.release();
    }

}
