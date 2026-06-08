package com.example.lab3392.dto;

import java.time.LocalDate;

public record OperationLogQuery(
        String operationType,
        LocalDate startDate,
        LocalDate endDate
) {
    public String normalizedOperationType() {
        if (operationType == null) return null;
        String t = operationType.trim();
        return t.isEmpty() ? null : t;
    }

    public LocalDate normalizedStartDate() {
        return startDate;
    }

    public LocalDate normalizedEndDate() {
        return endDate;
    }
}
