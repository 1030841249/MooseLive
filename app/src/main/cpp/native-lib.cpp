#include <jni.h>
#include <string>
#include <rtmp.h>
#include <log.h>
#include <pthread.h>
#include <android/log.h>
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


void receiveRTMPData(JNIEnv *env) {

    jclass cls = env->GetObjectClass(g_obj);
    jmethodID receiveRtmpData = env->GetMethodID(cls, "receiveRtmpData", "([B)V");

    int nRead = 0;
    int bufSize = 1024 * 1024 * 10;
    char *buf = (char *) malloc(bufSize);
    FILE *fp = fopen("/data/data/com.live.mooselive/receive.flv","wb");
    if (!fp) {
        __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "创建文件失败");
    }
    jbyteArray byteArray = env->NewByteArray(bufSize);
    int countReadSize = 0;
    int oneTagSize = 0;
//    while(nRead = RTMP_Read(rtmp,buf,bufSize)) {
//        env->SetByteArrayRegion(byteArray, 0, nRead, (jbyte *)buf);
//        env->CallVoidMethod(g_obj, receiveRtmpData,byteArray);
//        fwrite(buf, 1, nRead, fp);
//        countReadSize += nRead;
//
//
//
//        __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "本次读取数：%d  总读取数量：%d", nRead,
//                            countReadSize);
//    }

    free(buf);
}

void releaseRTMP() {
//    RTMP_Close(rtmp);
//    RTMP_Free(rtmp);
//    rtmp = NULL;
}

int connectRTMP(JNIEnv *env,char* url) {

//    if(rtmp != NULL) {
//        releaseRTMP();
//    }
//
//    bool isLiveStream = false;
//
//    rtmp =RTMP_Alloc();
//    RTMP_Init(rtmp);
//    rtmp->Link.timeout = 10;
//    if (!RTMP_SetupURL(rtmp, url)) {
//        RTMP_Free(rtmp);
//        return -1;
//    }
//
//    RTMP_SetBufferMS(rtmp, 3600 * 1000);
//
//    if (!RTMP_Connect(rtmp, NULL)) {
//        RTMP_Log(RTMP_LOGERROR, "NetConnect is Error");
//        RTMP_Free(rtmp);
//        return -1;
//    }
//    if(!RTMP_ConnectStream(rtmp,0)) {
//        RTMP_Log(RTMP_LOGERROR, "NetStream is Error");
//        RTMP_Free(rtmp);
//        RTMP_Close(rtmp);
//        return -1;
//    }


    __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "RTMP 连接成功");
    receiveRTMPData(env);
    return 0;
}

extern "C" {
JNIEXPORT void JNICALL
Java_com_live_mooselive_activity_RTMPActivity_connectRTMP(JNIEnv *env, jobject thiz, jstring url) {
    const char* rtmpUrl = env->GetStringUTFChars(url, NULL);
    g_obj = env->NewGlobalRef(thiz);
    char *tmp = const_cast<char *>(rtmpUrl);
    connectRTMP(env,tmp);
}

JNIEXPORT void JNICALL
Java_com_live_mooselive_activity_RTMPActivity_closeRTMP(JNIEnv *env, jobject thiz) {
//    if(rtmp) {
//        RTMP_Free(rtmp);
//        RTMP_Close(rtmp);
//        rtmp = nullptr;
//        __android_log_print(ANDROID_LOG_ERROR,"NATIVE","关闭RTMP成功 ");
//    } else {
//
//    }
}
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
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    /*调用发送接口*/
    int ret = RTMP_SendPacket(live->rtmp,packet,0);
    if(ret) {
        LOGE("SPS&PPS 发送成功");
    }
    RTMPPacket_Free(packet);
    free(packet);    //释放内存
    return ret;
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
    packet->m_nInfoField2 = live->rtmp->m_stream_id;

    int ret = RTMP_SendPacket(live->rtmp,packet,0);

    RTMPPacket_Free(packet);
    free(packet);
    return ret;
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
//            LOGE("发送SPS&PPS");
//            ret = sendVideoSpsPps(live);
//            if (!ret) {
//                LOGE("ERROR sendVideoSpsPps");
//                return -1;
//            }
        }
        // 发送帧
//        LOGE("发送视频帧 是否为关键帧：%d",isKey);
        ret = sendH264Packet(buf,len,tms);
        if (!ret) {
            LOGE("ERROR sendH264Packet");
            return ret;
        } else {
//            LOGE("发送视频帧完成");
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
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    int ret = RTMP_SendPacket(live->rtmp,packet,TRUE);
    RTMPPacket_Free(packet);
    free(packet);
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
    packet->m_nInfoField2 = live->rtmp->m_stream_id;
    int ret = RTMP_SendPacket(live->rtmp,packet,TRUE);
    RTMPPacket_Free(packet);
    free(packet);
    return ret;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_live_mooselive_av_screen_ScreenLive_sendSPSPPS(JNIEnv *env, jobject thiz, jbyteArray data,
                                                        jint len) {
    jbyte *buf = env->GetByteArrayElements(data, 0);
    if ((buf[4] & 0x1F) == 7) { // sps + pps, MediaCodec 编码出来的 sps 和 pps 在一起
        // 分割并保存 sps 和 pps
        LOGE("解析 SPS&PPS");
        splitSpsPps(live, buf, len);
        sendVideoSpsPps(live);
    }
}


extern "C" {
JNIEXPORT void JNICALL
Java_com_live_mooselive_av_screen_ScreenLive_sendData(JNIEnv *env, jobject thiz, jint type,
                                                      jbyteArray data, jint len, jlong tms) {
    jbyte *buf = env->GetByteArrayElements(data, 0);
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
        default:

            break;
    }
    env->ReleaseByteArrayElements(data, buf, 0);
}

JNIEXPORT jint JNICALL
Java_com_live_mooselive_av_screen_ScreenLive_connectRTMP(JNIEnv *env, jobject thiz, jstring url) {
    const char *rtmpUrl = env->GetStringUTFChars(url, NULL);

    live = static_cast<Live *>(malloc(sizeof(Live)));
    live->rtmp = RTMP_Alloc();
    RTMP_Init(live->rtmp);
    live->rtmp->Link.timeout = 10;
    if (!RTMP_SetupURL(live->rtmp, (char *) rtmpUrl)) {
        LOGE("RTMP_设置连接失败");
        return -1;
    }

    RTMP_EnableWrite(live->rtmp); // 推送

    if (!RTMP_Connect(live->rtmp, NULL)) {
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
Java_com_live_mooselive_av_screen_ScreenLive_closeRTMP(JNIEnv *env, jobject thiz) {
    if (live) {
        if (live->rtmp) {
//            RTMP_Close(live->rtmp);
            RTMP_Free(live->rtmp);
        }
        live->rtmp = NULL;
        free(live);
    }
    return 1;
}

}