package com.zengjing.xiaozengojsandbox.utils;

import cn.hutool.core.util.StrUtil;
import com.zengjing.xiaozengojsandbox.model.ExecuteMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;

public class ProcessUtils {
    /**
     * 执行正常进程
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runProcessAndGetMessage(Process runProcess,String opName){
        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            int exitVaile = runProcess.waitFor();
            executeMessage.setExitVaile(exitVaile);
            if(exitVaile==0){
                System.out.println(opName+"成功");
                //分批获取程序的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compileOutputLine = null;
                ArrayList<String> outputList = new ArrayList<>();
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
                //逐行读取程序输出
                while((compileOutputLine=bufferedReader.readLine())!=null){
                    outputList.add(compileOutputLine);
//                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));
            }else{
                System.out.println(opName+"失败，错误码："+exitVaile);
                //分批获取程序的正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                String compileOutputLine = null;
                ArrayList<String> outputList = new ArrayList<>();
//                StringBuilder compileOutputStringBuilder = new StringBuilder();
                //逐行读取程序输出
                while((compileOutputLine=bufferedReader.readLine())!=null){
                    outputList.add(compileOutputLine);
//                    compileOutputStringBuilder.append(compileOutputLine).append("\n");
                }
                executeMessage.setMessage(StringUtils.join(outputList,"\n"));
                //分批获取程序的错误输出
                BufferedReader errorBufferedReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                String errorCompileOutputLine = null;
                ArrayList<String> errorOutputList = new ArrayList<>();
//                StringBuilder errorCompileOutputStringBuilder = new StringBuilder();
                //逐行读取程序错误输出
                while((errorCompileOutputLine=errorBufferedReader.readLine())!=null){
                    errorOutputList.add(errorCompileOutputLine);
//                    errorCompileOutputStringBuilder.append(errorCompileOutputLine).append("\n");
                }
                executeMessage.setErrorMessage(StringUtils.join(errorOutputList,"\n"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;
    }

    /**
     * 执行交互式进程
     * @param runProcess
     * @param opName
     * @return
     */
    public static ExecuteMessage runInteractAndGetMessage(Process runProcess,String opName,String args){
        ExecuteMessage executeMessage = new ExecuteMessage();
        try{
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            //向控制台1输入数据
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            outputStreamWriter.write(join);
            //执行输入
            outputStreamWriter.flush();
            //分批获取程序的正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String compileOutputLine = null;
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            //逐行读取程序输出
            while((compileOutputLine=bufferedReader.readLine())!=null){
                compileOutputStringBuilder.append(compileOutputLine).append("\n");
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());
            //记得资源的释放，否则会卡死
            outputStreamWriter.close();
            inputStream.close();
            outputStream.close();
            runProcess.destroy();
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        }catch (Exception e){
            e.printStackTrace();
        }
        return executeMessage;
    }
}
