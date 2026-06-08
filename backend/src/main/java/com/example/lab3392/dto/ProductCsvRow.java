package com.example.lab3392.dto;

import java.math.BigDecimal;

public record ProductCsvRow(
        String name,
        String categoryName,
        String description,
        BigDecimal price,
        Integer stock,
        String status
) {
}
