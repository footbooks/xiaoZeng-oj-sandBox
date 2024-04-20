package com.zengjing.xiaozengojsandbox.unsafe;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 用户运行其他程序（比如木马文件）
 */
public class RunFileError {
    public static void main(String[] args) throws IOException, InterruptedException {
        String userDir = System.getProperty("user.dir");
        String filePath = userDir + File.separator+"src/main/resources/木马程序.bat";
        String errorMessage = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorMessage));
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String message = null;
        while ((message=bufferedReader.readLine())!=null){
            System.out.println(message);
        }
        System.out.println("执行异常程序成功");
    }
}
