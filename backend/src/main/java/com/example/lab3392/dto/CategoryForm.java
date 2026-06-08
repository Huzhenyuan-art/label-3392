package com.example.lab3392.dto;

import com.example.lab3392.entity.ProductCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryForm(
        Long id,
        @NotBlank(message = "名称不能为空")
        @Size(min = 1, max = 50, message = "名称长度需为 1-50")
        String name,
        @NotBlank(message = "编码不能为空")
        @Size(min = 1, max = 50, message = "编码长度需为 1-50")
        String code,
        @Size(max = 255, message = "描述最长 255")
        String description,
        @NotNull(message = "排序不能为空")
        @Min(value = 0, message = "排序不能小于 0")
        Integer sortOrder,
        @NotBlank(message = "状态不能为空")
        String status
) {
    public static CategoryForm empty() {
        return new CategoryForm(null, "", "", "", 0, "ACTIVE");
    }

    public static CategoryForm fromEntity(ProductCategory c) {
        return new CategoryForm(c.getId(), c.getName(), c.getCode(), c.getDescription(), c.getSortOrder(), c.getStatus());
    }
}
