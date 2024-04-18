package com.zengjing.xiaozengojsandbox.model;

import lombok.Data;

@Data
public class ExecuteMessage {
    private Integer exitVaile;
    private String message;
    private String errorMessage;
    private Long time;
}
