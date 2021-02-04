package com.live.mooselive.av.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.live.mooselive.utils.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BaseDecoder implements Runnable {

    static final String TAG = BaseDecoder.class.getName();

    MediaCodec mDecoder;
    MediaExtractor mExtractor;
    MediaFormat mFormat;
    Surface mSurface;
    ByteBuffer[] mInputBuffers, mOutputBuffers;

    private int mWidth;
    private int mHeight;
    private long mDuration;

    private DecodeState mCurState;
    boolean render = false;

    public BaseDecoder(MediaExtractor mExtractor,Surface surface) {
        this.mExtractor = mExtractor;
        mSurface = surface;
    }

    @Override
    public void run() {
        while(true) {
            initFormat();
            initDecoder();
            // Android 4.4 版本以下
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                mInputBuffers = mDecoder.getInputBuffers();
                mOutputBuffers = mDecoder.getOutputBuffers();
                onDecode();
            }
        }
    }

    protected void initFormat() {
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
            if (mediaFormat.getString(MediaFormat.KEY_MIME).startsWith(getFormatType())) {
                mFormat = mediaFormat;
                mExtractor.selectTrack(i);
                mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
                mDuration = mFormat.getLong(MediaFormat.KEY_DURATION);
                break;
            }
        }
    }

    protected void initDecoder() {
        try {
            mCurState = DecodeState.INIT;
            String type = mFormat.getString(MediaFormat.KEY_MIME);
            mDecoder = MediaCodec.createDecoderByType(type);
            // 异步处理，需要在调用 configure 之前设置
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDecoder.setCallback(new MediaCodec.Callback() {
                    @Override
                    public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                        BaseDecoder.this.onInputBufferAvailable(index,codec.getInputBuffer(index));
                    }

                    @Override
                    public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                        BaseDecoder.this.onOutputBufferAvailable(index, info, codec.getOutputBuffer(index));
                    }

                    @Override
                    public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                        BaseDecoder.this.onError(e);
                    }

                    @Override
                    public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                        BaseDecoder.this.onOutputFormatChanged(format);
                    }
                });
            }
            mDecoder.configure(mFormat, mSurface, null, 0);
            mDecoder.start();
            mCurState = DecodeState.START;
        } catch (IOException e) {
            LogUtil.e(TAG,"解码器创建失败！");
            e.printStackTrace();
        }
    }

    protected void onDecode() {
        int inputIndex = mDecoder.dequeueInputBuffer(1000);
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = mInputBuffers[inputIndex];
            onInputBufferAvailable(inputIndex,inputBuffer);
        } else {
            // TODO
            return;
        }
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 1000);
        if (outputIndex > 0) {
            ByteBuffer outputBuffer = mOutputBuffers[outputIndex];
            onOutputBufferAvailable(inputIndex, bufferInfo,outputBuffer);
        } else {
            // TODO
            return;
        }
    }

    protected abstract String getFormatType();

    public MediaFormat getFormat() {
        return mFormat;
    }

    public int getmWidth() {
        return mWidth;
    }

    public int getmHeight() {
        return mHeight;
    }

    public long getmDuration() {
        return mDuration;
    }

    /**
     * Called when an input buffer becomes available.
     *
     * @param index The index of the available input buffer.
     */
    void onInputBufferAvailable(int index,ByteBuffer inputBuffer) {
        int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
        if (sampleSize > 0) {
            mDecoder.queueInputBuffer(index,0,sampleSize,mExtractor.getSampleTime(),0);
        } else {    // 编码结束传递一个EOF标记
            mDecoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        }
        mDecoder.flush();
    }

    /**
     * Called when an output buffer becomes available.
     *
     * @param index The index of the available output buffer.
     * @param bufferInfo Info regarding the available output buffer {@link MediaCodec.BufferInfo}.
     */
    void onOutputBufferAvailable( int index,  MediaCodec.BufferInfo bufferInfo,ByteBuffer outputBuffer){
        outputBuffer.position(bufferInfo.offset);
        mDecoder.releaseOutputBuffer(index, render);
    }

    /**
     * Called when the MediaCodec encountered an error
     *
     * @param e The {@link MediaCodec.CodecException} object describing the error.
     */
    void onError(MediaCodec.CodecException e){

    }

    /**
     * Called when the output format has changed
     *
     * @param format The new output format.
     */
    void onOutputFormatChanged( MediaFormat format) {
        mFormat = format;
    }

    void onClose() {
        mFormat = null;
        mDecoder.release();
    }

}
