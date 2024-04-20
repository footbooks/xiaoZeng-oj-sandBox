package com.zengjing.xiaozengojsandbox.unsafe;

/**
 * 用户恶意占用时间
 */
public class SleepError {
    public static void main(String[] args) throws InterruptedException {
        Thread.sleep(60*60*1000l);
        System.out.println("睡完了");
    }
}
