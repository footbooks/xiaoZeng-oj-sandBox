package com.zengjing.xiaozengojsandbox.unsafe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 用户植入木马
 */
public class WriteFileError {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir+ File.separator+"src/main/resources/木马程序.bat";
        String errorMessage = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorMessage));
        System.out.println("木马植入成功，你完了哈哈哈");
    }
}
