package com.github.choseongah.ssh.shell.basic;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BasicController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
