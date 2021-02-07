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

    private final Object mLock = new Object();

    MediaCodec mDecoder;
    MediaExtractor mExtractor;
    MediaFormat mFormat;
    Surface mSurface;
    ByteBuffer[] mInputBuffers, mOutputBuffers;

    private int mWidth;
    private int mHeight;
    private long mDuration;

    private int mTrackIndex;

    private DecodeState mCurState;
    boolean render = false;
    // 音视频同步，以系统时间为准
    protected long mStartTimeForSync = 0;

    public BaseDecoder(String path, Surface surface) {
        mSurface = surface;
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        init();
    }

    public BaseDecoder(MediaExtractor mExtractor,Surface surface) {
        this.mExtractor = mExtractor;
        mSurface = surface;
        init();
    }

    private void init() {
        if (mSurface != null) {
            render = true;
        }
        initFormat();
        initDecoder();
        mCurState = DecodeState.RUNNING;
        mInputBuffers = mDecoder.getInputBuffers();
        mOutputBuffers = mDecoder.getOutputBuffers();
    }

    @Override
    public void run() {
        mStartTimeForSync = System.currentTimeMillis();
        while(mCurState == DecodeState.RUNNING) {
            if (mCurState == DecodeState.PAUSE) {
                waitDecoder();
                mStartTimeForSync = System.currentTimeMillis() - getPTS();
            }
            onDecode();
        }
        close();
        processWork();
    }

    protected void initFormat() {
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = mExtractor.getTrackFormat(i);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith(getFormatType())) {
                mTrackIndex = i;
                mFormat = mediaFormat;
                if (mime.startsWith("video/")) {
                    mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                    mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
                }
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
//            initCallback();
            mDecoder.configure(mFormat, mSurface, null, 0);
            mDecoder.start();
            mCurState = DecodeState.START;
        } catch (IOException e) {
            LogUtil.e(TAG,"解码器创建失败！");
            e.printStackTrace();
        }
    }

    private void initCallback() {
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
    }

    protected void onDecode() {
        mExtractor.selectTrack(mTrackIndex);
        MediaCodec.BufferInfo bufferInfo;
        int inputIndex;
        int outputIndex;
        while (true) {
            inputIndex = mDecoder.dequeueInputBuffer(1000);
            if (inputIndex >= 0) {
                ByteBuffer inputBuffer = mInputBuffers[inputIndex];
                onInputBufferAvailable(inputIndex,inputBuffer);
            }
            bufferInfo = new MediaCodec.BufferInfo();
            outputIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 1000);
            if (outputIndex >= 0) break;
            switch (outputIndex) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    mOutputBuffers = mDecoder.getOutputBuffers();
                    break;
            }
        }
        sleepRender();
        if (outputIndex >= 0) {
            ByteBuffer outputBuffer = mOutputBuffers[outputIndex];
            onOutputBufferAvailable(outputIndex, bufferInfo,outputBuffer);
        }
    }

    private void processWork() {
        switch (mCurState) {
            case PAUSE:

        }
    }

    /**
     * Called when an input buffer becomes available.
     *
     * @param index The index of the available input buffer.
     */
    void onInputBufferAvailable(int index,ByteBuffer inputBuffer) {
        int sampleSize = mExtractor.readSampleData(inputBuffer, 0);
        if (sampleSize >= 0) {
            mDecoder.queueInputBuffer(index,0,sampleSize,mExtractor.getSampleTime(),mExtractor.getSampleFlags());
            mExtractor.advance();
        } else {    // 编码结束传递一个EOF标记
            mDecoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            mCurState = DecodeState.PAUSE;
        }
    }

    /**
     * Called when an output buffer becomes available.
     *
     * @param index The index of the available output buffer.
     * @param bufferInfo Info regarding the available output buffer {@link MediaCodec.BufferInfo}.
     */
    void onOutputBufferAvailable( int index,  MediaCodec.BufferInfo bufferInfo,ByteBuffer outputBuffer){
        outputBuffer.position(bufferInfo.offset);
        renderData(bufferInfo,outputBuffer);
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
    }

    /**
     * 同步
     * 当前时间是否达到该帧的 PTS
     */
    private void sleepRender() {
        long curTime = System.currentTimeMillis() - mStartTimeForSync;
        if (curTime < getPTS()) {
            // 未达到显示时间，休眠这个差量值
            try {
                Thread.sleep(getPTS() - curTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * PTS
     * @return 微秒转换为毫秒
     */
    private long getPTS() {
        return mExtractor.getSampleTime() / 1000;
    }

    public void pause() throws InterruptedException {
        mCurState = DecodeState.PAUSE;
    }

    public void resume() {
        mCurState = DecodeState.RUNNING;
        synchronized (mLock) {
            mLock.notify();
        }
    }

    private void waitDecoder() {
        synchronized (mLock) {
            try {
                mLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    void close() {
        mFormat = null;
        mDecoder.release();
        mExtractor.release();
        mDecoder = null;
        mExtractor = null;
    }

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

    protected abstract String getFormatType();

    protected abstract void renderData(MediaCodec.BufferInfo bufferInfo, ByteBuffer byteBuffer);
}
