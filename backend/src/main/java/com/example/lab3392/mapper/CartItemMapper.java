package com.example.lab3392.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lab3392.entity.CartItem;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("SELECT ci.*, p.*, c.name AS category_name " +
            "FROM cart_items ci " +
            "JOIN products p ON ci.product_id = p.id " +
            "LEFT JOIN product_categories c ON p.category_id = c.id " +
            "WHERE ci.user_id = #{userId} " +
            "ORDER BY ci.updated_at DESC, ci.id DESC")
    List<CartItem> selectByUserIdWithProduct(@Param("userId") Long userId);
}
