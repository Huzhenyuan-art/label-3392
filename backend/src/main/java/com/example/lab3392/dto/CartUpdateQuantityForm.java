package com.example.lab3392.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateQuantityForm(
        @NotNull(message = "请输入数量")
        @Min(value = 1, message = "数量至少为 1")
        Integer quantity
) {}
