package com.zengjing.xiaozengojsandbox.security;

import java.security.Permission;

public class DefultSecurityManager extends SecurityManager{
    //检测所有权限

    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何权限控制");
        super.checkPermission(perm);
    }
}
