package com.abreaking.knativedemo.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * hello world for knative-serving
 * @author liwei
 * @date 2021/11/9
 */
@RestController
public class KnativeServingController {

    @Value("${TARGET:World}")
    String target;

    @RequestMapping("/")
    String hello() {
        return "Hello " + target + "!";
    }

}
