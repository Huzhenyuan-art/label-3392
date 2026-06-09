package com.example.lab3392.dto;

import lombok.Data;
import java.util.List;

@Data
public class DashboardStats {

    private long totalCount;
    private long activeCount;
    private long inactiveCount;
    private double activePercent;
    private double inactivePercent;
    private List<DailyCount> trend;
    private List<LowStockProduct> lowStockProducts;

    @Data
    public static class DailyCount {
        private String date;
        private long count;
    }

    @Data
    public static class LowStockProduct {
        private Long id;
        private String name;
        private Integer stock;
        private String categoryName;
    }
}
