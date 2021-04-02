package com.live.mooselive.utils;

public class YUVUtil {
    /**
     * NV12 : YYYY UVUV
     * NV21 : YYYY VUVU
     * @param nv21
     * @return
     */
    public static byte[] convertNV21ToNV12(byte[] nv21) {
        int length = nv21.length;
        byte[] nv12 = new byte[length];
        // Y
        System.arraycopy(nv21, 0, nv12, 0, length / 2);
        int size = length / 2;
        for (int i = 0; i < size / 4; i ++) {
            nv12[size + i * 2] = nv21[size + i * 2 + 1];
            nv12[size + i * 2 + 1] = nv21[size + i * 2];
        }
        return nv12;
    }
}
