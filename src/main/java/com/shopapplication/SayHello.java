package com.shopapplication;

import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SayHello {
    @GetMapping("/")
    public String sayHello(){
        return "hello guys in the home page ";
    }
    
}
