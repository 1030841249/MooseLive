package com.live.mooselive.av.camera;


import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.view.SurfaceHolder;

import com.live.mooselive.App;
import com.live.mooselive.av.encoder.CameraEncoder;
import com.live.mooselive.utils.LogUtil;
import com.live.mooselive.utils.RTMPUtil;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK;
import static android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT;

public class CameraLive {
    private static final String TAG = "CameraUtil";

    private int mCameraId = CAMERA_FACING_BACK;
    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private int mWidth = 1920;
    private int mHeight = 1080;
    private byte[] mPreviewBuffer;
    CameraEncoder mCameraEncoder;
    private ScheduledExecutorService mScheduledExecutorService;

    private Camera.PreviewCallback mPreviewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (mCamera != null) {
                mCamera.addCallbackBuffer(mPreviewBuffer);
            }
            if (mCameraEncoder != null) {
                mCameraEncoder.addFrame(data);
            }
        }
    };

    public CameraLive(SurfaceHolder surfaceHolder) {
        mScheduledExecutorService = Executors.newScheduledThreadPool(2);
        if (!checkCamera()) {
            return;
        }
        mSurfaceHolder =surfaceHolder;
        initCamera();
        mCameraEncoder = new CameraEncoder(mWidth,mHeight);
    }

    private void initCamera() {
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

    public void initParameters() {
        Camera.Parameters parameters = mCamera.getParameters();
        if (mCameraId == CAMERA_FACING_BACK) { // 前置摄像头会自动对焦，设置该参数会报错
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setPreviewSize(mWidth,mHeight);
        parameters.setPreviewFormat(ImageFormat.NV21);
        mCamera.setParameters(parameters);
        initPreviewBuffer();
    }

    private void initPreviewBuffer() {
//        Camera.Size size = mCamera.getParameters().getPreviewSize();
//        mWidth = size.width;
//        mHeight = size.height;
        mPreviewBuffer = new byte[mWidth * mHeight * 3 / 2];
        mCamera.addCallbackBuffer(mPreviewBuffer);
    }

    private boolean checkCamera() {
        return App.getInstance().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public void switchCamera() {
        stopPreview();
        mCameraId = mCameraId == 0 ? 1 : 0;
        initCamera();
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void startLive(String url) {
        mScheduledExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (RTMPUtil.connectRTMP(url) == 1) {
                    LogUtil.e(TAG,"Camera 连接RTMP成功");
                    mCameraEncoder.run();
                } else {
                    LogUtil.e(TAG,"Camera 连接RTMP失败");
                }
            }
        });
    }

    public void setPreviewCallback(Camera.PreviewCallback previewCallback) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(previewCallback);
        }
    }

    public static void setCameraDisplayOrientation(
            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
//        int rotation = activity.getWindowManager().getDefaultDisplay()
//                .getRotation();
        int degrees = 0;
//        switch (rotation) {
//            case Surface.ROTATION_0:
//                degrees = 0;
//                break;
//            case Surface.ROTATION_90:
//                degrees = 90;
//                break;
//            case Surface.ROTATION_180:
//                degrees = 180;
//                break;
//            case Surface.ROTATION_270:
//                degrees = 270;
//                break;
//        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(90);
    }
}
