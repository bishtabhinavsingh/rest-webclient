package com.beasttech.restfulapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class Controller {

    @Autowired
    RestServiceCaller restServiceCaller;

    @GetMapping("/apiget")
    public String getEndPoint(@RequestParam("params") String params){
        Map<String, String> methodsAndURLs = new HashMap<>();
        methodsAndURLs.put("GET", "http://localhost:8090/async/gettest");
        methodsAndURLs.put("PUT", "http://localhost:8090/async/puttest");
        methodsAndURLs.put("POST", "http://localhost:8090/async/posttest");
        methodsAndURLs.put("DELETE", "http://localhost:8090/async/deletetest");

        try{
            for (Map.Entry<String, String> entry : methodsAndURLs.entrySet()) {
                ResponseEntity<String> response = restServiceCaller.requestAsync(params,HttpMethod.resolve(entry.getKey()),entry.getValue());
                methodsAndURLs.put(entry.getKey(),response.getBody());
                System.out.println(entry.getKey() + "  :  " +response.getBody());
            }
            return methodsAndURLs.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
