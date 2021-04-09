package com.live.mooselive.av.bean;

import android.os.Handler;
import android.os.HandlerThread;

import java.util.Deque;
import java.util.LinkedList;

public abstract class BaseLive implements Runnable {

    private enum STATE{
        RUNNING,
        STOP
    }

    private STATE mCurState = STATE.RUNNING;
    private Deque<RTMPPacket> mPacketList = new LinkedList();

    @Override
    public void run() {
        while (mCurState == STATE.RUNNING) {
            running();
        }
        onStop();
    }

    public RTMPPacket getFristFrame(){
        return mPacketList.getFirst();
    }

    public RTMPPacket removeFirst(){
        return mPacketList.removeFirst();
    }

    public void addFrame(RTMPPacket rtmpPacket) {
        if (rtmpPacket != null) {
            mPacketList.addLast(rtmpPacket);
        }
    }

    public boolean isNotEmpty() {
        return !mPacketList.isEmpty();
    }

    public abstract void running();

    public void start() {
        mCurState = STATE.RUNNING;
        onStart();
    }

    public void stop() {
        mCurState = STATE.STOP;
    }

    public abstract void onStart();

    public abstract void onStop();
}
