package com.zengjing.xiaozengojsandbox.model;

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {
    /**
     * 提交信息
     */
    private String message;
    /**
     * 内存限制（MB）
     */
    private Long memory;
    /**
     * 时间信息（MB）
     */
    private Long time;
}
