package com.example.lab3392.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileForm(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        String email
) {
    public static ProfileForm empty() {
        return new ProfileForm("");
    }
}
