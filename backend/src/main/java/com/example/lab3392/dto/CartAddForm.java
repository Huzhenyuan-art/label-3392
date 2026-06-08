package com.example.lab3392.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddForm(
        @NotNull(message = "请选择产品")
        Long productId,

        @NotNull(message = "请输入数量")
        @Min(value = 1, message = "数量至少为 1")
        Integer quantity
) {
    public static CartAddForm empty() {
        return new CartAddForm(null, 1);
    }
}
