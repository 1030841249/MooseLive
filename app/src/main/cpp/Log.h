//
// Created by Higo on 2021/3/8.
//

#include <android/log.h>
#ifndef MOOSELIVE_LOG_H
#define MOOSELIVE_LOG_H
#define LOG_TAG "LOG_NATIVE"
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#endif //MOOSELIVE_LOG_H
