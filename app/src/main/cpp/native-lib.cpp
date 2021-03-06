#include <jni.h>
#include <string>
#include <rtmp.h>
#include <log.h>
#include <pthread.h>
#include <android/log.h>
#include <opencl-c-base.h>
#include "Log.h"

//RTMP *rtmp = NULL;
JavaVM *g_jvm;
jobject g_obj;

typedef struct {
    int16_t sps_len;
    int16_t pps_len;
    int8_t *sps;
    int8_t *pps;
    RTMP *rtmp;
} Live;

Live *live;



int sendPacket(Live *live, RTMPPacket* packet) {
    int ret = -1;
    if (live && live->rtmp && RTMP_IsConnected(live->rtmp)) {
        // 防止关闭RTMP连接时报错
        packet->m_nInfoField2 = live->rtmp->m_stream_id;
        ret = RTMP_SendPacket(live->rtmp, packet, 0);
    }
    RTMPPacket_Free(packet);
    free(packet);    //释放内存
    return ret;
}

// MediaCodec中，sps和 pps数据在一起，所以为了发送分割 sps 和 pps
void splitSpsPps(Live *live,int8_t *buf,int len) {
    // 7 sps
    // 8 pps
    for (int i = 0; i < len;i++) {
//        LOGE("SPS&PPS : %d", buf[i]);
        if (i + 4 < len) { // 忽略起始码
            // 通过起始码（0X00 00 00 01）区分 NAL 单元
            if(buf[i] == 0x00 && buf[i+1] == 0x00
               && buf[i+2] == 0x00 && buf[i+3] == 0x01) {

                if ((buf[i + 4] & 0x1F) == 8) { // pps
                    // sps 数据已经确定
                    live->sps_len = i - 4;
                    live->sps = static_cast<int8_t *>(malloc(live->sps_len));
                    memcpy(live->sps, buf+4, live->sps_len);
                    live->pps_len = len - (4 + live->sps_len) - 4;
                    live->pps = static_cast<int8_t *>(malloc(live->pps_len));
                    memcpy(live->pps, buf + 4 + live->sps_len + 4, live->pps_len);
                    LOGE("SPS&PPS 解析完成 总长：%d SPS 长：%d   PPS 长：%d",len,live->sps_len,live->pps_len);
                    break;
                }
            }
        }
    }

}

int sendVideoSpsPps(Live *live) {
    int body_size = 16 + live->sps_len + live->pps_len;
    RTMPPacket * packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, body_size);
    int i = 0;
    // 关键帧 0001 AVC编码 0111
    packet->m_body[i++] = 0x17;
    // 帧类型 , AVC sequence header 0
    packet->m_body[i++] = 0x00;
    // CTS，帧类型不为 NALU 时为 0
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;
    packet->m_body[i++] = 0x00;

    // avc sequence header
    /*AVCDecoderConfigurationRecord*/
    packet->m_body[i++] = 0x01;           // configurationVersion 固定版本 1
    packet->m_body[i++] = live->sps[1];   // AVCProfileIndication
    packet->m_body[i++] = live->sps[2];   // profile_compatibility
    packet->m_body[i++] = live->sps[3];   // AVCLevelIndication
    packet->m_body[i++] = 0xff;           // Reserved（占6位） 111111
    // + lengthSizeMinusOne（占2位,表示nalu长度表示字节数，总为 4 ） 11

    /*sps*/
    packet->m_body[i++] = 0xe1;   // reserved(占3位） 111 + numOfSPS（占5位）总为 1
    packet->m_body[i++] = (live->sps_len >> 8) & 0xff;    // spsLength 长 2 字节
    packet->m_body[i++] = live->sps_len & 0xff;
    memcpy(&packet->m_body[i],live->sps,live->sps_len);     // sps
    i +=  live->sps_len;

    /*pps*/
    packet->m_body[i++]   = 0x01;   // numOfPPS
    packet->m_body[i++] = (live->pps_len >> 8) & 0xff;    // ppsLength 长 2 字节
    packet->m_body[i++] = (live->pps_len) & 0xff;
    memcpy(&packet->m_body[i],live->pps,live->pps_len);     // pps

    // video
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_nChannel = 0x04;
    packet->m_nTimeStamp = 0;
    packet->m_hasAbsTimestamp = 0;
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    /*调用发送接口*/
    return sendPacket(live,packet);
}

/**
 * 发送 H.264 格式数据
 * @param buf 编码后的 H.264 数据，包含起始码
 * @param size 数据长度
 * @param bIsKeyFrame 是否为关键帧
 * @param live sps和pps 信息
 * @return
 */
int sendH264Packet(int8_t *buf, int len,long tms){
    /*去掉帧界定符*/
    if (buf[2] == 0x00) { /*00 00 00 01*/
        buf += 4;
        len -= 4;
    } else if (buf[2] == 0x01) { /*00 00 01*/
        buf += 3;
        len -= 3;
    }

    int body_size = len + 9;
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, body_size); // 9 个字节表示 Tag header + NALUs 的长度

    int type = buf[0] & 0x1f;
    // 是否为关键帧（1/2）、AVC格式（7）
    packet->m_body[0] = 0x27;
    if (type == 5) {
        packet->m_body[0] = 0x17;
    }

    // AVC NALU
    packet->m_body[1] = 0x01;
    // cts
    packet->m_body[2] = 0x00;
    packet->m_body[3] = 0x00;
    packet->m_body[4] = 0x00;
    // NALU size
    packet->m_body[5] = (len >> 24) & 0xff;
    packet->m_body[6] = (len >> 16) & 0xff;
    packet->m_body[7] = (len >> 8) & 0xff;
    packet->m_body[8] = (len) & 0xff;
    // NALU data
    memcpy(&packet->m_body[9], buf, len);
    // header
    packet->m_packetType = RTMP_PACKET_TYPE_VIDEO;
    packet->m_nBodySize = body_size;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = tms;
    packet->m_nChannel = 0x04; // csid
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return sendPacket(live,packet);
}

/**
 * 发送数据到 RTMP 前，剔除起始码数据（0x00 00 00 01）
 * @param buf
 * @param len
 * @param tms
 * @return
 */
int sendVideoData(int8_t *buf, int len, long tms) {
    int ret = 0;
    // nal type field in the nalu header
    // 低 5 位
    // 发送关键帧之前发送 sps 和 pps
    if ((buf[4] & 0x1F) == 7) { // sps + pps, MediaCodec 编码出来的 sps 和 pps 在一起
        // 分割并保存 sps 和 pps
        LOGE("解析 SPS&PPS");
        splitSpsPps(live, buf, len);
        sendVideoSpsPps(live);
    } else {
        if ((buf[4] & 0x1F) == 5) { // 关键帧
            // 发送 sps 和 pps
            ret = sendVideoSpsPps(live);
            if (!ret) {
                LOGE("ERROR sendVideoSpsPps");
                return -1;
            }
        }
        // 发送帧
        ret = sendH264Packet(buf,len,tms);
        if (!ret) {
            LOGE("ERROR sendH264Packet");
            return ret;
        }
    }
    return ret;
}

/**
 *
 * @param data
 * @param len
 * @param type 音频类型，1解码 2数据
 * @param tms
 * @return
 */
int sendAudioHeader(int8_t *data, int len, long tms) {
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, len + 2); // 2 字节的音频 Tag HEADER

    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x00;
    memcpy(&packet->m_body[2], data, len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = len+2;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = 0;
    packet->m_nChannel = 0x05; // 视频 04，音频 05
    packet->m_headerType = RTMP_PACKET_SIZE_MEDIUM;
    return sendPacket(live,packet);
}

/**
 *
 * @param data
 * @param len
 * @param type 音频类型，1解码 2数据
 * @param tms
 * @return
 */
int sendAudioData(int8_t *data, int len, long tms) {
    RTMPPacket *packet = static_cast<RTMPPacket *>(malloc(sizeof(RTMPPacket)));
    RTMPPacket_Alloc(packet, len + 2); // 2 字节的音频 Tag HEADER

    packet->m_body[0] = 0xAF;
    packet->m_body[1] = 0x01;
    memcpy(&packet->m_body[2], data, len);

    packet->m_packetType = RTMP_PACKET_TYPE_AUDIO;
    packet->m_nBodySize = len+2;
    packet->m_hasAbsTimestamp = 0;
    packet->m_nTimeStamp = tms;
    packet->m_nChannel = 0x05; // 视频 04，音频 05
    packet->m_headerType = RTMP_PACKET_SIZE_LARGE;
    return sendPacket(live,packet);
}

void sendData(int8_t  type,int8_t* buf,int len,long tms) {
    switch (type) {
        case 0: // video
            sendVideoData(buf, len, tms);
            break;
        case 1: // aac header
            sendAudioHeader(buf, len, tms);
            break;
        case 2: // aac data
            sendAudioData(buf, len, tms);
            break;
    }
}

void yuv_rotate_90(int8_t *des,int8_t *src,int width,int height)
{
    int n = 0;
    int hw = width>>1;
    int hh = height>>1;
    int size = width * height;
    int hsize = size>>2;

    int pos = 0;
    //copy y
    for(int j = 0; j < width;j++)
    {
        pos = size;
        for(int i = height - 1; i >= 0; i--)
        {	pos-=width;
            des[n++] = src[pos + j];
        }
    }
    //copy uv
    int8_t *ptemp = src + size;
    int m = n + hsize;
    for(int j = 0;j < hw;j++)
    {  	pos= hsize;
        for(int i = hh - 1;i >= 0;i--)
        {
            pos-=hw;
            des[n++] = ptemp[ pos + j ];
            des[m++] = ptemp[ pos + j+ hsize ];
        }
    }
}
extern "C" {
    JNIEXPORT jint JNICALL
    Java_com_live_mooselive_utils_RTMPUtil_connectRTMP(JNIEnv *env, jclass clazz, jstring url) {
        const char *rtmpUrl = env->GetStringUTFChars(url, 0);

        live = static_cast<Live *>(malloc(sizeof(Live)));
        live->rtmp = RTMP_Alloc();
        RTMP_Init(live->rtmp);
        live->rtmp->Link.timeout = 10;
        if (!RTMP_SetupURL(live->rtmp, (char *) rtmpUrl)) {
            LOGE("RTMP_设置连接失败");
            return -1;
        }

        RTMP_EnableWrite(live->rtmp); // 推送

        if (!RTMP_Connect(live->rtmp, 0)) {
            LOGE("RTMP_连接失败");
            RTMP_Free(live->rtmp);
            return -1;
        }
        if (!RTMP_ConnectStream(live->rtmp, 0)) {
            LOGE("RTMP_创建流失败");
            RTMP_Close(live->rtmp);
            RTMP_Free(live->rtmp);
            return -1;
        }
        LOGE("RTMP_连接成功");

        env->ReleaseStringUTFChars(url, rtmpUrl);
        return 1;
    }

    JNIEXPORT jint JNICALL
    Java_com_live_mooselive_utils_RTMPUtil_closeRTMP(JNIEnv *env, jclass clazz) {
        if (live) {
            if (live->rtmp) {
                if (RTMP_IsConnected(live->rtmp)) {
                    RTMP_Close(live->rtmp);
                }
                RTMP_Free(live->rtmp);
            }
            live->rtmp = nullptr;
            free(live);
        }
        live = nullptr;
        return 1;
    }

    JNIEXPORT void JNICALL
    Java_com_live_mooselive_utils_RTMPUtil_sendData(JNIEnv *env, jclass clazz, jint type,
                                                    jbyteArray data, jint len, jlong tms) {
        LOGE("SEND Data");
        jbyte *buf = env->GetByteArrayElements(data, 0);
        sendData(type,buf,len,tms);
        env->ReleaseByteArrayElements(data, buf, 0);
    }

    JNIEXPORT void JNICALL
    Java_com_live_mooselive_utils_RTMPUtil_sendDataNeedRotate(JNIEnv *env, jclass clazz, jint type,
                                                    jbyteArray data, jint len, jlong tms,jint width,jint height) {
        jbyte *buf = env->GetByteArrayElements(data, 0);
        int8_t *newBuf = buf;
        yuv_rotate_90(newBuf,buf,width,height);
        sendData(type,newBuf,len,tms);
        env->ReleaseByteArrayElements(data, buf, 0);
    }


}
extern "C"
JNIEXPORT jint JNICALL
Java_com_live_mooselive_utils_CameraUtil_connectRTMP(JNIEnv *env, jclass clazz, jstring url) {
    const char *rtmpUrl = env->GetStringUTFChars(url, 0);

    live = static_cast<Live *>(malloc(sizeof(Live)));
    live->rtmp = RTMP_Alloc();
    RTMP_Init(live->rtmp);
    live->rtmp->Link.timeout = 10;
    if (!RTMP_SetupURL(live->rtmp, (char *) rtmpUrl)) {
        LOGE("RTMP_设置连接失败");
        return -1;
    }

    RTMP_EnableWrite(live->rtmp); // 推送

    if (!RTMP_Connect(live->rtmp, 0)) {
        LOGE("RTMP_连接失败");
        RTMP_Free(live->rtmp);
        return -1;
    }
    if (!RTMP_ConnectStream(live->rtmp, 0)) {
        LOGE("RTMP_创建流失败");
        RTMP_Close(live->rtmp);
        RTMP_Free(live->rtmp);
        return -1;
    }
    LOGE("RTMP_连接成功");

    env->ReleaseStringUTFChars(url, rtmpUrl);
    return 1;
}