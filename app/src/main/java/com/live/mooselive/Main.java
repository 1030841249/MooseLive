package com.live.mooselive;

public class Main {
    static Object mLcok = new Object();
    static long startTime = 0;
    public static void main(String[] args) {
        startTime = System.currentTimeMillis();
        new Thread(()->{
            synchronized (mLcok) {
                while(true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("通知锁     " + (System.currentTimeMillis() - startTime));
                    mLcok.notify();
                    break;
                }
            }
        }).start();
        synchronized (mLcok) {
            try {
                System.out.println("释放锁     " + (System.currentTimeMillis() - startTime));
                mLcok.wait();
                System.out.println("结束     " + (System.currentTimeMillis() - startTime));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
