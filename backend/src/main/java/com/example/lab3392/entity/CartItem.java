package com.example.lab3392.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("cart_items")
public class CartItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long productId;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Product product;

    @TableField(exist = false)
    private BigDecimal subtotal;

    public BigDecimal getSubtotal() {
        if (product != null && product.getPrice() != null && quantity != null) {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
        return subtotal;
    }
}
