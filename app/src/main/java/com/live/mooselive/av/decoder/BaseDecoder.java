package com.live.mooselive.av.decoder;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.live.mooselive.App;
import com.live.mooselive.R;
import com.live.mooselive.utils.LogUtil;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class BaseDecoder implements Runnable {

    static long S_SYNC_TIME = 0;

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
//            AssetFileDescriptor assetFileDescriptor = App.getInstance().getResources().openRawResourceFd(R.raw.test);
//            mExtractor.setDataSource(assetFileDescriptor);
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
                    if (mWidth == 0) {
//                        mFormat.setInteger(MediaFormat.KEY_WIDTH, 1024);
                    }
                    if (mHeight == 0) {
//                        mFormat.setInteger(MediaFormat.KEY_HEIGHT, 576);
                    }
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
            LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    请求输入队列");
            inputIndex = mDecoder.dequeueInputBuffer(1000);
            if (inputIndex >= 0) {
                LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    将待解码数据填充进队列");
                ByteBuffer inputBuffer = mInputBuffers[inputIndex];
                onInputBufferAvailable(inputIndex,inputBuffer);
            }
            LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    请求解码队列");
            bufferInfo = new MediaCodec.BufferInfo();
            outputIndex = mDecoder.dequeueOutputBuffer(bufferInfo, 1000);
            LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    outputIndex:  " + outputIndex);
            if (outputIndex >= 0) {
                break;
            }
            switch (outputIndex) {
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    INFO_TRY_AGAIN_LATER");
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    LogUtil.e(TAG, mFormat.getString(MediaFormat.KEY_MIME) + "    INFO_OUTPUT_FORMAT_CHANGED");
//                    mFormat = mDecoder.getOutputFormat();
                    break;
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    mOutputBuffers = mDecoder.getOutputBuffers();
                    break;
                default:
                    break;
            }
        }
        sleepRender();
        if (outputIndex >= 0) {
            LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    使用解码后的数据");
            ByteBuffer outputBuffer = mOutputBuffers[outputIndex];
            onOutputBufferAvailable(outputIndex, bufferInfo,outputBuffer);
        }
//        LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    surface state : " +);
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
        // 偏移量小于总读取数
        if (bufferInfo.offset < bufferInfo.size) {
            LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    偏移量小于总读取数，通知渲染");
            outputBuffer.position(bufferInfo.offset);
            renderData(bufferInfo,outputBuffer);
        }
        LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    start 释放空间");
        mDecoder.releaseOutputBuffer(index, render);
        LogUtil.e(TAG,mFormat.getString(MediaFormat.KEY_MIME) +"    end 释放空间");
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
        if (!render) {
            // audio
            S_SYNC_TIME = getPTS();
        }
        long curTime = S_SYNC_TIME;
        if (curTime < getPTS()) {
            // 未达到显示时间，休眠这个差量值
            LogUtil.e(TAG, mFormat.getString(MediaFormat.KEY_MIME) + "    sleepRender : " + getPTS() + " - " + curTime + " = " + (getPTS() - curTime));
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

    public void finish() {
        mCurState = DecodeState.FINISH;
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
