package com.example.lab3392.service;

import com.example.lab3392.dto.CartAddForm;
import com.example.lab3392.entity.CartItem;
import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    void addToCart(CartAddForm form);

    List<CartItem> getCurrentUserCart();

    void updateQuantity(Long cartItemId, Integer quantity);

    void removeItem(Long cartItemId);

    BigDecimal calculateTotal(List<CartItem> items);

    List<String> validateCartForCheckout(List<CartItem> items);
}
