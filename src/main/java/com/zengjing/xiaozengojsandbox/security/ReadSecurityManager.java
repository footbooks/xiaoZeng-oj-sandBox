package com.zengjing.xiaozengojsandbox.security;

/**
 * 限制读权限
 */
public class ReadSecurityManager extends SecurityManager{
    @Override
    public void checkRead(String file) {
        throw new SecurityException("读权限异常"+file);
    }
}