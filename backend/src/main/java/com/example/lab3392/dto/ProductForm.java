package com.example.lab3392.dto;

import com.example.lab3392.entity.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductForm(
        Long id,
        @NotBlank(message = "名称不能为空")
        @Size(min = 1, max = 50, message = "名称长度需为 1-50")
        String name,
        @Size(max = 255, message = "描述最长 255")
        String description,
        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.00", message = "价格不能小于 0")
        BigDecimal price,
        @NotNull(message = "库存不能为空")
        @Min(value = 0, message = "库存不能小于 0")
        Integer stock,
        @NotBlank(message = "状态不能为空")
        String status
) {
    public static ProductForm empty() {
        return new ProductForm(null, "", "", BigDecimal.ZERO, 0, "ACTIVE");
    }

    public static ProductForm fromEntity(Product p) {
        return new ProductForm(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.getStatus());
    }
}

