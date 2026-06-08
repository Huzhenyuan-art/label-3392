package com.example.lab3392.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.example.lab3392.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT p.*, c.name AS category_name " +
            "FROM products p " +
            "LEFT JOIN product_categories c ON p.category_id = c.id " +
            "${ew.customSqlSegment}")
    IPage<Product> selectPageWithCategory(IPage<Product> page, @Param(Constants.WRAPPER) Wrapper<Product> wrapper);

    @Select("SELECT p.*, c.name AS category_name " +
            "FROM products p " +
            "LEFT JOIN product_categories c ON p.category_id = c.id " +
            "WHERE p.id = #{id}")
    Product selectByIdWithCategory(Long id);
}

