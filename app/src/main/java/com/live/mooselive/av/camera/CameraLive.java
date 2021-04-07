package com.live.mooselive.av.camera;


import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.ImageView;

import com.live.mooselive.App;
import com.live.mooselive.av.encoder.CameraEncoder;
import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.RTMPUtil;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class CameraLive {
    private static final String TAG = "CameraUtil";

    private int mCameraId = CAMERA_FACING_BACK;
    private Camera mCamera;
    private Activity mActivity;
    private SurfaceHolder mSurfaceHolder;
    private int mWidth = 1920;
    private int mHeight = 1080;
    private byte[] mPreviewBuffer;
    CameraEncoder mCameraEncoder;

    private ScheduledExecutorService mScheduledExecutorService;
    private Future mCameraTask;
    private boolean isStart = false;

    private String url;

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
//            LogUtil.e(TAG,"onPreviewFrame");
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mPreviewBuffer);
            }
            if (mCameraEncoder != null && isStart) {
                mCameraEncoder.addFrame(data);
            }
        }
    };


    public CameraLive(Activity activity,SurfaceHolder surfaceHolder) {
        mScheduledExecutorService = Executors.newScheduledThreadPool(2);
        if (!checkCamera()) {
            return;
        }
        mActivity = activity;
        mSurfaceHolder =surfaceHolder;
        initCamera();
    }

    private void initCamera() {
        initEncoder();
        mCamera = Camera.open(mCameraId);
        setCameraDisplayOrientation(mCameraId,mCamera);
        initParameters();
        mCamera.setPreviewCallbackWithBuffer(mPreviewCallback);
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCamera.startPreview();
    }

    private void initEncoder() {
        mCameraEncoder = new CameraEncoder(mWidth, mHeight);
        mCameraEncoder.setCameraType(mCameraId);
    }

    public void initParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (mCameraId == CAMERA_FACING_BACK) { // 前置摄像头会自动对焦，设置该参数会报错
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setPreviewSize(mWidth,mHeight);
        parameters.setPictureSize(mWidth,mHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
        initPreviewBuffer();
    }

    private void initPreviewBuffer() {
        Camera.Size size = mCamera.getParameters().getPreviewSize();
//        mWidth = size.width;
//        mHeight = size.height;
//        mPreviewBuffer = new byte[mWidth * mHeight * 4];
        mPreviewBuffer =  new byte[mWidth * mHeight * 3 / 2];
//        mPreviewBuffer =  new byte[mWidth * mHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
        mCamera.addCallbackBuffer(mPreviewBuffer);
    }

    private boolean checkCamera() {
        return App.getInstance().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void switchCamera() {
        stopPreview();
        mCameraId = mCameraId == 0 ? 1 : 0;
        initCamera();
//        startLive(url);
    }

    public void startLive(String url) {
        this.url = url;
        mCameraTask = mScheduledExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                if (RTMPUtil.connectRTMP(url) == 1) {
                    LogUtil.e(TAG,"Camera 连接RTMP成功");
                    isStart = true;
                    mCameraEncoder.run();
                } else {
                    LogUtil.e(TAG,"Camera 连接RTMP失败");
                }
            }
        });
    }

    public void stopLive() {
        stopPreview();
        initCamera();
    }

    public void stopPreview() {
        isStart = false;
        if (mCamera != null) {
            if (mCameraTask != null) {
                mCameraTask.cancel(true);
            }
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
        if (mCameraEncoder != null) {
            mCameraEncoder.stop();
            mCameraEncoder.cleanFrame();
            mCameraEncoder = null;
        }
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    public void setCameraDisplayOrientation(
            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = mActivity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }
}
