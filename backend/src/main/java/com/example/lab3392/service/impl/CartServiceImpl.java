package com.example.lab3392.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.lab3392.dto.CartAddForm;
import com.example.lab3392.entity.CartItem;
import com.example.lab3392.entity.Product;
import com.example.lab3392.entity.User;
import com.example.lab3392.mapper.CartItemMapper;
import com.example.lab3392.mapper.ProductMapper;
import com.example.lab3392.mapper.UserMapper;
import com.example.lab3392.service.CartService;
import com.example.lab3392.service.ProductService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartServiceImpl implements CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final ProductService productService;
    private final UserMapper userMapper;

    public CartServiceImpl(CartItemMapper cartItemMapper, ProductMapper productMapper,
            ProductService productService, UserMapper userMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.productService = productService;
        this.userMapper = userMapper;
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User u = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (u == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return u.getId();
    }

    private void validateProductForCart(Product product, int requestedQuantity) {
        if (!"ACTIVE".equals(product.getStatus())) {
            throw new IllegalArgumentException("产品「" + product.getName() + "」已下架，无法加入购物车");
        }
        if (product.getStock() == null || product.getStock() < requestedQuantity) {
            throw new IllegalArgumentException("产品「" + product.getName() + "」库存不足，当前库存：" +
                    (product.getStock() == null ? 0 : product.getStock()));
        }
    }

    @Override
    @Transactional
    public void addToCart(CartAddForm form) {
        Long userId = getCurrentUserId();
        Product product = productService.getByIdOrThrow(form.productId());
        validateProductForCart(product, form.quantity());

        CartItem existing = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getProductId, form.productId())
        );

        if (existing != null) {
            int newQuantity = existing.getQuantity() + form.quantity();
            validateProductForCart(product, newQuantity);
            existing.setQuantity(newQuantity);
            cartItemMapper.updateById(existing);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(form.productId());
            item.setQuantity(form.quantity());
            cartItemMapper.insert(item);
        }
    }

    @Override
    public List<CartItem> getCurrentUserCart() {
        Long userId = getCurrentUserId();
        List<CartItem> items = cartItemMapper.selectByUserIdWithProduct(userId);
        for (CartItem item : items) {
            Product product = productService.getByIdWithCategory(item.getProductId());
            item.setProduct(product);
        }
        return items;
    }

    private CartItem getCartItemAndValidateOwnership(Long cartItemId) {
        Long userId = getCurrentUserId();
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null) {
            throw new IllegalArgumentException("购物车条目不存在");
        }
        if (!item.getUserId().equals(userId)) {
            throw new SecurityException("无权操作此购物车条目");
        }
        return item;
    }

    @Override
    @Transactional
    public void updateQuantity(Long cartItemId, Integer quantity) {
        CartItem item = getCartItemAndValidateOwnership(cartItemId);
        Product product = productService.getByIdOrThrow(item.getProductId());
        validateProductForCart(product, quantity);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void removeItem(Long cartItemId) {
        CartItem item = getCartItemAndValidateOwnership(cartItemId);
        cartItemMapper.deleteById(item.getId());
    }

    @Override
    public BigDecimal calculateTotal(List<CartItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items == null) return total;
        for (CartItem item : items) {
            if (item.getSubtotal() != null) {
                total = total.add(item.getSubtotal());
            }
        }
        return total;
    }

    @Override
    public List<String> validateCartForCheckout(List<CartItem> items) {
        List<String> errors = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            errors.add("购物车为空");
            return errors;
        }
        for (CartItem item : items) {
            Product p = item.getProduct();
            if (p == null) {
                errors.add("购物车中存在无效商品信息缺失的商品");
                continue;
            } else {
                if (!"ACTIVE".equals(p.getStatus())) {
                    errors.add("商品「" + p.getName() + "」已下架，请移除后再结算");
                }
                if (p.getStock() == null || p.getStock() < item.getQuantity()) {
                    errors.add("商品「" + p.getName() + "」库存不足，当前库存：" +
                            (p.getStock() == null ? 0 : p.getStock()));
                }
            }
        }
        return errors;
    }
}
