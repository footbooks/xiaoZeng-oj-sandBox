package com.zengjing.xiaozengojsandbox;


import com.zengjing.xiaozengojsandbox.model.ExecuteCodeRequest;
import com.zengjing.xiaozengojsandbox.model.ExecuteCodeResponse;

import java.io.IOException;

public interface CodeSandbox {
    ExecuteCodeResponse executeCode(ExecuteCodeRequest excuteCodeRequest) throws IOException;
}
