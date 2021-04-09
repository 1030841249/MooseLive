package com.live.mooselive.av.bean;

public class RTMPPacket {
    public int type;
    public byte[] data;
    public int len;
    public long tms;

    public RTMPPacket(byte[] buffer, int len, long tms) {
        this.data = buffer;
        this.len = len;
        this.tms = tms;
    }

    public RTMPPacket(int type, byte[] data, int len, long tms) {
        this.type = type;
        this.data = data;
        this.len = len;
        this.tms = tms;
    }
}
