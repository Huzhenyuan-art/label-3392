package com.example.lab3392.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CsvImportResult {
    private int totalRows;
    private int successCount;
    private int failureCount;
    private final List<FailureDetail> failures = new ArrayList<>();

    @Data
    public static class FailureDetail {
        private final int rowNumber;
        private final String productName;
        private final String reason;

        public FailureDetail(int rowNumber, String productName, String reason) {
            this.rowNumber = rowNumber;
            this.productName = productName;
            this.reason = reason;
        }
    }

    public void addSuccess() {
        successCount++;
    }

    public void addFailure(int rowNumber, String productName, String reason) {
        failureCount++;
        failures.add(new FailureDetail(rowNumber, productName, reason));
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }
}
