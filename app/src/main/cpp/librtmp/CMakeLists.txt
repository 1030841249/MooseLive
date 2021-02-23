cmake_minimum_required(VERSION 3.4.1)
# 不加密
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
file(GLOB rtmp_source *.c)
add_library(rtmp STATIC ${rtmp_source})