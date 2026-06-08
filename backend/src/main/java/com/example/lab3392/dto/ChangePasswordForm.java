package com.example.lab3392.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordForm(
        @NotBlank(message = "旧密码不能为空")
        String oldPassword,
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 50, message = "新密码长度至少 6 位")
        String newPassword,
        @NotBlank(message = "确认密码不能为空")
        String confirmPassword
) {
    public static ChangePasswordForm empty() {
        return new ChangePasswordForm("", "", "");
    }
}
