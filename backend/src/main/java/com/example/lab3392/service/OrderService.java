package com.example.lab3392.service;

import com.example.lab3392.dto.OrderQuery;
import com.example.lab3392.entity.Order;
import java.util.List;

public interface OrderService {
    Order createOrderFromCart(Long userId);
    Order payOrder(Long orderId, Long userId);
    Order cancelOrder(Long orderId, Long userId);
    Order getOrderById(Long orderId, Long userId);
    Order getOrderByIdForAdmin(Long orderId);
    List<Order> getUserOrders(Long userId, String status);
    List<Order> getAllOrders(OrderQuery query);
    List<Order> getExpiredPendingOrders();
}
