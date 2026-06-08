package com.example.lab3392.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.lab3392.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {}

