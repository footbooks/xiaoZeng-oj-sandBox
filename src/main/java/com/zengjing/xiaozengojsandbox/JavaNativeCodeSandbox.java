package com.zengjing.xiaozengojsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeRequest;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeResponse;
import com.zengjing.xiaozengojsandbox.model.ExecuteMessage;
import com.zengjing.xiaozengojsandbox.model.JudgeInfo;
import com.zengjing.xiaozengojsandbox.utils.ProcessUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class JavaNativeCodeSandbox implements CodeSandbox{
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    //程序超时时间
    private static final long TIME_OUT = 5000l;
    //程序黑名单
    private static final List<String> blackList = Arrays.asList("Files","exec");
    //初始化
    private static WordTree wordTree;
    static {
        wordTree=new WordTree();
        wordTree.addWords(blackList);
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest){
        String code = excuteCodeRequest.getCode();
        String language = excuteCodeRequest.getLanguage();
        List<String> inputList = excuteCodeRequest.getInputList();
        //校验代码中是否包含黑名单
        FoundWord foundWord = wordTree.matchWord(code);
        if(foundWord!=null){
            System.out.println("包含禁止词"+foundWord.getFoundWord());
            ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
            executeCodeResponse.setStatus(3);
            executeCodeResponse.setMessage("包含非法词");
            return executeCodeResponse;
        }
        //1.将用户代码保存至文件夹
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir+ File.separator+ GLOBAL_CODE_DIR_NAME;
        //判断全局代码是否存在，没用则创建
        if(!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //2.把用户代码隔离存放
        String userCodeParentPath = globalCodePathName+File.separator+ UUID.randomUUID();
        String userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        //3.编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            //编译代码
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            System.out.println(executeMessage);
        } catch (Exception e) {
            return getErrorResponse(e);
        }
        //4.执行代码
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
        for(String test : inputList){
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=utf-8 -cp %s Main %s",userCodeParentPath,test);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //运行程序前开启一个新线程休眠，如果休眠时间超时，杀死正在运行的进程
                new Thread(()->{
                    try{
                        Thread.sleep(TIME_OUT);
                        System.out.println("超时了，中断");
                        if (runProcess.isAlive()){
                            runProcess.destroy();
                        }
                    }catch (Exception e) {
                        throw new RuntimeException();
                    }
                }).start();
                //运行代码
                ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(runProcess, "运行");
                //将输出信息存入集合中
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                return getErrorResponse(e);
            }
        }
        //5.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        //取用时最大值，判断程序有没有超时
        Long maxTime = null;
        for(ExecuteMessage executeMessage:executeMessageList){
            Long time = executeMessage.getTime();
            if(time>maxTime){
                maxTime=time;
            }
            String errorMessage = executeMessage.getErrorMessage();
            if(StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
        }
        executeCodeResponse.setOutputList(outputList);
        if (outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        //要调用第三方库，先不实现
//        judgeInfo.setMessage();
        executeCodeResponse.setJudgeInfo(judgeInfo);
        //6.清理文件,防止服务器空间不足
        if(userCodeFile.getParentFile() != null){
            String userCodeParentPathEnd = userCodeFile.getParentFile().getAbsolutePath();
            boolean result = FileUtil.del(userCodeParentPathEnd);
            System.out.println("删除"+(result?"成功":"失败"));
        }
        return executeCodeResponse;
    }
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
