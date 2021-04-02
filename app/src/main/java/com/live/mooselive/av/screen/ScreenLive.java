package com.live.mooselive.av.screen;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.live.mooselive.R;
import com.live.mooselive.av.bean.RTMPPacket;
import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.RTMPUtil;
import com.live.mooselive.utils.ToastUtil;

import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;
import static com.live.mooselive.utils.RTMPUtil.*;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenLive implements Runnable {

    public static final String TAG = "ScreenLive";

    private static final int REQUEST_CODE = 0X01;

    public interface ScreenCodecCallback {
        void onEncodedVideo(byte[] data, long tms);

        void onEncodedAudio(byte[] data, long tms);

        void onFormatChanged(MediaFormat mediaFormat);
    }

    static {
        System.loadLibrary("native-lib");
    }

    private MediaProjectionManager mediaProjectionManager = null;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;

    private ScreenCodecCallback mCallback;
    private ScreenVideo mScreenVideo;
    private ScreenAudio mScreenAudio;

    /*音视频同步*/
    private Handler mTaskHandler;
    private LinkedList<RTMPPacket> mAudioFrames, mVideoFrames;
    // 判断音频和视频帧的时间戳差异，并发送到RTMP
    private Runnable mTask;
    private float mPTSGen = 0;
    private float mOffset = 20;


    private Handler mCountTimeHander;
    private ScheduledExecutorService mScheduledExecutorService;
    private View mTimeRootView;
    private TextView tvTime;
    private ImageView ivStart, ivStop;
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;

    private String mUrl;
    private int width, height;

    SimpleDateFormat simpleDateFormat;
    private long mStartTime = 0;
    // 已缩减的视频帧时间戳，之后的视频帧pts都需要在减去这个数值后计算，避免时间戳修改后还是使用原时间戳计算
    public static long mModifyVideoTime;

    Context mContext;

    boolean isFirstAudio = true;

    public ScreenLive(Context mContext, String url) {
        this.mContext = mContext;
        mUrl = url;
        mCountTimeHander = new Handler();
        mAudioFrames = new LinkedList<>();
        mVideoFrames = new LinkedList<>();
        mCallback = new ScreenCodecCallback() {
            @Override
            public void onEncodedVideo(byte[] data, long tms) {
                mVideoFrames.add(new RTMPPacket(RTMP_TYPE_VIDEO, data, data.length, tms));
            }

            @Override
            public void onEncodedAudio(byte[] data, long tms) {
                if (isFirstAudio) {
                    isFirstAudio = false;
                    mAudioFrames.add(new RTMPPacket(RTMP_TYPE_ADUIO_HEADER, new byte[]{(byte) 0x11, (byte) 0x90}, 2, 0));
                }
                mAudioFrames.add(new RTMPPacket(RTMP_TYPE_AUDIO_DATA, data, data.length, tms));
            }

            @Override
            public void onFormatChanged(MediaFormat mediaFormat) {
//                ByteBuffer byteBuffer = mediaFormat.getByteBuffer("csd-0");
//                byte[] sps = new byte[byteBuffer.remaining()];
//                byteBuffer.get(sps);
//                byteBuffer = mediaFormat.getByteBuffer("csd-1");
//                byte[] pps = new byte[byteBuffer.remaining()];
//                byteBuffer.get(pps);
//                byte[] buffer = new byte[sps.length + pps.length];
//                System.arraycopy(sps, 0, buffer, 0, sps.length);
//                System.arraycopy(pps, 0, buffer, sps.length, pps.length);
//                sendSPSPPS(buffer,buffer.length);
            }
        };
        mTask = new Runnable() {
            @Override
            public void run() {
                while(true) {
                    // 视频帧向音频帧靠拢
                    RTMPPacket video = null;
                    RTMPPacket audio = null;
                    if (!mVideoFrames.isEmpty()) {
                        video = mVideoFrames.getFirst();
                    }
                    if (!mAudioFrames.isEmpty()) {
                        audio = mAudioFrames.getFirst();
                    }
                    if (video != null && audio != null) {
                        float diff = 0;
                        if ((video.tms - audio.tms) >= 60L) {
                            diff = ((video.tms - audio.tms)/10*8);
                            LogUtil.e(TAG,"video&audio difference " + ((video.tms - audio.tms)/10*8));
                            mModifyVideoTime += diff;
                            video.tms -= diff;
                        }
                        // 时间戳小的先发送
                        if (video.tms < audio.tms) {
//                            LogUtil.e(TAG,"发送视频帧 tms " + video.tms);
                            mVideoFrames.removeFirst();
                            RTMPUtil.sendData(video.type, video.data, video.len, video.tms);
                        } else {
//                            LogUtil.e(TAG,"发送音频帧 tms " + audio.tms);
                            mAudioFrames.removeFirst();
                            RTMPUtil.sendData(audio.type, audio.data, audio.len, audio.tms);
                        }
                    }
                }
            }
        };
        initTimeView();
    }

    private void initTimeView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(mContext)) {
                showTopView();
            }
        }
    }

    private void initScreenCodec() {
        mScreenVideo = new ScreenVideo(width, height, mCallback);
        mScreenAudio = new ScreenAudio(mCallback);
    }

    private void initMediaProjection() {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
//        width = displayMetrics.widthPixels;
//        height = displayMetrics.heightPixels;
        width = 1920;
        height = 1080;
        mediaProjectionManager = (MediaProjectionManager) mContext.getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent screenIntent = mediaProjectionManager.createScreenCaptureIntent();
        ((Activity) mContext).startActivityForResult(screenIntent, REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                initScreenCodec();
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                if (mediaProjection != null) {
                    DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
                    virtualDisplay = mediaProjection.createVirtualDisplay("-Anchor", width, height, displayMetrics.densityDpi,
                            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mScreenVideo.getSurface(), null, null);
                    mScheduledExecutorService = Executors.newScheduledThreadPool(2);
                    mScheduledExecutorService.execute(this);
                }
            }
        }
    }

    @Override
    public void run() {
        if (RTMPUtil.connectRTMP(mUrl) == 1) {
            ToastUtil.showShortToast("RTMP推流连接成功");
            LogUtil.e("ScreenLive", "RTMP 连接成功");
            // 连接成功后开始编码，并传输数据
            updateTime();
            mScreenAudio.start();
            mScreenVideo.start();
            mScheduledExecutorService.execute(mTask);
        } else {
            ToastUtil.showShortToast("RTMP推流连接失败");
            LogUtil.e("ScreenLive", "RTMP 连接失败");
        }
    }

    /**
     * 连接到 RTMP 地址，开始直播
     */
    public void startLive() {
        initMediaProjection();
    }

    public void stopLive() {
        isFirstAudio = true;
        mStartTime = 0;
        RTMPUtil.closeRTMP();
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (mScreenVideo != null) {
            mScreenVideo.stop();
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        mScheduledExecutorService.shutdown();
    }

    private void showTopView() {
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));

        mTimeRootView = LayoutInflater.from(mContext).inflate(R.layout.window_top_layout, null);
        tvTime = mTimeRootView.findViewById(R.id.tv_window_top_time);
        ivStart = mTimeRootView.findViewById(R.id.iv_window_top_time_start);
        ivStop = mTimeRootView.findViewById(R.id.iv_window_top_time_stop);
        windowManager = ((Activity) mContext).getWindowManager();
        params = new WindowManager.LayoutParams();
        params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        params.format = PixelFormat.RGBA_8888;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        params.gravity = Gravity.TOP;
        params.width = (int) tvTime.getPaint().measureText(tvTime.getText().toString()) + 200;
        params.height = 100;
        mTimeRootView.setOnTouchListener(new View.OnTouchListener() {
            int lastX, lastY;
            int paramX, paramY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = (int) event.getRawX();
                        lastY = (int) event.getRawY();
                        paramX = params.x;
                        paramY = params.y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) event.getRawX() - lastX;
                        int dy = (int) event.getRawY() - lastY;
                        params.x = paramX + dx;
                        params.y = paramY + dy;
                        // 更新悬浮窗位置
                        windowManager.updateViewLayout(mTimeRootView, params);
                        break;
                }
                return true;
            }
        });
        ivStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivStart.setVisibility(View.GONE);
                ivStop.setVisibility(View.VISIBLE);
                startLive();
            }
        });
        ivStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ivStart.setVisibility(View.VISIBLE);
                ivStop.setVisibility(View.GONE);
                stopLive();
            }
        });
        windowManager.addView(mTimeRootView, params);
    }

    private void updateTime() {
        mStartTime = System.currentTimeMillis();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (mStartTime == 0) { // 突然关闭
                    return;
                }
                String format = simpleDateFormat.format(System.currentTimeMillis() - mStartTime);
                tvTime.setText(format);
                mCountTimeHander.postDelayed(this, 1000);
            }
        };
        mCountTimeHander.postDelayed(runnable, 1000);
    }

}
