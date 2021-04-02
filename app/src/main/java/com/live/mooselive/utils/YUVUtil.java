package com.live.mooselive.utils;

public class YUVUtil {
    /**
     * NV12 : YYYY UVUV
     * NV21 : YYYY VUVU
     * @param nv21
     * @return
     */
    public static byte[] convertNV21ToNV12(byte[] nv21,int width,int height) {
        int length = nv21.length;
        byte[] nv12 = new byte[length];
        // Y
        System.arraycopy(nv21, 0, nv12, 0, width * height);
        int size = width * height;
        for (int i = 0; i < size / 4; i ++) {
            nv12[size + i * 2] = nv21[size + i * 2 + 1];
            nv12[size + i * 2 + 1] = nv21[size + i * 2];
        }
        return nv12;
    }
//
    public static byte[] rotateYUV240SP(byte[] src,int width,int height) {
        byte[] des = new byte[src.length];
        int wh = src.length / 2;
        //旋转Y
        int k = 0;
        for(int i=0;i<width;i++) {
            for(int j=0;j<height;j++)
            {
                des[k] = src[width*j + i];
                k++;
            }
        }

        for(int i=0;i<width/2;i++) {
            for(int j=0;j<height/2;j++)
            {
                des[k] = src[wh+ width/2*j + i];
                des[k+width*height/4]=src[wh*5/4 + width/2*j + i];
                k++;
            }
        }
        return des;
    }
}
