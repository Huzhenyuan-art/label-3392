package com.example.lab3392.controlleradvice;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request, Model model) {
        model.addAttribute("message", ex.getMessage() == null ? "请求不合法" : ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/simple";
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleSecurityException(SecurityException ex, HttpServletRequest request, Model model) {
        model.addAttribute("message", ex.getMessage() == null ? "无权执行此操作" : ex.getMessage());
        model.addAttribute("path", request.getRequestURI());
        return "error/simple";
    }
}
