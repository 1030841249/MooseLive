#include <jni.h>
#include <string>
#include <rtmp.h>
#include <log.h>
#include <pthread.h>
#include "android/log.h"

RTMP *rtmp = NULL;

JavaVM *g_jvm;
jobject g_obj;

char* oneTag;


void run() {


//    while(true) {
//
//        __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "线程运行中");
//    }
}


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
    while(nRead = RTMP_Read(rtmp,buf,bufSize)) {
        env->SetByteArrayRegion(byteArray, 0, nRead, (jbyte *)buf);
        env->CallVoidMethod(g_obj, receiveRtmpData,byteArray);
        fwrite(buf, 1, nRead, fp);
        countReadSize += nRead;



        __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "本次读取数：%d  总读取数量：%d", nRead,
                            countReadSize);
    }

    free(buf);
}

void judgeOneTag(char* tmpData) {

}

void releaseRTMP() {
    RTMP_Close(rtmp);
    RTMP_Free(rtmp);
    rtmp = NULL;
}

int connectRTMP(JNIEnv *env,char* url) {

    if(rtmp != NULL) {
        releaseRTMP();
    }

    bool isLiveStream = false;

    rtmp =RTMP_Alloc();
    RTMP_Init(rtmp);
    rtmp->Link.timeout = 10;
    if (!RTMP_SetupURL(rtmp, url)) {
        RTMP_Free(rtmp);
        return -1;
    }
    if(isLiveStream) {
        rtmp->Link.lFlags |= RTMP_LF_LIVE;
    }

    RTMP_SetBufferMS(rtmp, 3600 * 1000);

    if (!RTMP_Connect(rtmp, NULL)) {
        RTMP_Log(RTMP_LOGERROR, "NetConnect is Error");
        RTMP_Free(rtmp);
        return -1;
    }
    if(!RTMP_ConnectStream(rtmp,0)) {
        RTMP_Log(RTMP_LOGERROR, "NetStream is Error");
        RTMP_Free(rtmp);
        RTMP_Close(rtmp);
        return -1;
    }


    __android_log_print(ANDROID_LOG_ERROR, "NATIVE", "RTMP 连接成功");
    receiveRTMPData(env);
    // 连接操作完成，开启线程接收数据
//    pthread_create(&thread, nullptr, reinterpret_cast<void *(*)(void *)>(receiveRTMPData), nullptr);
    return 0;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_live_mooselive_activity_MainActivity_stringFromJNI(JNIEnv *env, jobject thiz) {
    std::string hello = "Hello from C++";

    env->GetJavaVM(&g_jvm);
    g_obj = env->NewGlobalRef(thiz);

    __android_log_print(ANDROID_LOG_ERROR,"NATIVE","开启线程 ");
    pthread_t p[3];
    pthread_create(&p[0], NULL, reinterpret_cast<void *(*)(void *)>(run), NULL);
    return env->NewStringUTF(hello.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_live_mooselive_activity_RTMPActivity_connectRTMP(JNIEnv *env, jobject thiz, jstring url) {
    const char* rtmpUrl = env->GetStringUTFChars(url, NULL);
    g_obj = env->NewGlobalRef(thiz);
    char *tmp = const_cast<char *>(rtmpUrl);
    connectRTMP(env,tmp);
}extern "C"
JNIEXPORT void JNICALL
Java_com_live_mooselive_activity_RTMPActivity_closeRTMP(JNIEnv *env, jobject thiz) {
    if(rtmp) {
        RTMP_Free(rtmp);
        RTMP_Close(rtmp);
        rtmp = nullptr;
        __android_log_print(ANDROID_LOG_ERROR,"NATIVE","关闭RTMP成功 ");
    } else {

    }
}

int sendAudioData(int8_t *data,jint len,long tms) {

}

int sendVideoData(){

}


extern "C"
JNIEXPORT void JNICALL
Java_com_live_mooselive_av_ScreenLive_sendData(JNIEnv *env, jobject thiz, jint type,
                                               jbyteArray data, jint len, jlong tms) {
    switch(type) {
        case 0: // audio

            break;
        case 1: // video

            break;
    }
}