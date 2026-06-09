package com.example.lab3392.service.impl;

import com.example.lab3392.entity.Order;
import com.example.lab3392.entity.OrderItem;
import com.example.lab3392.mapper.OrderItemMapper;
import com.example.lab3392.mapper.OrderMapper;
import com.example.lab3392.service.CacheService;
import com.example.lab3392.service.OrderService;
import com.example.lab3392.service.StockService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutScheduler.class);

    private final OrderService orderService;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final StockService stockService;
    private final CacheService cacheService;

    public OrderTimeoutScheduler(OrderService orderService, OrderMapper orderMapper,
                                 OrderItemMapper orderItemMapper, StockService stockService,
                                 CacheService cacheService) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.stockService = stockService;
        this.cacheService = cacheService;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredOrders() {
        List<Order> expiredOrders = orderService.getExpiredPendingOrders();
        if (expiredOrders.isEmpty()) return;

        log.info("发现 {} 个超时未支付订单，开始自动取消", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
                for (OrderItem item : items) {
                    stockService.releaseStock(item.getProductId(), item.getQuantity(), order.getId());
                }
                order.setStatus(Order.STATUS_CANCELLED);
                orderMapper.updateById(order);
                log.info("订单 {} 已超时自动取消", order.getOrderNo());
            } catch (Exception e) {
                log.error("取消超时订单 {} 失败: {}", order.getOrderNo(), e.getMessage());
            }
        }

        cacheService.evictAllProductCaches();
        cacheService.evictAllDashboardCaches();
    }
}
