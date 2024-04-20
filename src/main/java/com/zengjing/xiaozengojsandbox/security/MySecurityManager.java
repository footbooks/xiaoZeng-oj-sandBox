package com.zengjing.xiaozengojsandbox.security;

import java.security.Permission;

public class MySecurityManager extends SecurityManager{
    //检测所有权限

    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何权限控制");
        super.checkPermission(perm);
    }

    /**
     * 限制读文件
     */
    @Override
    public void checkRead(String file) {
        throw new SecurityException("读权限异常"+file);
    }

    /**
     * 限制写文件
     * @param file   the system-dependent file name.
     */
    @Override
    public void checkWrite(String file) {
        throw new SecurityException("写权限异常"+file);
    }
    /**
     * 限制删除文件
     */
    @Override
    public void checkDelete(String file) {
        throw new SecurityException("删除权限异常"+file);
    }
}
