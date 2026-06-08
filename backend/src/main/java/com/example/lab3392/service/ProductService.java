package com.example.lab3392.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.lab3392.dto.ProductForm;
import com.example.lab3392.dto.ProductQuery;
import com.example.lab3392.entity.Product;

public interface ProductService {
    IPage<Product> search(ProductQuery q, long page, long size);

    Product getByIdOrThrow(Long id);

    Product getByIdWithCategory(Long id);

    Product create(ProductForm form);

    void update(Long id, ProductForm form);

    void delete(Long id);
}

