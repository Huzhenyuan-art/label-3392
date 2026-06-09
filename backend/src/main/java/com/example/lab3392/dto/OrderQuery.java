package com.example.lab3392.dto;

public record OrderQuery(
        String status,
        String orderNo
) {
    public String normalizedOrderNo() {
        if (orderNo == null || orderNo.isBlank()) return null;
        return orderNo.trim();
    }
}
