package com.zengjing.xiaozengojsandbox.unsafe;

import java.util.ArrayList;

/**
 * 用户恶意占用空间
 */
public class MemoryError {
    public static void main(String[] args) {
        ArrayList<Byte[]> bytes = new ArrayList<>();
        while (true){
            bytes.add(new Byte[10000]);
        }
    }
}
