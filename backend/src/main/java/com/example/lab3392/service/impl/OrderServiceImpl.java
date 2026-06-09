package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.dto.OrderQuery;
import com.example.lab3392.entity.CartItem;
import com.example.lab3392.entity.Order;
import com.example.lab3392.entity.OrderItem;
import com.example.lab3392.entity.Product;
import com.example.lab3392.mapper.CartItemMapper;
import com.example.lab3392.mapper.OrderItemMapper;
import com.example.lab3392.mapper.OrderMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.service.CacheService;
import com.example.lab3392.service.OrderService;
import com.example.lab3392.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private static final int ORDER_TIMEOUT_MINUTES = 30;
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final CartItemMapper cartItemMapper;
    private final StockService stockService;
    private final CacheService cacheService;

    public OrderServiceImpl(OrderMapper orderMapper, OrderItemMapper orderItemMapper,
                            ProductMapper productMapper, CartItemMapper cartItemMapper,
                            StockService stockService, CacheService cacheService) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.cartItemMapper = cartItemMapper;
        this.stockService = stockService;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional
    public Order createOrderFromCart(Long userId) {
        List<CartItem> cartItems = cartItemMapper.selectByUserIdWithProduct(userId);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new IllegalArgumentException("购物车为空，无法提交订单");
        }

        for (CartItem item : cartItems) {
            if (item.getProduct() == null) {
                Product p = productMapper.selectById(item.getProductId());
                item.setProduct(p);
            }
        }

        List<String> errors = validateCartItems(cartItems);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus(Order.STATUS_PENDING_PAYMENT);
        order.setExpireAt(LocalDateTime.now().plusMinutes(ORDER_TIMEOUT_MINUTES));
        order.setTotalAmount(BigDecimal.ZERO);
        orderMapper.insert(order);

        Map<Long, Integer> productQuantityMap = new HashMap<>();
        Map<Long, Integer> productStockMap = new HashMap<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            if (product == null) continue;

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setProductPrice(product.getPrice());
            item.setQuantity(cartItem.getQuantity());
            item.setSubtotal(subtotal);
            orderItemMapper.insert(item);
            orderItems.add(item);

            productQuantityMap.put(product.getId(), cartItem.getQuantity());
            productStockMap.put(product.getId(), product.getStock());
        }

        try {
            stockService.batchHoldStock(productQuantityMap, productStockMap, order.getId());
        } catch (IllegalArgumentException e) {
            order.setStatus(Order.STATUS_CANCELLED);
            orderMapper.updateById(order);
            throw e;
        }

        order.setTotalAmount(totalAmount);
        order.setItems(orderItems);
        orderMapper.updateById(order);

        cacheService.evictAllProductCaches();
        cacheService.evictAllDashboardCaches();
        return order;
    }

    @Override
    @Transactional
    public Order payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("无权操作此订单");
        }
        if (!Order.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("订单状态不是待支付，无法支付");
        }
        if (order.getExpireAt() != null && LocalDateTime.now().isAfter(order.getExpireAt())) {
            throw new IllegalArgumentException("订单已超时，请重新下单");
        }

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        boolean allDeducted = true;
        for (OrderItem item : items) {
            int rows = orderMapper.deductStock(item.getProductId(), item.getQuantity());
            if (rows == 0) {
                allDeducted = false;
            }
        }

        if (!allDeducted) {
            throw new IllegalArgumentException("库存不足，支付失败。请稍后重试或取消订单");
        }

        for (OrderItem item : items) {
            stockService.clearHoldOnPayment(item.getProductId(), item.getQuantity(), orderId);
        }

        order.setStatus(Order.STATUS_PAID);
        order.setPaidAt(LocalDateTime.now());
        orderMapper.updateById(order);

        for (OrderItem item : items) {
            cartItemMapper.delete(
                    new LambdaQueryWrapper<CartItem>()
                            .eq(CartItem::getUserId, userId)
                            .eq(CartItem::getProductId, item.getProductId())
            );
        }

        cacheService.evictAllProductCaches();
        cacheService.evictAllDashboardCaches();
        return order;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("无权操作此订单");
        }
        if (!Order.STATUS_PENDING_PAYMENT.equals(order.getStatus())) {
            throw new IllegalArgumentException("只能取消待支付订单");
        }

        List<OrderItem> items = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : items) {
            stockService.releaseStock(item.getProductId(), item.getQuantity(), orderId);
        }

        order.setStatus(Order.STATUS_CANCELLED);
        orderMapper.updateById(order);

        cacheService.evictAllProductCaches();
        cacheService.evictAllDashboardCaches();
        return order;
    }

    @Override
    public Order getOrderById(Long orderId, Long userId) {
        Order order = orderMapper.selectByIdWithUsername(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("无权查看此订单");
        }
        order.setItems(orderItemMapper.selectByOrderId(orderId));
        return order;
    }

    @Override
    public Order getOrderByIdForAdmin(Long orderId) {
        Order order = orderMapper.selectByIdWithUsername(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        order.setItems(orderItemMapper.selectByOrderId(orderId));
        return order;
    }

    @Override
    public List<Order> getUserOrders(Long userId, String status) {
        List<Order> orders;
        if (status != null && !status.isBlank()) {
            orders = orderMapper.selectByUserIdAndStatus(userId, status);
        } else {
            orders = orderMapper.selectByUserIdWithUsername(userId);
        }
        for (Order order : orders) {
            order.setItems(orderItemMapper.selectByOrderId(order.getId()));
        }
        return orders;
    }

    @Override
    public List<Order> getAllOrders(OrderQuery query) {
        String status = query.status();
        String orderNo = query.normalizedOrderNo();
        List<Order> orders;

        if (orderNo != null && status != null && !status.isBlank()) {
            orders = orderMapper.searchByStatusAndOrderNo(status, orderNo);
        } else if (orderNo != null) {
            orders = orderMapper.searchByOrderNoWithUsername(orderNo);
        } else if (status != null && !status.isBlank()) {
            orders = orderMapper.selectByStatusWithUsername(status);
        } else {
            orders = orderMapper.selectAllWithUsername();
        }

        for (Order order : orders) {
            order.setItems(orderItemMapper.selectByOrderId(order.getId()));
        }
        return orders;
    }

    @Override
    public List<Order> getExpiredPendingOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, Order.STATUS_PENDING_PAYMENT)
               .lt(Order::getExpireAt, LocalDateTime.now());
        return orderMapper.selectList(wrapper);
    }

    private String generateOrderNo() {
        String timestamp = LocalDateTime.now().format(ORDER_NO_FMT);
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + timestamp + random;
    }

    private List<String> validateCartItems(List<CartItem> items) {
        List<String> errors = new ArrayList<>();
        for (CartItem item : items) {
            Product p = item.getProduct();
            if (p == null) {
                errors.add("购物车中存在无效商品");
                continue;
            }
            if (!"ACTIVE".equals(p.getStatus())) {
                errors.add("商品「" + p.getName() + "」已下架，请移除后再结算");
            }
            int available = stockService.getAvailableStock(
                    p.getStock() != null ? p.getStock() : 0, p.getId());
            if (available < item.getQuantity()) {
                errors.add("商品「" + p.getName() + "」可用库存不足，当前可用：" + available);
            }
        }
        return errors;
    }
}
