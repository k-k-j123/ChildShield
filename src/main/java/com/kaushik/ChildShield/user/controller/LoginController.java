package com.kaushik.ChildShield.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping({"/login", "/", "/index", "/index.html"})
    public String login() {
        return "index";
    }

    @GetMapping({"/register", "/register.html"})
    public String register() {
        return "register";
    }
}
