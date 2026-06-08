package com.example.lab3392.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterForm(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 20, message = "用户名长度需为 3-20")
        String username,
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email,
        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 50, message = "密码长度至少 6 位")
        String password,
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
    public static RegisterForm empty() {
        return new RegisterForm("", "", "", "");
    }
}
