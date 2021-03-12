package com.live.mooselive.bean;

public class RTMPPacket {
    public byte[] buffer;
    public int len;
    public long tms;

    public RTMPPacket(byte[] buffer, int len, long tms) {
        this.buffer = buffer;
        this.len = len;
        this.tms = tms;
    }
}
