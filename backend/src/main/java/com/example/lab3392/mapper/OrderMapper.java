package com.example.lab3392.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lab3392.entity.Order;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.user_id = #{userId} " +
            "ORDER BY o.created_at DESC")
    List<Order> selectByUserIdWithUsername(@Param("userId") Long userId);

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.user_id = #{userId} AND o.status = #{status} " +
            "ORDER BY o.created_at DESC")
    List<Order> selectByUserIdAndStatus(@Param("userId") Long userId, @Param("status") String status);

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "ORDER BY o.created_at DESC")
    List<Order> selectAllWithUsername();

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.status = #{status} " +
            "ORDER BY o.created_at DESC")
    List<Order> selectByStatusWithUsername(@Param("status") String status);

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.order_no = #{orderNo} " +
            "ORDER BY o.created_at DESC")
    List<Order> searchByOrderNoWithUsername(@Param("orderNo") String orderNo);

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.status = #{status} AND o.order_no LIKE CONCAT('%',#{orderNo},'%') " +
            "ORDER BY o.created_at DESC")
    List<Order> searchByStatusAndOrderNo(@Param("status") String status, @Param("orderNo") String orderNo);

    @Update("UPDATE products SET stock = stock - #{quantity}, updated_at = NOW() " +
            "WHERE id = #{productId} AND stock >= #{quantity}")
    int deductStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Select("SELECT o.*, u.username FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.id = #{id}")
    Order selectByIdWithUsername(@Param("id") Long id);
}
