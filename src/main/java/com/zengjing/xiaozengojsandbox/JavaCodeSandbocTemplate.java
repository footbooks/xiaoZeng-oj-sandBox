package com.zengjing.xiaozengojsandbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeRequest;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeResponse;
import com.zengjing.xiaozengojsandbox.model.ExecuteMessage;
import com.zengjing.xiaozengojsandbox.model.JudgeInfo;
import com.zengjing.xiaozengojsandbox.security.MySecurityManager;
import com.zengjing.xiaozengojsandbox.utils.ProcessUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
@Slf4j
public abstract class JavaCodeSandbocTemplate implements CodeSandbox{
    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";
    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";
    //程序超时时间
    private static final long TIME_OUT = 5000l;

    /**
     * 将代码报存为文件
     * @param code 用户提交代码
     * @return File
     */
    public File saveCodeToFile(String code){
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
        return userCodeFile;
    }

    /**
     * 编译文件
     * @param userCodeFile 保存的代码文件
     * @return 编译信息
     */
    public ExecuteMessage compileFile(File userCodeFile){
        //3.编译代码，得到class文件
        String compileCmd = String.format("javac -encoding utf-8 %s",userCodeFile.getAbsolutePath());
        try {
            Process compileProcess = Runtime.getRuntime().exec(compileCmd);
            //编译代码
            ExecuteMessage executeMessage = ProcessUtils.runProcessAndGetMessage(compileProcess, "编译");
            if(executeMessage.getExitVaile()!=0){
                throw new RuntimeException("编译异常");
            }
            return executeMessage;
        } catch (Exception e) {
//            return getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行文件
     * @param inputList 输入用例
     * @return 执行信息
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList){
        //4.执行代码
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
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
                throw new RuntimeException("执行异常",e);
            }
        }
        return executeMessageList;
    }

    /**
     * 收集整理结果
     * @param executeMessageList 执行信息集合
     * @return 结果
     */
    public ExecuteCodeResponse collectResult(List<ExecuteMessage> executeMessageList){
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
        return executeCodeResponse;
    }

    /**
     * 清理文件 防止浪费空间资源
     * @param userCodeFile 用户保持代码文件
     */
    public boolean deleteFile(File userCodeFile){
        //6.清理文件,防止服务器空间不足
        if(userCodeFile.getParentFile() != null){
            String userCodeParentPathEnd = userCodeFile.getParentFile().getAbsolutePath();
            boolean result = FileUtil.del(userCodeParentPathEnd);
            return result;
        }
        return false;
    }
    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest){
        System.setSecurityManager(new MySecurityManager());
        String code = excuteCodeRequest.getCode();
        String language = excuteCodeRequest.getLanguage();
        List<String> inputList = excuteCodeRequest.getInputList();
        //1.将用户代码保存至文件
        File userCodeFile = saveCodeToFile(code);
        //2.编译代码，得到class文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        //3.执行代码
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile,inputList);
        //4.收集整理输出结果
        ExecuteCodeResponse executeCodeResponse = collectResult(executeMessageList);
        //5.清理文件,防止服务器空间不足
        boolean b = deleteFile(userCodeFile);
        if (!b){
            log.error("删除文件失败");
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
