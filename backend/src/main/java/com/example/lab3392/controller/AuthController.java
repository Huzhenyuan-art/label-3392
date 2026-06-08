package com.example.lab3392.controller;

import com.example.lab3392.dto.RegisterForm;
import com.example.lab3392.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", RegisterForm.empty());
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("error", binding.getAllErrors().isEmpty() ? "表单校验失败" : binding.getAllErrors().get(0).getDefaultMessage());
            return "auth/register";
        }
        if (!form.password().equals(form.confirmPassword())) {
            model.addAttribute("error", "两次密码输入不一致");
            return "auth/register";
        }

        try {
            userService.register(form.username().trim(), form.email().trim(), form.password());
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}
