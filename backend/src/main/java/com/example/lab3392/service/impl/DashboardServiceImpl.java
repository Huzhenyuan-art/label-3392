package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.dto.DashboardStats;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.ProductCategory;
import com.example.lab3392.mapper.ProductCategoryMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.DashboardService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DashboardServiceImpl(ProductMapper productMapper, ProductCategoryMapper categoryMapper) {
        this.productMapper = productMapper;
        this.categoryMapper = categoryMapper;
    }

    @Override
    @Cacheable(cacheNames = "dashboardStatsV1")
    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        long total = productMapper.selectCount(null);
        long active = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getStatus, "ACTIVE"));
        long inactive = total - active;

        stats.setTotalCount(total);
        stats.setActiveCount(active);
        stats.setInactiveCount(inactive);

        if (total > 0) {
            stats.setActivePercent(BigDecimal.valueOf(active)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue());
            stats.setInactivePercent(BigDecimal.valueOf(inactive)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue());
        } else {
            stats.setActivePercent(0.0);
            stats.setInactivePercent(0.0);
        }

        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(6);
        LocalDateTime startDateTime = startDay.atStartOfDay();

        List<Product> recentProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .ge(Product::getCreatedAt, startDateTime)
                        .select(Product::getId, Product::getCreatedAt));

        Map<LocalDate, Long> countByDate = recentProducts.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getCreatedAt().toLocalDate(),
                        Collectors.counting()));

        List<DashboardStats.DailyCount> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            DashboardStats.DailyCount dc = new DashboardStats.DailyCount();
            dc.setDate(d.format(DATE_FMT));
            dc.setCount(countByDate.getOrDefault(d, 0L));
            trend.add(dc);
        }
        stats.setTrend(trend);

        List<Product> lowStockList = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                        .lt(Product::getStock, 10)
                        .orderByAsc(Product::getStock)
                        .select(Product::getId, Product::getName, Product::getStock, Product::getCategoryId));

        List<Long> catIds = lowStockList.stream()
                .map(Product::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> catNameMap = new HashMap<>();
        if (!catIds.isEmpty()) {
            categoryMapper.selectBatchIds(catIds)
                    .forEach(c -> catNameMap.put(c.getId(), c.getName()));
        }

        List<DashboardStats.LowStockProduct> lowStockProducts = lowStockList.stream().map(p -> {
            DashboardStats.LowStockProduct lsp = new DashboardStats.LowStockProduct();
            lsp.setId(p.getId());
            lsp.setName(p.getName());
            lsp.setStock(p.getStock());
            lsp.setCategoryName(catNameMap.get(p.getCategoryId()));
            return lsp;
        }).collect(Collectors.toList());
        stats.setLowStockProducts(lowStockProducts);

        return stats;
    }
}
