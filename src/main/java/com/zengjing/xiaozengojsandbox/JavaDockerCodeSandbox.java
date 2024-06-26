package com.zengjing.xiaozengojsandbox;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ArrayUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeRequest;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeResponse;
import com.zengjing.xiaozengojsandbox.model.ExecuteMessage;
import org.springframework.stereotype.Component;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class JavaDockerCodeSandbox extends JavaCodeSandbocTemplate{
    //程序超时时间
    private static final long TIME_OUT = 5000l;
    private static Boolean FIRST_INIT = true;

    @Override
    public List<ExecuteMessage> runFile(File userCodeFile, List<String> inputList) {
        //3.创建容器，上传编译文件
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        DockerClient dockerClient = DockerClientBuilder.getInstance().build();
        //拉取镜像
        String dockerName = "openjdk:8-alpine";
        if (FIRST_INIT){
            PullImageResultCallback pullImageResultCallback = new PullImageResultCallback(){
                @Override
                public void onNext(PullResponseItem item) {
                    System.out.println("拉取镜像状态："+item.getStatus());
                    super.onNext(item);
                }
            };
            PullImageCmd pullImageCmd = dockerClient.pullImageCmd(dockerName);
            try {
                pullImageCmd.exec(pullImageResultCallback)
                        .awaitCompletion();
            } catch (InterruptedException e) {
                System.out.println("拉取镜像异常");
                throw new RuntimeException(e);
            }
            System.out.println("拉取镜像成功");
        }
        //创建容器
        CreateContainerCmd containerCmd = dockerClient.createContainerCmd(dockerName);
        String profileConfig = ResourceUtil.readUtf8Str("profile.json");
        HostConfig hostConfig = new HostConfig();
        //限制内存为100
        hostConfig.withMemory(100*1000* 1000L);
        //限制使用cpu核数为1
        hostConfig.withCpuCount(1L);
        //设置内存交换值
        hostConfig.withMemorySwap(0L);
        //限制用户权限
        hostConfig.withSecurityOpts(Arrays.asList("seccomp="+profileConfig));
        hostConfig.setBinds(new Bind(userCodeParentPath,new Volume("/app")));
        CreateContainerResponse createContainerResponse = containerCmd
                .withHostConfig(hostConfig)
                //限制网络
                .withNetworkDisabled(true)
                //限制不能往根路径读写文件
                .withReadonlyRootfs(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withTty(true)
                .exec();
        System.out.println(createContainerResponse);
        String containerId = createContainerResponse.getId();
        //启动容器
        dockerClient.startContainerCmd(containerId).exec();
        //执行命令并获取结果
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
        ExecuteMessage executeMessage = new ExecuteMessage();
        final String[] message = {null};
        final String[] errorMessage = {null};
        String[] cmdArray = new String[]{"java","-cp","/cpp","Main"};
        for(String inputArg : inputList){
            StopWatch stopWatch = new StopWatch();
            String[] inputArgsList = inputArg.split(" ");
            String[] endCmdLists = ArrayUtil.append(cmdArray, inputArgsList);
            long time = 0l;
            ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(containerId)
                    .withCmd(endCmdLists)
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .exec();
            System.out.println("执行命令"+execCreateCmdResponse);
            String execId = execCreateCmdResponse.getId();
            //判断程序是否超时
            final boolean[] timeout = {true};
            ExecStartResultCallback execStartResultCallback = new ExecStartResultCallback(){
                @Override
                public void onNext(Frame frame) {
                    StreamType streamType = frame.getStreamType();
                    if(StreamType.STDERR.equals(streamType)){
                        errorMessage[0] =new String(frame.getPayload());
                        System.out.println("输出错误结果："+frame.getPayload());

                    }else{
                        message[0] =new String(frame.getPayload());
                        System.out.println("输出结果："+frame.getPayload());
                    }
                    super.onNext(frame);
                }
                @Override
                public void onComplete(){
                    //如果执行完成，则表示没超时
                    timeout[0] =false;
                    super.onComplete();
                }
            };
            //获取内存的占用
            final long[] maxMemory = {0l};
            StatsCmd statsCmd = dockerClient.statsCmd(containerId);
            ResultCallback<Statistics> resultCallback = new ResultCallback(){

                @Override
                public void onNext(Object obj) {
                    Statistics statistics = (Statistics) obj;
                    maxMemory[0] = Math.max(statistics.getMemoryStats().getUsage(), maxMemory[0]);
                    System.out.println("内存占用:"+statistics.getMemoryStats().getUsage());
                }

                @Override
                public void close() throws IOException {

                }

                @Override
                public void onStart(Closeable closeable) {

                }


                @Override
                public void onError(Throwable throwable) {

                }

                @Override
                public void onComplete() {

                }
            };
            statsCmd.exec(resultCallback);
            try {
                stopWatch.start();
                dockerClient.execStartCmd(execId)
                        .exec(execStartResultCallback)
                        //设置程序运行时间，单位毫秒
                        .awaitCompletion(TIME_OUT, TimeUnit.MICROSECONDS);
                stopWatch.stop();
                time = stopWatch.getLastTaskTimeMillis();
                statsCmd.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            executeMessage.setMessage(message[0]);
            executeMessage.setErrorMessage(errorMessage[0]);
            executeMessage.setTime(time);
            executeMessage.setMemory(maxMemory[0]);
            executeMessageList.add(executeMessage);
        }
        return executeMessageList;
    }

}
