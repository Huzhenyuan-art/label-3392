package com.example.lab3392.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ErrorPageController {
    @GetMapping("/error/403")
    public String denied() {
        return "error/403";
    }
}

