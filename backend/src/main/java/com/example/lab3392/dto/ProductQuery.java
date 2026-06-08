package com.example.lab3392.dto;

import java.math.BigDecimal;

public record ProductQuery(String name, BigDecimal minPrice, BigDecimal maxPrice) {
    public String normalizedName() {
        if (name == null) return null;
        String n = name.trim();
        return n.isEmpty() ? null : n;
    }
}

