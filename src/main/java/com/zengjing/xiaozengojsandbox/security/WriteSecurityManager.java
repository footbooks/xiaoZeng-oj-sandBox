package com.zengjing.xiaozengojsandbox.security;

/**
 * 限制写权限
 */
public class WriteSecurityManager extends SecurityManager{
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("写权限异常"+file);
    }
}