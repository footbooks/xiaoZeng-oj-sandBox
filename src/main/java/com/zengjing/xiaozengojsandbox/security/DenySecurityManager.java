package com.zengjing.xiaozengojsandbox.security;

import java.security.Permission;

/**
 * 带所有权限安全管理器
 */
public class DenySecurityManager extends SecurityManager{
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限异常"+perm.toString());
    }
}
