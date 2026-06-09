package com.example.lab3392.controller;

import com.example.lab3392.dto.OrderQuery;
import com.example.lab3392.entity.Order;
import com.example.lab3392.entity.OrderItem;
import com.example.lab3392.entity.User;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class OrderController {

    private final OrderService orderService;
    private final UserMapper userMapper;

    public OrderController(OrderService orderService, UserMapper userMapper) {
        this.orderService = orderService;
        this.userMapper = userMapper;
    }

    private Long getCurrentUserId(Principal principal) {
        User u = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, principal.getName()));
        if (u == null) throw new IllegalArgumentException("用户不存在");
        return u.getId();
    }

    @PostMapping("/orders")
    public String createOrder(Principal principal, RedirectAttributes ra) {
        try {
            Long userId = getCurrentUserId(principal);
            Order order = orderService.createOrderFromCart(userId);
            ra.addFlashAttribute("flashOk", "订单已创建，请尽快支付");
            return "redirect:/orders/" + order.getId() + "/pay";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/cart/checkout";
        }
    }

    @GetMapping("/orders/{id}/pay")
    public String payPage(@PathVariable Long id, Principal principal, Model model) {
        try {
            Long userId = getCurrentUserId(principal);
            Order order = orderService.getOrderById(id, userId);
            model.addAttribute("order", order);
            model.addAttribute("username", principal.getName());
            model.addAttribute("totalQuantity", order.getItems() != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0);

            boolean isExpired = order.getExpireAt() != null &&
                    LocalDateTime.now().isAfter(order.getExpireAt());
            model.addAttribute("isExpired", isExpired);

            if (!isExpired && order.getExpireAt() != null) {
                long remainingSeconds = Duration.between(
                        LocalDateTime.now(), order.getExpireAt()).getSeconds();
                model.addAttribute("remainingSeconds", remainingSeconds);
            } else {
                model.addAttribute("remainingSeconds", 0);
            }

            return "orders/pay";
        } catch (SecurityException e) {
            return "redirect:/orders";
        } catch (IllegalArgumentException e) {
            return "redirect:/orders";
        }
    }

    @PostMapping("/orders/{id}/pay")
    public String confirmPay(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            Long userId = getCurrentUserId(principal);
            orderService.payOrder(id, userId);
            ra.addFlashAttribute("flashOk", "支付成功！");
            return "redirect:/orders/" + id + "?paid";
        } catch (IllegalArgumentException | SecurityException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/orders/" + id + "/pay";
        }
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            Long userId = getCurrentUserId(principal);
            orderService.cancelOrder(id, userId);
            ra.addFlashAttribute("flashOk", "订单已取消");
        } catch (IllegalArgumentException | SecurityException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
        }
        return "redirect:/orders";
    }

    @GetMapping("/orders")
    public String userOrders(@RequestParam(required = false) String status,
                             Principal principal, Model model) {
        Long userId = getCurrentUserId(principal);
        List<Order> orders = orderService.getUserOrders(userId, status);
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("username", principal.getName());
        return "orders/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, Principal principal, Model model) {
        try {
            Long userId = getCurrentUserId(principal);
            Order order = orderService.getOrderById(id, userId);
            model.addAttribute("order", order);
            model.addAttribute("username", principal.getName());
            model.addAttribute("totalQuantity", order.getItems() != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0);
            return "orders/detail";
        } catch (SecurityException e) {
            return "redirect:/orders";
        }
    }

    @GetMapping("/admin/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrders(@RequestParam(required = false) String status,
                              @RequestParam(required = false) String orderNo,
                              Principal principal, Model model) {
        OrderQuery query = new OrderQuery(status, orderNo);
        List<Order> orders = orderService.getAllOrders(query);
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);
        model.addAttribute("currentOrderNo", orderNo);
        model.addAttribute("username", principal.getName());
        return "orders/admin-list";
    }

    @GetMapping("/admin/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOrderDetail(@PathVariable Long id, Principal principal, Model model) {
        Order order = orderService.getOrderByIdForAdmin(id);
        model.addAttribute("order", order);
        model.addAttribute("username", principal.getName());
        model.addAttribute("isAdmin", true);
        model.addAttribute("totalQuantity", order.getItems() != null ? order.getItems().stream().mapToInt(OrderItem::getQuantity).sum() : 0);
        return "orders/detail";
    }
}
