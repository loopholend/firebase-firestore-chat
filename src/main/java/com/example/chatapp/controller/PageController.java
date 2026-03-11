package com.example.chatapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login.html")
    public String loginHtmlRedirect() {
        return "redirect:/login";
    }

    @GetMapping("/register.html")
    public String registerHtmlRedirect() {
        return "redirect:/register";
    }

    @GetMapping("/home.html")
    public String homeHtmlRedirect() {
        return "redirect:/home";
    }
}
