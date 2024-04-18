package com.zengjing.xiaozengojsandbox.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/")
public class MainController {
    @RequestMapping("/health")
    public String healthCheck(){
        return "ok";
    }
}
