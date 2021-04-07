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
    public static byte[] rotateYUVDegree90(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }
        // Rotate the U and V color components
        i = imageWidth * imageHeight * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i--;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i--;
            }
        }
        return yuv;
    }

    //前置旋转270度；
    public static byte[] rotateYUVDegree270(byte[] data, int imageWidth, int imageHeight) {
        byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                yuv[i] = data[y * imageWidth + x];
                i++;
            }
        }// Rotate the U and V color components
        i = imageWidth * imageHeight;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                i++;
                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                i++;
            }
        }
        return yuv;
    }

    public static byte[] frameMirror(byte[] data, int width, int height) {
        byte tempData;
        for (int i = 0; i < height * 3 / 2; i++) {
            for (int j = 0; j < width / 2; j++) {
                tempData = data[i * width + j];
                data[i * width + j] = data[(i + 1) * width - 1 - j];
                data[(i + 1) * width - 1 - j] = tempData;
            }

        }
        return data;
    }

    public static byte[] frameNV21Mirror(byte[] src, int w, int h) { //src是原始yuv数组
        int i;
        int index;
        byte temp;
        int a, b;
        //mirror y
        for (i = 0; i < h; i++) {
            a = i * w;
            b = (i + 1) * w - 1;
            while (a < b) {
                temp = src[a];
                src[a] = src[b];
                src[b] = temp;
                a++;
                b--;
            }
        }

        // mirror u and v
        index = w * h;
        for (i = 0; i < h / 2; i++) {
            a = i * w;
            b = (i + 1) * w - 2;
            while (a < b) {
                temp = src[a + index];
                src[a + index] = src[b + index];
                src[b + index] = temp;

                temp = src[a + index + 1];
                src[a + index + 1] = src[b + index + 1];
                src[b + index + 1] = temp;
                a+=2;
                b-=2;
            }
        }
        return src;
    }
}
