package com.live.mooselive.utils;

public class RTMPUtil {
    public static final int RTMP_TYPE_VIDEO = 0X00;
    public static final int RTMP_TYPE_ADUIO_HEADER = 0X01;
    public static final int RTMP_TYPE_AUDIO_DATA = 0X02;

    static {
        System.loadLibrary("native-lib");
    }

    public static final native int connectRTMP(String url);

    public static final native int closeRTMP();

    public static final native void sendData(int type, byte[] data, int len, long tms);
}
